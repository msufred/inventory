package org.gemseeker.app.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class EmbeddedDatabase {
    
    private static EmbeddedDatabase instance;
    private Connection connection;
    private Properties properties;
    
    private EmbeddedDatabase() throws ClassNotFoundException, SQLException {
        initProperties();
        openDatabase();
        createTables();
    }

    public static EmbeddedDatabase getInstance() throws ClassNotFoundException, SQLException {
        if (instance == null) instance = new EmbeddedDatabase();
        return instance;
    }
    
    private void initProperties() {
        properties = new Properties();
        properties.put("user", "admin");
        properties.put("password", "admin");
    }
    
    public void openDatabase() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        String dbUrl = "jdbc:h2:file:" + Utils.getDatabasePath();
        connection = DriverManager.getConnection(dbUrl, properties);
    }
    
    public void closeDatabase() throws SQLException {
        if (connection != null) connection.close();
    }
    
    private void createTables() throws SQLException {
        if (connection != null) {
            for (String sql : DatabaseUtils.tables()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        }
    }
    
    public boolean addEntry(IEntry entry) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                return statement.executeUpdate(entry.insertSQL()) > 0;
            }
        }
        return false;
    }
    
    public int addEntryReturnId(IEntry entry) throws SQLException {
        if (connection != null) {
            try (PreparedStatement statement = connection.prepareStatement(entry.insertSQL(), PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.executeUpdate();
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }
    
    public boolean updateEntry(String table, String col, Object colValue, String keyCol, Object keyValue) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("UPDATE %s SET %s='%s' WHERE %s='%s'",
                        table, col, colValue, keyCol, keyValue);
                return statement.executeUpdate(sql) > 0;
            }
        }
        return false;
    }
    
    // =====================================================
    
    public ArrayList<Product> getProducts() throws SQLException {
        ArrayList<Product> products = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM products");
                while (rs.next()) {
                    Product product = new Product();
                    product.setId(rs.getInt(1));
                    product.setName(rs.getString(2));
                    product.setSku(rs.getString(3));
                    product.setSupplier(rs.getString(4));
                    product.setUnit(rs.getString(5));
                    product.setUnitPrice(rs.getDouble(6));
                    products.add(product);
                }
            }
        }
        return products;
    }
    
    public ArrayList<Stock> getStocks() throws SQLException {
        ArrayList<Stock> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM products INNER JOIN stocks ON "
                        + "stocks.product_id = products.id");
                while (rs.next()) {
                    Product product = new Product();
                    product.setId(rs.getInt(1));
                    product.setName(rs.getString(2));
                    product.setSku(rs.getString(3));
                    product.setSupplier(rs.getString(4));
                    product.setUnit(rs.getString(5));
                    product.setUnitPrice(rs.getDouble(6));
                    
                    Stock stock = new Stock();
                    stock.setId(rs.getInt(7));
                    stock.setProductId(rs.getInt(8));
                    stock.setQuantity(rs.getInt(9));
                    stock.setQuantityOut(rs.getInt(10));
                    stock.setProduct(product);
                    stocks.add(stock);
                }
            }
        }
        return stocks;
    }
    
    public Stock getStock(int productId) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery(String.format("SELECT * FROM stocks WHERE product_id = '%d' LIMIT 1", productId));
                if (rs.next()) {
                    Stock stock = new Stock();
                    stock.setId(rs.getInt(1));
                    stock.setProductId(rs.getInt(2));
                    stock.setQuantity(rs.getInt(3));
                    stock.setQuantityOut(rs.getInt(4));
                    return stock;
                }
            }
        }
        return null;
    }
    
    public ArrayList<Order> getOrders() throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM orders");
                while (rs.next()) {
                    Order order = new Order();
                    order.setId(rs.getInt(1));
                    order.setDate(rs.getDate(2).toLocalDate());
                    order.setName(rs.getString(3));
                    order.setTotal(rs.getDouble(4));
                    orders.add(order);
                }
            }
        }
        return orders;
    }
    
    public ArrayList<OrderItem> getOrderItems(int orderId) throws SQLException {
        ArrayList<OrderItem> orderItems = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery(String.format("SELECT * FROM order_items "
                        + "INNER JOIN products ON order_items.product_id = products.id WHERE order_id='%d'", orderId));
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt(1));
                    item.setOrderId(rs.getInt(2));
                    item.setProductId(rs.getInt(3));
                    item.setDiscount(rs.getDouble(4));
                    item.setUnitPrice(rs.getDouble(5));
                    item.setQuantity(rs.getInt(6));
                    item.setListPrice(rs.getDouble(7));
                    item.setQuantityOut(rs.getInt(8));
                    item.setTotalOut(rs.getDouble(9));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(10));
                    product.setName(rs.getString(11));
                    product.setSku(rs.getString(12));
                    product.setSupplier(rs.getString(13));
                    product.setUnit(rs.getString(14));
                    product.setUnitPrice(rs.getDouble(15));
                    
                    item.setProduct(product);
                    orderItems.add(item);
                }
            }
        }
        return orderItems;
    }
}