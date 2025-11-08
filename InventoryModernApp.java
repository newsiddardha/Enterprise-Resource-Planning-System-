/* InventoryModernApp.java
   Modern Swing Inventory app with Login + Logout + SQLite + Role-based Access + FlatLaf
   Roles: Admin (all), Manager (no delete), Staff (sales only)
   Default theme: FlatDarkLaf (toggle to Light via sidebar button)
*/
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class InventoryModernApp {

    private static final String[] COLUMN_NAMES = {
            "SKU", "Item Name", "Quantity", "Cost Price", "Selling Price",
            "Category", "Location", "Min Stock Threshold"
    };

    private JFrame frame, loginFrame;
    private DefaultTableModel model;
    private JTable inventoryTable;
    private TableRowSorter<DefaultTableModel> sorter;
    private int skuCounter = 1;

    private final DBHelper db = new DBHelper();

    private String currentUser, currentRole;

    private final CardLayout mainCards = new CardLayout();
    private final JPanel mainPanel = new JPanel(mainCards);

    private boolean darkTheme = true;

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(new FlatDarkLaf()); } catch (Exception ignore) {}
        SwingUtilities.invokeLater(() -> new InventoryModernApp().showLoginScreen());
    }

    // -------------------- Login Screen --------------------
    private void showLoginScreen() {
        applyLookAndFeel(); // ensure LAF matches current flag

        loginFrame = new JFrame("Login - Inventory System");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(420, 260);
        loginFrame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("🔐 Login");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField();
        userField.putClientProperty("JTextField.placeholderText", "admin");

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.gridx = 0;
        panel.add(userLabel, gbc);
        gbc.gridx = 1;
        panel.add(userField, gbc);

        JLabel passLabel = new JLabel("Password:");
        JPasswordField passField = new JPasswordField();
        passField.putClientProperty("JTextField.placeholderText", "admin123");

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(passLabel, gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        JButton loginBtn = new JButton("Login");
        loginBtn.setFocusPainted(false);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            String role = db.authenticateRole(u, p);
            if (role != null) {
                currentUser = u;
                currentRole = role;
                loginFrame.dispose();
                createAndShowGUI();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        loginFrame.setContentPane(panel);
        loginFrame.setVisible(true);
    }

    // -------------------- Main App --------------------
    private void createAndShowGUI() {
        frame = new JFrame("Inventory — Modern UI  (" + currentUser + " : " + currentRole + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 720);
        frame.setMinimumSize(new Dimension(900, 600));

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel sidebar = buildSidebar();
        root.add(sidebar, BorderLayout.WEST);

        model = new DefaultTableModel(COLUMN_NAMES, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        inventoryTable = new JTable(model);
        inventoryTable.setRowHeight(28);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setShowGrid(false);
        sorter = new TableRowSorter<>(model);
        inventoryTable.setRowSorter(sorter);
        inventoryTable.setDefaultRenderer(Object.class, new ModernTableCellRenderer());

        mainPanel.add(buildEntryPanel(), "ENTRY");
        mainPanel.add(buildInventoryPanel(), "INVENTORY");
        mainPanel.add(buildExitPanel(), "EXIT");
        mainPanel.add(buildReportsPanel(), "REPORTS");
        mainPanel.add(buildDashboardPanel(), "DASHBOARD");

        mainCards.show(mainPanel, "DASHBOARD");
        root.add(mainPanel, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        loadInventoryFromDB();
        skuCounter = computeNextSku();
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(darkTheme ? new FlatDarkLaf() : new FlatLightLaf());
        } catch (Exception ignore) {}
        if (frame != null) SwingUtilities.updateComponentTreeUI(frame);
        if (loginFrame != null) SwingUtilities.updateComponentTreeUI(loginFrame);
    }

    private int computeNextSku() {
        int max = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            String sku = model.getValueAt(i, 0).toString();
            try {
                int n = Integer.parseInt(sku.replaceAll("\\D+", ""));
                if (n > max) max = n;
            } catch (Exception ignored) {}
        }
        return max + 1;
    }

    private void loadInventoryFromDB() {
        model.setRowCount(0);
        List<Object[]> rows = db.fetchInventory();
        for (Object[] r : rows) model.addRow(r);
    }

    // ------------------------- Sidebar -------------------------
    private JPanel buildSidebar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setPreferredSize(new Dimension(230, 0));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("\uD83D\uDCE6  Inventory");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(new EmptyBorder(6, 6, 12, 6));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 8, 8));

        JButton dashboardBtn = createSidebarButton("\uD83D\uDCC8  Dashboard", e -> switchTo("DASHBOARD"));
        JButton entryBtn     = createSidebarButton("➕  Entry", e -> switchTo("ENTRY"));
        JButton inventoryBtn = createSidebarButton("📦  Inventory", e -> switchTo("INVENTORY"));
        JButton salesBtn     = createSidebarButton("💳  Exit (Sales)", e -> switchTo("EXIT"));
        JButton reportsBtn   = createSidebarButton("📑  Reports", e -> switchTo("REPORTS"));

        buttons.add(dashboardBtn);
        if (!"Staff".equals(currentRole)) buttons.add(entryBtn);
        buttons.add(inventoryBtn);
        buttons.add(salesBtn);
        if (!"Staff".equals(currentRole)) buttons.add(reportsBtn);

        JPanel footer = new JPanel(new GridLayout(2, 1, 6, 6));
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));

        JButton themeToggle = new JButton(darkTheme ? "☀  Light Theme" : "🌙  Dark Theme");
        themeToggle.addActionListener(e -> {
            darkTheme = !darkTheme;
            applyLookAndFeel();
            themeToggle.setText(darkTheme ? "☀  Light Theme" : "🌙  Dark Theme");
            JOptionPane.showMessageDialog(frame, (darkTheme ? "Dark" : "Light") + " mode enabled");
        });

        JButton logoutBtn = new JButton("🔓  Logout");
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            showLoginScreen();
        });

        footer.add(themeToggle);
        footer.add(logoutBtn);

        p.add(title, BorderLayout.NORTH);
        p.add(buttons, BorderLayout.CENTER);
        p.add(footer, BorderLayout.SOUTH);
        return p;
    }

    private JButton createSidebarButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        b.setFocusPainted(false);
        b.addActionListener(al);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void switchTo(String card) {
        mainCards.show(mainPanel, card);
    }

    // ------------------------- Panels -------------------------
    private JPanel cardContainer(String title) {
        JPanel container = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
        header.add(t, BorderLayout.WEST);
        container.add(header, BorderLayout.NORTH);
        return container;
    }

    private JPanel horizontalSplit(JComponent left, JComponent right) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.65;
        p.add(left, gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        p.add(right, gbc);
        return p;
    }

    private JPanel buildEntryPanel() {
        JPanel container = cardContainer("Add / Restock Items");
        JPanel inner = new JPanel(new GridBagLayout());

        if ("Staff".equals(currentRole)) {
            inner.add(new JLabel("Access denied: Staff cannot add/restock items."));
            container.add(inner, BorderLayout.CENTER);
            return container;
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        JTextField itemNameField = new JTextField();
        itemNameField.putClientProperty("JTextField.placeholderText", "Item name");
        JTextField quantityField = new JTextField();
        quantityField.putClientProperty("JTextField.placeholderText", "Quantity");
        JTextField costField = new JTextField();
        costField.putClientProperty("JTextField.placeholderText", "Cost price");
        JTextField sellField = new JTextField();
        sellField.putClientProperty("JTextField.placeholderText", "Selling price");
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{"Electronics", "Clothing", "Food", "Other"});
        JComboBox<String> locationBox = new JComboBox<>(new String[]{"Warehouse A", "Warehouse B", "Shelf 1", "Shelf 2"});
        JTextField minStockField = new JTextField();
        minStockField.putClientProperty("JTextField.placeholderText", "Min stock threshold");
        JButton addButton = new JButton("➕ Add Item");

        gbc.gridx = 0; gbc.gridy = row; inner.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1; inner.add(itemNameField, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; inner.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; inner.add(quantityField, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; inner.add(new JLabel("Cost:"), gbc);
        gbc.gridx = 1; inner.add(costField, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; inner.add(new JLabel("Selling Price:"), gbc);
        gbc.gridx = 1; inner.add(sellField, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; inner.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; inner.add(categoryBox, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; inner.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1; inner.add(locationBox, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; inner.add(new JLabel("Min Stock:"), gbc);
        gbc.gridx = 1; inner.add(minStockField, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; inner.add(addButton, gbc);

        // Restock area
        JPanel restock = new JPanel(new GridBagLayout());
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(6, 6, 6, 6);
        r.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> skuBox = new JComboBox<>();
        updateSKUComboFromModel(skuBox);
        modelAddTableListenerToUpdateSKU(skuBox);

        JTextField restockQty = new JTextField();
        restockQty.putClientProperty("JTextField.placeholderText", "Qty to add");
        JButton restockBtn = new JButton("📥 Restock");

        r.gridx = 0; r.gridy = 0; restock.add(new JLabel("SKU:"), r);
        r.gridx = 1; restock.add(skuBox, r);
        r.gridx = 0; r.gridy = 1; restock.add(new JLabel("Qty:"), r);
        r.gridx = 1; restock.add(restockQty, r);
        r.gridx = 0; r.gridy = 2; r.gridwidth = 2; restock.add(restockBtn, r);

        addButton.addActionListener(e -> {
            String name = itemNameField.getText().trim();
            String q = quantityField.getText().trim();
            String c = costField.getText().trim();
            String s = sellField.getText().trim();
            String min = minStockField.getText().trim();
            if (name.isEmpty() || q.isEmpty() || c.isEmpty() || s.isEmpty() || min.isEmpty()) {
                showInfo("Validation", "Please fill all fields");
                return;
            }
            try {
                int qty = Integer.parseInt(q);
                int mn = Integer.parseInt(min);
                double cost = Double.parseDouble(c);
                double sell = Double.parseDouble(s);
                if (qty < 0 || mn < 0 || cost < 0 || sell < 0) throw new Exception();
                String sku = String.format("UQ%03d", skuCounter++);
                db.insertItem(sku, name, qty, cost, sell,
                        Objects.toString(categoryBox.getSelectedItem(), "Other"),
                        Objects.toString(locationBox.getSelectedItem(), "Warehouse A"), mn);
                model.addRow(new Object[]{sku, name, qty, cost, sell, categoryBox.getSelectedItem(), locationBox.getSelectedItem(), mn});
                updateSKUComboFromModel(skuBox);
                showInfo("Success", "Item added: " + name);
                itemNameField.setText(""); quantityField.setText("");
                costField.setText(""); sellField.setText(""); minStockField.setText("");
            } catch (Exception ex) {
                showInfo("Error", "Invalid numeric values");
            }
        });

        restockBtn.addActionListener(e -> {
            String sku = (String) skuBox.getSelectedItem();
            String qtys = restockQty.getText().trim();
            if (sku == null || qtys.isEmpty()) {
                showInfo("Error", "Select SKU and enter qty");
                return;
            }
            try {
                int qv = Integer.parseInt(qtys);
                if (qv <= 0) throw new Exception();
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (sku.equals(model.getValueAt(i, 0))) {
                        int cur = Integer.parseInt(model.getValueAt(i, 2).toString());
                        int newQty = cur + qv;
                        db.updateQuantity(sku, newQty);
                        model.setValueAt(newQty, i, 2);
                        showInfo("Restocked", "New qty: " + newQty);
                        restockQty.setText("");
                        break;
                    }
                }
            } catch (Exception ex) {
                showInfo("Error", "Invalid qty");
            }
        });

        container.add(horizontalSplit(inner, restock), BorderLayout.CENTER);
        return container;
    }

    private void modelAddTableListenerToUpdateSKU(JComboBox<String> skuBox) {
        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                updateSKUComboFromModel(skuBox);
            }
        });
    }

    private JPanel buildInventoryPanel() {
        JPanel container = cardContainer("Inventory");
        JPanel top = new JPanel(new BorderLayout(8, 8));
        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", "Search by SKU/Name/Category");
        search.setPreferredSize(new Dimension(240, 30));
        JButton low = new JButton("⚠ Low Stock");
        JButton del = new JButton("🗑 Delete");
        del.setEnabled("Admin".equals(currentRole)); // only Admin can delete

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(low); btns.add(del);
        top.add(search, BorderLayout.CENTER);
        top.add(btns, BorderLayout.EAST);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String t = search.getText().trim();
                if (t.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + t, 0, 1, 5));
            }
        });

        del.addActionListener(e -> {
            int sel = inventoryTable.getSelectedRow();
            if (sel == -1) {
                showInfo("Delete", "Select an item to delete");
                return;
            }
            int mr = inventoryTable.convertRowIndexToModel(sel);
            String sku = model.getValueAt(mr, 0).toString();
            int confirm = JOptionPane.showConfirmDialog(frame, "Delete " + sku + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                db.deleteItem(sku);
                model.removeRow(mr);
                showInfo("Deleted", "Item removed");
            }
        });

        low.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < model.getRowCount(); i++) {
                int qty = Integer.parseInt(model.getValueAt(i, 2).toString());
                int min = Integer.parseInt(model.getValueAt(i, 7).toString());
                if (qty <= min) sb.append(model.getValueAt(i, 0)).append(" - ").append(model.getValueAt(i, 1)).append(" (").append(qty).append(")\n");
            }
            showInfo("Low stock items", sb.length() == 0 ? "None" : sb.toString());
        });

        container.add(top, BorderLayout.NORTH);
        container.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        return container;
    }

    private JPanel buildExitPanel() {
        JPanel container = cardContainer("Confirm Sale");
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<String> nameBox = new JComboBox<>();
        updateNameComboFromModel(nameBox);
        model.addTableModelListener(e -> updateNameComboFromModel(nameBox));
        JTextField qtyField = new JTextField();
        qtyField.putClientProperty("JTextField.placeholderText", "Qty to sell");
        JButton sellBtn = new JButton("💳 Confirm Sale");

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Select Item:"), gbc);
        gbc.gridx = 1; form.add(nameBox, gbc);
        gbc.gridy = 1; gbc.gridx = 0; form.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1; form.add(qtyField, gbc);
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2; form.add(sellBtn, gbc);

        sellBtn.addActionListener(e -> {
            String name = (String) nameBox.getSelectedItem();
            String q = qtyField.getText().trim();
            if (name == null || q.isEmpty()) {
                showInfo("Error", "Select item and qty");
                return;
            }
            try {
                int qty = Integer.parseInt(q);
                if (qty <= 0) throw new Exception();
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (name.equals(model.getValueAt(i, 1))) {
                        int cur = Integer.parseInt(model.getValueAt(i, 2).toString());
                        if (qty > cur) { showInfo("Error", "Sale exceeds stock"); return; }
                        String sku = model.getValueAt(i, 0).toString();
                        String category = model.getValueAt(i, 5).toString();
                        double sell = Double.parseDouble(model.getValueAt(i, 4).toString());
                        int newQty = cur - qty;

                        db.updateQuantity(sku, newQty);
                        model.setValueAt(newQty, i, 2);

                        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        db.insertSale(sku, name, category, qty, sell, ts);

                        showInfo("Sold", "Sale recorded. Remaining: " + newQty);
                        qtyField.setText("");
                        break;
                    }
                }
            } catch (Exception ex) {
                showInfo("Error", "Invalid qty");
            }
        });

        container.add(form, BorderLayout.NORTH);
        return container;
    }

    private JPanel buildReportsPanel() {
        JPanel container = cardContainer("Reports");

        if ("Staff".equals(currentRole)) {
            container.add(new JLabel("Access denied: Staff cannot view reports."), BorderLayout.CENTER);
            return container;
        }

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton stockBtn = new JButton("📦 Stock Summary");
        JButton salesBtn = new JButton("💸 Sales Report");
        top.add(stockBtn); top.add(salesBtn);

        JTextArea out = new JTextArea(18, 60);
        out.setEditable(false);

        container.add(top, BorderLayout.NORTH);
        container.add(new JScrollPane(out), BorderLayout.CENTER);

        stockBtn.addActionListener(e -> {
            int total = model.getRowCount();
            double value = db.totalInventoryValue();
            StringBuilder low = new StringBuilder();
            for (int i = 0; i < model.getRowCount(); i++) {
                int q = Integer.parseInt(model.getValueAt(i, 2).toString());
                int mn = Integer.parseInt(model.getValueAt(i, 7).toString());
                if (q <= mn) low.append(model.getValueAt(i, 0)).append(" - ").append(model.getValueAt(i, 1)).append("\n");
            }
            out.setText("Total items: " + total +
                    "\nTotal inventory value: $" + String.format("%.2f", value) +
                    "\nLow stock:\n" + (low.length() == 0 ? "None" : low.toString()));
        });

        salesBtn.addActionListener(e -> {
            List<String> lines = db.fetchSalesLines();
            out.setText(lines.isEmpty() ? "No sales yet" : String.join("\n", lines));
        });

        return container;
    }

    private JPanel buildDashboardPanel() {
        JPanel container = cardContainer("Dashboard");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Overview"));
        container.add(top, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 12));
        charts.add(new InventoryValueChart());
        charts.add(new CategoryPieChart());
        container.add(charts, BorderLayout.CENTER);
        return container;
    }

    // -------------------- Utils --------------------
    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateSKUComboFromModel(JComboBox<String> box) {
        box.removeAllItems();
        for (int i = 0; i < model.getRowCount(); i++)
            box.addItem(model.getValueAt(i, 0).toString());
    }

    private void updateNameComboFromModel(JComboBox<String> box) {
        box.removeAllItems();
        for (int i = 0; i < model.getRowCount(); i++)
            box.addItem(model.getValueAt(i, 1).toString());
    }

    // -------------------- Renderer & Charts --------------------
    private static class ModernTableCellRenderer extends JLabel implements javax.swing.table.TableCellRenderer {
        public ModernTableCellRenderer() {
            setOpaque(true);
            setBorder(new EmptyBorder(4, 8, 4, 8));
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                Color bg = (row % 2 == 0) ? new Color(245,245,245) : Color.WHITE;
                setBackground(bg);
                setForeground(Color.BLACK);
            }
            try {
                int modelRow = table.convertRowIndexToModel(row);
                int qty = Integer.parseInt(table.getModel().getValueAt(modelRow, 2).toString());
                int min = Integer.parseInt(table.getModel().getValueAt(modelRow, 7).toString());
                if (qty <= min && !isSelected) setBackground(new Color(255, 220, 220));
            } catch (Exception ignored) {}
            return this;
        }
    }

    private class InventoryValueChart extends JPanel {
        public InventoryValueChart() { setPreferredSize(new Dimension(400, 300)); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Map<String, Double> map = new LinkedHashMap<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                String cat = model.getValueAt(i, 5).toString();
                int q = Integer.parseInt(model.getValueAt(i, 2).toString());
                double c = Double.parseDouble(model.getValueAt(i, 3).toString());
                map.put(cat, map.getOrDefault(cat, 0.0) + q * c);
            }
            int w = getWidth(), h = getHeight();
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Inventory Value by Category", 12, 20);
            if (map.isEmpty()) { g2.drawString("No data", 12, 40); g2.dispose(); return; }
            double max = map.values().stream().mapToDouble(d -> d).max().orElse(1);
            int x = 20, barW = Math.max(40, (w - 60) / Math.max(1, map.size()));
            int i = 0;
            for (Map.Entry<String, Double> e : map.entrySet()) {
                int bh = (int) ((h - 80) * (e.getValue() / max));
                g2.setColor(new Color(90, 160, 250));
                g2.fillRoundRect(x + i * barW, h - 30 - bh, barW - 14, bh, 8, 8);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(e.getKey(), x + i * barW, h - 10);
                i++;
            }
            g2.dispose();
        }
    }

    private class CategoryPieChart extends JPanel {
        public CategoryPieChart() { setPreferredSize(new Dimension(400, 300)); }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Map<String, Integer> map = new LinkedHashMap<>();
            int total = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                String cat = model.getValueAt(i, 5).toString();
                int q = Integer.parseInt(model.getValueAt(i, 2).toString());
                map.put(cat, map.getOrDefault(cat, 0) + q);
                total += q;
            }
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Stock Distribution by Category", 12, 20);
            if (map.isEmpty()) { g2.drawString("No data", 12, 40); g2.dispose(); return; }
            int cx = getWidth() / 2 + 20, cy = getHeight() / 2 + 10, r = Math.min(getWidth(), getHeight()) / 4;
            double start = 0; int idx = 0;
            Color[] palette = new Color[]{ new Color(120,200,120), new Color(200,120,120), new Color(120,160,200), new Color(200,160,120) };
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                double ang = e.getValue() * 360.0 / Math.max(1, total);
                g2.setColor(palette[idx % palette.length]);
                g2.fillArc(cx - r, cy - r, r * 2, r * 2, (int) start, (int) Math.ceil(ang));
                start += ang; idx++;
            }
            int lx = 16, ly = 50; idx = 0;
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                g2.setColor(palette[idx % palette.length]);
                g2.fillRect(lx, ly + idx * 20, 12, 12);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(e.getKey() + " (" + e.getValue() + ")", lx + 18, ly + 12 + idx * 20);
                idx++;
            }
            g2.dispose();
        }
    }
}
