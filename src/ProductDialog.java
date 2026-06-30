import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class ProductDialog extends JDialog {
    private JTextField txtSku;
    private JTextField txtName;
    private JTextField txtCategory;
    private JTextField txtPrice;
    private JTextField txtQuantity;
    private JTextField txtThreshold;
    private JButton btnSave;
    private JButton btnCancel;
    
    private boolean saved = false;
    private Product product;
    private boolean isEditMode;

    public ProductDialog(Frame parent, String title) {
        super(parent, title, true);
        this.isEditMode = false;
        this.product = new Product();
        initComponents();
    }

    public ProductDialog(Frame parent, String title, Product product) {
        super(parent, title, true);
        this.isEditMode = true;
        this.product = product;
        initComponents();
        populateFields();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setResizable(false);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIManager.getColor("Table.selectionBackground"));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel lblHeader = new JLabel(isEditMode ? "Edit Product Details" : "Add New Product");
        lblHeader.setFont(lblHeader.getFont().deriveFont(Font.BOLD, 16f));
        lblHeader.setForeground(UIManager.getColor("Table.selectionForeground"));
        headerPanel.add(lblHeader, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Labels and Textfields
        addFormField(formPanel, gbc, 0, "SKU *:", txtSku = new JTextField(15));
        addFormField(formPanel, gbc, 1, "Name *:", txtName = new JTextField(15));
        addFormField(formPanel, gbc, 2, "Category:", txtCategory = new JTextField(15));
        addFormField(formPanel, gbc, 3, "Price ($) *:", txtPrice = new JTextField(15));
        addFormField(formPanel, gbc, 4, "Quantity *:", txtQuantity = new JTextField(15));
        addFormField(formPanel, gbc, 5, "Min Threshold *:", txtThreshold = new JTextField(15));

        // Add placeholders
        txtSku.putClientProperty("JTextField.placeholderText", "e.g. PROD-1001");
        txtName.putClientProperty("JTextField.placeholderText", "e.g. Wireless Mouse");
        txtCategory.putClientProperty("JTextField.placeholderText", "e.g. Electronics");
        txtPrice.putClientProperty("JTextField.placeholderText", "e.g. 29.99");
        txtQuantity.putClientProperty("JTextField.placeholderText", "e.g. 50");
        txtThreshold.putClientProperty("JTextField.placeholderText", "Alert threshold (default: 5)");

        // Set default threshold in add mode
        if (!isEditMode) {
            txtThreshold.setText("5");
        }

        add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBorder(new EmptyBorder(0, 10, 10, 20));

        btnSave = new JButton(isEditMode ? "Update" : "Save");
        btnSave.putClientProperty("JButton.buttonType", "accent"); // Highlight color for FlatLaf
        btnSave.addActionListener(e -> onSave());

        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        add(buttonPanel, BorderLayout.SOUTH);

        // Enter key saves
        getRootPane().setDefaultButton(btnSave);

        pack();
        setLocationRelativeTo(getParent());
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(component, gbc);
    }

    private void populateFields() {
        txtSku.setText(product.getSku());
        // In edit mode, we make SKU read-only to maintain database consistency, 
        // or we can allow editing if we validate uniqueness (excluding current ID).
        // Let's keep it editable, but validate uniqueness.
        txtName.setText(product.getName());
        txtCategory.setText(product.getCategory());
        txtPrice.setText(String.valueOf(product.getPrice()));
        txtQuantity.setText(String.valueOf(product.getQuantity()));
        txtThreshold.setText(String.valueOf(product.getMinThreshold()));
    }

    private void onSave() {
        String sku = txtSku.getText().trim();
        String name = txtName.getText().trim();
        String category = txtCategory.getText().trim();
        String priceText = txtPrice.getText().trim();
        String quantityText = txtQuantity.getText().trim();
        String thresholdText = txtThreshold.getText().trim();

        // Validations
        if (sku.isEmpty() || name.isEmpty() || priceText.isEmpty() || quantityText.isEmpty() || thresholdText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields (*)!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0) {
                JOptionPane.showMessageDialog(this, "Price cannot be negative!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Price!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity < 0) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be negative!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid integer for Quantity!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int threshold;
        try {
            threshold = Integer.parseInt(thresholdText);
            if (threshold < 0) {
                JOptionPane.showMessageDialog(this, "Min Threshold cannot be negative!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid integer for Min Threshold!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check SKU uniqueness
        try {
            int excludeId = isEditMode ? product.getId() : -1;
            if (DatabaseHelper.isSkuExists(sku, excludeId)) {
                JOptionPane.showMessageDialog(this, "SKU '" + sku + "' already exists! Please use a unique SKU.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error checking SKU: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Populate object
        product.setSku(sku);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setMinThreshold(threshold);

        try {
            boolean success;
            if (isEditMode) {
                success = DatabaseHelper.updateProduct(product);
            } else {
                success = DatabaseHelper.addProduct(product);
            }

            if (success) {
                saved = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save product information.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public Product getProduct() {
        return product;
    }
}
