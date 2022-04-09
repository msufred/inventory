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
        updateDatabase();
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
    
    private void updateDatabase() throws SQLException {
        if (connection != null) {
            ArrayList<String> sqls = new ArrayList<>();
            
            // 1.0.0-beta-02
            sqls.add("ALTER TABLE products ADD COLUMN IF NOT EXISTS total DOUBLE DEFAULT 0");
            
            for (String sql : sqls) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        }
    }
    
    public boolean executeQuery(String sql) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                return statement.executeUpdate(sql) > 0;
            }
        }
        return false;
    }
    
    public boolean addEntry(IEntry entry) throws SQLException {
        return executeQuery(entry.insertSQL());
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
        String sql = String.format("UPDATE %s SET %s='%s' WHERE %s='%s'", table, col, colValue, keyCol, keyValue);
        return executeQuery(sql);
    }
    
    public boolean deleteEntry(String table, String keyColumn, Object keyValue) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE %s='%s'", table, keyColumn, keyValue);
        return executeQuery(sql);
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
                    product.setDate(rs.getDate(2).toLocalDate());
                    product.setName(rs.getString(3));
                    product.setSku(rs.getString(4));
                    product.setSupplier(rs.getString(5));
                    product.setUnit(rs.getString(6));
                    product.setUnitPrice(rs.getDouble(7));
                    product.setTotal(rs.getDouble(8));
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
                    product.setDate(rs.getDate(2).toLocalDate());
                    product.setName(rs.getString(3));
                    product.setSku(rs.getString(4));
                    product.setSupplier(rs.getString(5));
                    product.setUnit(rs.getString(6));
                    product.setUnitPrice(rs.getDouble(7));
                    product.setTotal(rs.getDouble(8));
                    
                    Stock stock = new Stock();
                    stock.setId(rs.getInt(9));
                    stock.setProductId(rs.getInt(10));
                    stock.setQuantity(rs.getInt(11));
                    stock.setQuantityOut(rs.getInt(12));
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
    
    public ArrayList<Order> getOrders(int productId) throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = "SELECT * FROM orders WHERE orders.id IN "
                        + "(SELECT order_id FROM order_items WHERE order_items.product_id = '"+ productId + "')";
                ResultSet rs = statement.executeQuery(sql);
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
                    item.setDiscountedPrice(rs.getDouble(5));
                    item.setQuantity(rs.getInt(6));
                    item.setListPrice(rs.getDouble(7));
                    item.setQuantityOut(rs.getInt(8));
                    item.setTotalOut(rs.getDouble(9));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(10));
                    product.setDate(rs.getDate(11).toLocalDate());
                    product.setName(rs.getString(12));
                    product.setSku(rs.getString(13));
                    product.setSupplier(rs.getString(14));
                    product.setUnit(rs.getString(15));
                    product.setUnitPrice(rs.getDouble(16));
                    product.setTotal(rs.getDouble(17));
                    
                    item.setProduct(product);
                    orderItems.add(item);
                }
            }
        }
        return orderItems;
    }
    
    public ArrayList<Invoice> getInvoices() throws SQLException {
        ArrayList<Invoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM invoices");
                while (rs.next()) {
                    Invoice invoice = new Invoice();
                    invoice.setId(rs.getString(1));
                    invoice.setOrderId(rs.getInt(2));
                    invoice.setDate(rs.getDate(3).toLocalDate());
                    invoice.setCustomer(rs.getString(4));
                    invoice.setAddress(rs.getString(5));
                    invoice.setTotal(rs.getDouble(6));
                    invoice.setPaymentType(rs.getString(7));
                    invoices.add(invoice);
                }
            }
        }
        return invoices;
    }
    
    public ArrayList<InvoiceItem> getInvoiceItems(String invoiceId) throws SQLException {
        ArrayList<InvoiceItem> items = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM invoice_items INNER JOIN "
                        + "products ON invoice_items.product_id = products.id WHERE invoice_id='%s'", invoiceId);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();
                    item.setId(rs.getInt(1));
                    item.setInvoiceId(rs.getString(2));
                    item.setProductId(rs.getInt(3));
                    item.setQuantity(rs.getInt(4));
                    item.setDiscount(rs.getDouble(5));
                    item.setDiscountedPrice(rs.getDouble(6));
                    item.setListPrice(rs.getDouble(7));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(8));
                    product.setDate(rs.getDate(9).toLocalDate());
                    product.setName(rs.getString(10));
                    product.setSku(rs.getString(11));
                    product.setSupplier(rs.getString(12));
                    product.setUnit(rs.getString(13));
                    product.setUnitPrice(rs.getDouble(14));
                    product.setTotal(rs.getDouble(15));
                    
                    item.setProduct(product);
                    items.add(item);
                }
            }
        }
        return items;
    }
    
    public ArrayList<PurchaseInvoice> getAllPurchaseInvoices() throws SQLException {
        ArrayList<PurchaseInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM purchase_invoices");
                while (rs.next()) {
                    PurchaseInvoice invoice = new PurchaseInvoice();
                    invoice.setId(rs.getString(1));
                    invoice.setDate(rs.getDate(2).toLocalDate());
                    invoice.setSupplier(rs.getString(3));
                    invoice.setTotal(rs.getDouble(4));
                    invoices.add(invoice);
                }
            }
        }
        return invoices;
    }
    
    public ArrayList<PurchaseProduct> getPurchaseProducts(String purchaseInvoiceId) throws SQLException {
        ArrayList<PurchaseProduct> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM purchase_products "
                        + "INNER JOIN products ON products.id = purchase_products.product_id "
                        + "INNER JOIN stocks ON stocks.product_id = products.id");
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    PurchaseProduct pp = new PurchaseProduct();
                    pp.setId(rs.getInt(1));
                    pp.setProductId(rs.getInt(2));
                    pp.setInvoiceId(rs.getString(3));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(4));
                    product.setDate(rs.getDate(5).toLocalDate());
                    product.setName(rs.getString(6));
                    product.setSku(rs.getString(7));
                    product.setSupplier(rs.getString(8));
                    product.setUnit(rs.getString(9));
                    product.setUnitPrice(rs.getDouble(10));
                    product.setTotal(rs.getDouble(11));
                    
                    Stock stock = new Stock();
                    stock.setId(rs.getInt(12));
                    stock.setProductId(rs.getInt(13));
                    stock.setQuantity(rs.getInt(14));
                    stock.setQuantityOut(rs.getInt(15));
                    stock.setProduct(product);
                    
                    pp.setProduct(product);
                    pp.setStock(stock);
                    stocks.add(pp);
                }
            }
        }
        return stocks;
    }
}
