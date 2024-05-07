import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class HeadOfficeGUI extends JFrame {
    private DatabaseManager dbManager;
    private JTable table;

    public HeadOfficeGUI(String dbUrl) {
        dbManager = new DatabaseManager(dbUrl);
        setTitle("Head Office");
        setSize(800, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        table = new JTable(dbManager.getTableModel());
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        JButton addButton = new JButton("Add Entry");
        JButton removeButton = new JButton("Remove Entry");
        JButton refreshButton = new JButton("Refresh");
        JButton consumeBatchButton = new JButton("Sync");


        addButton.addActionListener(this::addEntry);
        removeButton.addActionListener(this::removeEntry);
        refreshButton.addActionListener(this::refreshTable);
        consumeBatchButton.addActionListener(e -> dbManager.setupConsumer());

        panel.add(addButton);
        panel.add(removeButton);
        panel.add(refreshButton);
        panel.add(consumeBatchButton);

        add(panel, BorderLayout.SOUTH);
    }
    private void addEntry(ActionEvent event) {
        JDialog dialog = new JDialog(this, "Add New Entry", true);
        dialog.setLayout(new GridLayout(0, 2));
        JTextField dateField = new JTextField();
        JTextField regionField = new JTextField();
        JTextField productField = new JTextField();
        JTextField qtyField = new JTextField();
        JTextField costField = new JTextField();
        JTextField amtField = new JTextField();
        JTextField taxField = new JTextField();
        JTextField totalField = new JTextField();

        dialog.add(new JLabel("Date:"));
        dialog.add(dateField);
        dialog.add(new JLabel("Region:"));
        dialog.add(regionField);
        dialog.add(new JLabel("Product:"));
        dialog.add(productField);
        dialog.add(new JLabel("Quantity:"));
        dialog.add(qtyField);
        dialog.add(new JLabel("Cost:"));
        dialog.add(costField);
        dialog.add(new JLabel("Amount:"));
        dialog.add(amtField);
        dialog.add(new JLabel("Tax:"));
        dialog.add(taxField);
        dialog.add(new JLabel("Total:"));
        dialog.add(totalField);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            dbManager.addEntry(dateField.getText(), regionField.getText(), productField.getText(),
                    Integer.parseInt(qtyField.getText()), Double.parseDouble(costField.getText()),
                    Double.parseDouble(amtField.getText()), Double.parseDouble(taxField.getText()),
                    Double.parseDouble(totalField.getText()));
            refreshTable(null);
            dialog.dispose();
        });
        dialog.add(submitButton);
        dialog.pack();
        dialog.setVisible(true);
    }


    private void removeEntry(ActionEvent event) {
        // Remove selected row from database
        int selectedRow = table.getSelectedRow();

        if (selectedRow >= 0) {
            String region = table.getValueAt(selectedRow, 1).toString();
            String product = table.getValueAt(selectedRow, 2).toString();
            String qty = table.getValueAt(selectedRow, 3).toString();
            dbManager.removeEntry(region,product,qty);
            refreshTable(null);
        }
    }

    private void refreshTable(ActionEvent event) {
        table.setModel(dbManager.getTableModel());
    }

    public static void main(String[] args) {
        final String headOfficeDbUrl = "head_office";
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        EventQueue.invokeLater(() -> {
            new HeadOfficeGUI(headOfficeDbUrl).setVisible(true);
        });
    }
}