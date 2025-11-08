import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBHelper {
    private static final String DB_URL = "jdbc:sqlite:inventory.db";

    static {
        try {
            // Ensure SQLite JDBC driver is loaded
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found. Add sqlite-jdbc JAR to classpath.", e);
        }
    }

    public DBHelper() {
        init();
    }

    private void init() {
        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS users(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('Admin','Manager','Staff'))
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory(
                    sku TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    cost_price REAL NOT NULL,
                    sell_price REAL NOT NULL,
                    category TEXT,
                    location TEXT,
                    min_stock INTEGER DEFAULT 0
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS sales(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sku TEXT NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT,
                    qty INTEGER NOT NULL,
                    price REAL NOT NULL,
                    timestamp TEXT NOT NULL
                )
            """);

            // Seed users if empty
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM users")) {
                if (rs.next() && rs.getInt("c") == 0) {
                    st.executeUpdate("INSERT INTO users(username,password,role) VALUES" +
                            "('admin','admin123','Admin')," +
                            "('manager','manager123','Manager')," +
                            "('staff','staff123','Staff')");
                }
            }

            // Seed inventory if empty
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM inventory")) {
                if (rs.next() && rs.getInt("c") == 0) {
                    st.executeUpdate("INSERT INTO inventory(sku,name,quantity,cost_price,sell_price,category,location,min_stock) VALUES" +
                            "('UQ001','USB Cable',120,1.5,3.5,'Electronics','Shelf 1',10)," +
                            "('UQ002','T-Shirt',30,5.0,12.0,'Clothing','Shelf 2',5)," +
                            "('UQ003','Chips',200,0.5,1.2,'Food','Warehouse A',20)");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ---------- Auth ----------
    public String authenticateRole(String username, String password) {
        String sql = "SELECT role FROM users WHERE username=? AND password=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("role");
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Auth failed: " + e.getMessage(), e);
        }
    }

    // ---------- Inventory ----------
    public List<Object[]> fetchInventory() {
        String sql = "SELECT sku,name,quantity,cost_price,sell_price,category,location,min_stock FROM inventory ORDER BY sku";
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getString(1), rs.getString(2), rs.getInt(3),
                        rs.getDouble(4), rs.getDouble(5),
                        rs.getString(6), rs.getString(7), rs.getInt(8)
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fetch inventory failed: " + e.getMessage(), e);
        }
        return rows;
    }

    public void insertItem(String sku, String name, int qty, double cost, double sell, String cat, String loc, int min) {
        String sql = "INSERT INTO inventory(sku,name,quantity,cost_price,sell_price,category,location,min_stock) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sku); ps.setString(2, name); ps.setInt(3, qty);
            ps.setDouble(4, cost); ps.setDouble(5, sell);
            ps.setString(6, cat); ps.setString(7, loc); ps.setInt(8, min);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Insert item failed: " + e.getMessage(), e);
        }
    }

    public void updateQuantity(String sku, int newQty) {
        String sql = "UPDATE inventory SET quantity=? WHERE sku=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, newQty);
            ps.setString(2, sku);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update quantity failed: " + e.getMessage(), e);
        }
    }

    public void deleteItem(String sku) {
        String sql = "DELETE FROM inventory WHERE sku=?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Delete failed: " + e.getMessage(), e);
        }
    }

    // ---------- Sales ----------
    public void insertSale(String sku, String name, String category, int qty, double price, String ts) {
        String sql = "INSERT INTO sales(sku,name,category,qty,price,timestamp) VALUES(?,?,?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sku); ps.setString(2, name); ps.setString(3, category);
            ps.setInt(4, qty); ps.setDouble(5, price); ps.setString(6, ts);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Insert sale failed: " + e.getMessage(), e);
        }
    }

    public List<String> fetchSalesLines() {
        String sql = "SELECT timestamp, sku, name, qty, price FROM sales ORDER BY id DESC";
        List<String> lines = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lines.add(
                        rs.getString(1) + " - " + rs.getString(2) + " - " +
                                rs.getString(3) + " x" + rs.getInt(4) + " @ " +
                                String.format("%.2f", rs.getDouble(5))
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fetch sales failed: " + e.getMessage(), e);
        }
        return lines;
    }

    // ---------- Reports ----------
    public double totalInventoryValue() {
        String sql = "SELECT SUM(quantity * cost_price) FROM inventory";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        } catch (SQLException e) {
            throw new RuntimeException("Report failed: " + e.getMessage(), e);
        }
    }
}
