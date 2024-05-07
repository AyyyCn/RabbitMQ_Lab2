import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class JdbcRetrieve {

    private Connection connect() {
        // Replace these with your actual details
        String url = "jdbc:mysql://localhost:3306/head_office?useSSL=false";
        String user = "root";
        String password = "root";
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public DefaultTableModel getData() {
        String query = "SELECT * FROM Product_Sales";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Date", "Region", "Product", "Qty", "Cost", "Amt", "Tax", "Total"}, 0);
            while (rs.next()) {
                Object[] row = new Object[]{
                        rs.getDate("Date"),
                        rs.getString("Region"),
                        rs.getString("Product"),
                        rs.getInt("Qty"),
                        rs.getDouble("Cost"),
                        rs.getDouble("Amt"),
                        rs.getDouble("Tax"),
                        rs.getDouble("Total")
                };
                tableModel.addRow(row);
            }
            return tableModel;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
