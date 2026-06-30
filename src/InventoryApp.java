import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

public class InventoryApp extends JFrame {
    private JTable productTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    
    private JTextField txtSearch;
    private CardPanel cardTotalProducts;
    private CardPanel cardTotalValue;
    private CardPanel cardLowStockAlerts;
    
    private JLabel lblStatus;
    private JButton btnShowAll;
    private boolean showingLowStockOnly = false;
    
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$#,##0.00");

    public InventoryApp() {
        super("Inventory Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        
        initComponents();
        refreshData();
    }

    private void initComponents() {
        // Main Container
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);

        // --- TOP PANEL: Dashboard Stats & Header ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 15));
        
        // Header Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel lblAppTitle = new JLabel("INVENTORY HQ");
        lblAppTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblAppTitle.setForeground(UIManager.getColor("textText"));
        
        JLabel lblAppSubtitle = new JLabel("Real-time local stock tracking & management");
        lblAppSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAppSubtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        
        titlePanel.add(lblAppTitle, BorderLayout.NORTH);
        titlePanel.add(lblAppSubtitle, BorderLayout.SOUTH);
        topPanel.add(titlePanel, BorderLayout.NORTH);

        // Stats Cards Panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        cardTotalProducts = new CardPanel("TOTAL PRODUCTS", "0", UIManager.getColor("Label.foreground"));
        cardTotalValue = new CardPanel("TOTAL INVENTORY VALUE", "$0.00", UIManager.getColor("Component.accentColor"));
        
        // Low Stock Card gets a distinct warm/red color when count > 0
        cardLowStockAlerts = new CardPanel("LOW STOCK ALERTS", "0", Color.decode("#E06C75"));
        cardLowStockAlerts.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cardLowStockAlerts.setToolTipText("Click to view low stock items");
        cardLowStockAlerts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleLowStockFilter();
            }
        });

        statsPanel.add(cardTotalProducts);
        statsPanel.add(cardTotalValue);
        statsPanel.add(cardLowStockAlerts);
        topPanel.add(statsPanel, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --- CENTER PANEL: Search & Table ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // Toolbar Panel (Search and Action controls)
        JPanel toolbarPanel = new JPanel(new BorderLayout(10, 10));
        
        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        txtSearch = new JTextField(25);
        txtSearch.putClientProperty("JTextField.placeholderText", "Search by name, SKU, or category...");
        txtSearch.putClientProperty("JTextField.showClearButton", true);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { performSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { performSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { performSearch(); }
        });
        searchPanel.add(txtSearch);
        
        btnShowAll = new JButton("Reset Filter");
        btnShowAll.setVisible(false);
        btnShowAll.addActionListener(e -> {
            showingLowStockOnly = false;
            btnShowAll.setVisible(false);
            txtSearch.setText("");
            lblStatus.setText("Showing all products.");
            refreshData();
        });
        
        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchWrapper.add(new JLabel("🔍"));
        searchWrapper.add(txtSearch);
        searchWrapper.add(btnShowAll);
        
        toolbarPanel.add(searchWrapper, BorderLayout.WEST);

        // Right side of toolbar (Quick Refresh)
        JButton btnRefresh = new JButton("🔄 Refresh");
        btnRefresh.addActionListener(e -> refreshData());
        toolbarPanel.add(btnRefresh, BorderLayout.EAST);
        
        centerPanel.add(toolbarPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "SKU", "Product Name", "Category", "Price", "Qty in Stock", "Threshold"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };
        
        productTable = new JTable(tableModel);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productTable.setRowHeight(28);
        productTable.setShowGrid(true);
        productTable.setGridColor(UIManager.getColor("Table.gridColor"));
        
        // Hide ID column from view but keep in model
        productTable.getColumnModel().getColumn(0).setMinWidth(0);
        productTable.getColumnModel().getColumn(0).setMaxWidth(0);
        productTable.getColumnModel().getColumn(0).setWidth(0);
        
        // Setup row sorting
        rowSorter = new TableRowSorter<>(tableModel);
        productTable.setRowSorter(rowSorter);

        // Custom Cell Renderer to Highlight Low Stock
        DefaultTableCellRenderer lowStockRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Get original row index in the model
                int modelRow = table.convertRowIndexToModel(row);
                int qty = (int) tableModel.getValueAt(modelRow, 5);
                int threshold = (int) tableModel.getValueAt(modelRow, 6);
                
                if (!isSelected) {
                    if (qty <= threshold) {
                        c.setForeground(Color.decode("#E06C75")); // Alert soft red
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                        // Soft warning background
                        c.setBackground(Color.decode("#3E2D32"));
                    } else {
                        c.setForeground(table.getForeground());
                        c.setBackground(table.getBackground());
                    }
                } else {
                    c.setForeground(table.getSelectionForeground());
                    c.setBackground(table.getSelectionBackground());
                }
                
                // Format Price column to currency
                if (column == 4 && value instanceof Double) {
                    setText(CURRENCY_FORMAT.format(value));
                }
                
                return c;
            }
        };
        
        for (int i = 0; i < productTable.getColumnCount(); i++) {
            productTable.getColumnModel().getColumn(i).setCellRenderer(lowStockRenderer);
        }

        // Table mouse listener for double-click edit
        productTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && productTable.getSelectedRow() != -1) {
                    editSelectedProduct();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // --- EAST PANEL: Action Buttons ---
        JPanel eastPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        eastPanel.setBorder(new EmptyBorder(40, 5, 0, 5));

        JButton btnAdd = new JButton("✚ Add Product");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdd.putClientProperty("JButton.buttonType", "accent");
        btnAdd.addActionListener(e -> addProduct());

        JButton btnEdit = new JButton("✎ Edit Product");
        btnEdit.addActionListener(e -> editSelectedProduct());

        JButton btnStock = new JButton("⇅ Adjust Stock");
        btnStock.addActionListener(e -> adjustStock());

        JButton btnDelete = new JButton("🗑 Delete Product");
        btnDelete.addActionListener(e -> deleteSelectedProduct());

        eastPanel.add(btnAdd);
        eastPanel.add(btnEdit);
        eastPanel.add(btnStock);
        eastPanel.add(btnDelete);
        
        mainPanel.add(eastPanel, BorderLayout.EAST);

        // --- SOUTH PANEL: Status Bar ---
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        lblStatus = new JLabel("System Ready.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(UIManager.getColor("Label.disabledForeground"));
        
        southPanel.add(lblStatus, BorderLayout.WEST);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
    }

    private void refreshData() {
        try {
            // Load statistics
            int totalProducts = DatabaseHelper.getTotalProductsCount();
            double totalVal = DatabaseHelper.getTotalInventoryValue();
            int lowStockCount = DatabaseHelper.getLowStockCount();

            cardTotalProducts.setValue(String.valueOf(totalProducts));
            cardTotalValue.setValue(CURRENCY_FORMAT.format(totalVal));
            cardLowStockAlerts.setValue(String.valueOf(lowStockCount));

            // Load table items
            List<Product> products;
            if (showingLowStockOnly) {
                products = DatabaseHelper.getLowStockProducts();
                lblStatus.setText("Viewing low stock items only.");
                btnShowAll.setVisible(true);
            } else {
                products = DatabaseHelper.getAllProducts();
            }

            tableModel.setRowCount(0);
            for (Product p : products) {
                tableModel.addRow(new Object[]{
                        p.getId(),
                        p.getSku(),
                        p.getName(),
                        p.getCategory(),
                        p.getPrice(),
                        p.getQuantity(),
                        p.getMinThreshold()
                });
            }
            
            // Check if any low stock exists and show message in status bar
            if (lowStockCount > 0) {
                lblStatus.setText("⚠️ Warning: " + lowStockCount + " item(s) are below the low stock threshold!");
            } else if (!showingLowStockOnly) {
                lblStatus.setText("Database loaded successfully. Total products: " + totalProducts);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load inventory data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = txtSearch.getText().trim();
        if (showingLowStockOnly) {
            // Filter existing table model rows
            if (query.isEmpty()) {
                rowSorter.setRowFilter(null);
            } else {
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + query, 1, 2, 3)); // Search SKU, Name, Category
            }
        } else {
            // Retrieve from DB directly for high efficiency search
            try {
                List<Product> products = DatabaseHelper.searchProducts(query);
                tableModel.setRowCount(0);
                for (Product p : products) {
                    tableModel.addRow(new Object[]{
                            p.getId(),
                            p.getSku(),
                            p.getName(),
                            p.getCategory(),
                            p.getPrice(),
                            p.getQuantity(),
                            p.getMinThreshold()
                    });
                }
                lblStatus.setText("Search results for '" + query + "': " + products.size() + " items found.");
            } catch (SQLException e) {
                lblStatus.setText("Search failed: " + e.getMessage());
            }
        }
    }

    private void toggleLowStockFilter() {
        showingLowStockOnly = !showingLowStockOnly;
        if (showingLowStockOnly) {
            btnShowAll.setVisible(true);
        } else {
            btnShowAll.setVisible(false);
            txtSearch.setText("");
        }
        refreshData();
    }

    private void addProduct() {
        ProductDialog dialog = new ProductDialog(this, "Add New Product");
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
            lblStatus.setText("Product '" + dialog.getProduct().getName() + "' added successfully.");
        }
    }

    private void editSelectedProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit!", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productTable.convertRowIndexToModel(selectedRow);
        Product p = getProductFromModelRow(modelRow);

        ProductDialog dialog = new ProductDialog(this, "Edit Product: " + p.getName(), p);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
            lblStatus.setText("Product '" + dialog.getProduct().getName() + "' updated successfully.");
        }
    }

    private void adjustStock() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to adjust stock!", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 2);
        int currentQty = (int) tableModel.getValueAt(modelRow, 5);

        String response = JOptionPane.showInputDialog(this, 
                "Adjust stock for '" + productName + "'\nCurrent Quantity: " + currentQty + "\n\nEnter new stock quantity:", 
                "Adjust Stock", 
                JOptionPane.QUESTION_MESSAGE);

        if (response == null) return; // User cancelled

        try {
            int newQty = Integer.parseInt(response.trim());
            if (newQty < 0) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be negative!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = DatabaseHelper.updateStock(productId, newQty);
            if (success) {
                refreshData();
                lblStatus.setText("Stock adjusted for '" + productName + "' to " + newQty + ".");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update stock in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid integer quantity!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete!", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete product '" + productName + "'?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = DatabaseHelper.deleteProduct(productId);
                if (success) {
                    refreshData();
                    lblStatus.setText("Product '" + productName + "' deleted successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete product from database.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Product getProductFromModelRow(int modelRow) {
        Product p = new Product();
        p.setId((int) tableModel.getValueAt(modelRow, 0));
        p.setSku((String) tableModel.getValueAt(modelRow, 1));
        p.setName((String) tableModel.getValueAt(modelRow, 2));
        p.setCategory((String) tableModel.getValueAt(modelRow, 3));
        p.setPrice((double) tableModel.getValueAt(modelRow, 4));
        p.setQuantity((int) tableModel.getValueAt(modelRow, 5));
        p.setMinThreshold((int) tableModel.getValueAt(modelRow, 6));
        return p;
    }

    public static void main(String[] args) {
        // Run application on EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize FlatLaf Dark Theme for a premium feel
                FlatDarkLaf.setup();
            } catch (Exception e) {
                System.err.println("Failed to initialize FlatLaf theme.");
            }
            
            InventoryApp app = new InventoryApp();
            app.setVisible(true);
        });
    }

    // Inner Class for custom styled KPI Card
    private static class CardPanel extends JPanel {
        private final JLabel lblTitle;
        private final JLabel lblValue;
        private final Color valColor;

        public CardPanel(String title, String value, Color valColor) {
            this.valColor = valColor;
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(12, 16, 12, 16));
            
            lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblTitle.setForeground(UIManager.getColor("Label.disabledForeground"));
            
            lblValue = new JLabel(value);
            lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblValue.setForeground(valColor);
            
            add(lblTitle, BorderLayout.NORTH);
            add(lblValue, BorderLayout.CENTER);
        }

        public void setValue(String value) {
            lblValue.setText(value);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw a rounded card background matching FlatLaf theme components
            g2.setColor(UIManager.getColor("Table.background"));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            
            // Draw thin border
            g2.setColor(UIManager.getColor("Component.borderColor"));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
