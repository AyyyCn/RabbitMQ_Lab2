import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

import com.rabbitmq.client.*;


public class DatabaseManager {
    private String databaseUrl;
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "root";

    private List<String> changeLog = new ArrayList<>();
    public DatabaseManager(String databaseUrl) {
        this.databaseUrl =  "jdbc:mysql://localhost:3306/"+databaseUrl+"?useSSL=false";

    }
    private void logChange(String sql) {
        changeLog.add(sql);
    }
    public DefaultTableModel getTableModel() {
        try (java.sql.Connection connection = DriverManager.getConnection(databaseUrl, DATABASE_USER, DATABASE_PASSWORD)) {
            String query = "SELECT * FROM Product_Sales";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            return buildTableModel(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            return new DefaultTableModel();
        }
    }

    private static DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
            }
            data.add(vector);
        }

        return new DefaultTableModel(data, columnNames);
    }
    public void addEntry(String date, String region, String product, int qty, double cost, double amt, double tax, double total) {
        String sql = "INSERT INTO Product_Sales (Date, Region, Product, Qty, Cost, Amt, Tax, Total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection connection = DriverManager.getConnection(databaseUrl, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, date);
            pst.setString(2, region);
            pst.setString(3, product);
            pst.setInt(4, qty);
            pst.setDouble(5, cost);
            pst.setDouble(6, amt);
            pst.setDouble(7, tax);
            pst.setDouble(8, total);
            int affectedRows = pst.executeUpdate();
            if (affectedRows > 0) {
                logChange(String.format("INSERT INTO Product_Sales (Date, Region, Product, Qty, Cost, Amt, Tax, Total) VALUES ('%s', '%s', '%s', %d, %f, %f, %f, %f);", date, region, product, qty, cost, amt, tax, total));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void removeEntry(String region, String product,String Qty) {
        String sql = "DELETE FROM Product_Sales WHERE Region = ? AND Product = ? AND Qty =  ?";
        try (java.sql.Connection connection = DriverManager.getConnection(databaseUrl, DATABASE_USER, DATABASE_PASSWORD);
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, region);
            pst.setString(2, product);
            pst.setString(3, Qty);
            int affectedRows = pst.executeUpdate();
            if (affectedRows > 0) {
                logChange(String.format("DELETE FROM Product_Sales WHERE Region = '%s' AND Product = '%s' AND Qty= '%s';", region, product,Qty));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendBatch() {
        if (changeLog.isEmpty()) {
            System.out.println("No updates!");
            return;  // Nothing to send
        }
        String message = String.join(";", changeLog);  // Simple joining of commands; adjust as needed

        try {
            com.rabbitmq.client.ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");  // Adjust as needed
            try (com.rabbitmq.client.Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                channel.queueDeclare("branch_updates", true, false, false, null);
                channel.basicPublish("", "branch_updates", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
                System.out.println("Batch sent: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        changeLog.clear();  // Clear the log after sending
    }
    public void setupConsumer() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");  // Adjust as needed

        try {
            com.rabbitmq.client.Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare("branch_updates", true, false, false, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println("Received batch: " + message);
                applyChanges(message);  // Implement this method to apply changes to the database
            };
            channel.basicConsume("branch_updates", true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void applyChanges(String batch) {
        String[] commands = batch.split(";");  // Assuming each command ends with a semicolon
        try (java.sql.Connection connection = DriverManager.getConnection(databaseUrl, DATABASE_USER, DATABASE_PASSWORD)) {
            connection.setAutoCommit(false); // Begin transaction
            try (Statement statement = connection.createStatement()) {
                for (String command : commands) {
                    if (!command.trim().isEmpty()) {
                        statement.executeUpdate(command.trim());  // Execute each SQL command
                    }
                }
                connection.commit();  // Commit all changes
                System.out.println("All changes applied and committed successfully.");
            } catch (SQLException e) {
                connection.rollback();  // Rollback on error
                e.printStackTrace();
                System.out.println("Error applying changes, transaction rolled back: " + e.getMessage());
            } finally {
                connection.setAutoCommit(true); // Restore auto-commit
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error setting up database connection: " + e.getMessage());
        }
    }

}
