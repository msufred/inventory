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
    
    private boolean mReset = false;
    
    private EmbeddedDatabase() throws ClassNotFoundException, SQLException {
        initProperties();
        openDatabase();
        createTables();
        updateDatabase();
    }

    public static EmbeddedDatabase getInstance() throws ClassNotFoundException, SQLException {
        if (instance == null) instance = new EmbeddedDatabase();
        if (instance.mReset) {
            if (instance.connection == null) {
                instance.initProperties();
                instance.openDatabase();
            }
            instance.createTables();
            instance.updateDatabase();
        }
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
            
            // TODO add sql strings to sqls list
            
            for (String sql : sqls) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        }
    }
    
    public void reset() throws SQLException {
        if (connection != null) {
            for (String sql : DatabaseUtils.dropTables()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            mReset = true;
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
                ResultSet rs = statement.executeQuery("SELECT * FROM products INNER JOIN stocks ON "
                        + "stocks.product_id = products.id");
                while (rs.next()) {
                    int index = 1;
                    Product product = new Product();
                    product.setId(rs.getInt(index++));
                    product.setName(rs.getString(index++));
                    product.setSku(rs.getString(index++));
                    product.setSupplier(rs.getString(index++));
                    product.setUnit(rs.getString(index++));
                    product.setUnitPrice(rs.getDouble(index++));
                    product.setRetailPrice(rs.getDouble(index++));
                    
                    Stock stock = new Stock();
                    stock.setId(rs.getInt(index++));
                    stock.setProductId(rs.getInt(index++));
                    stock.setQuantity(rs.getInt(index++));
                    stock.setQuantityOut(rs.getInt(index++));
                    stock.setInStock(rs.getInt(index++));
                    product.setStock(stock);
                    
                    products.add(product);
                }
            }
        }
        return products;
    }
    
    public Stock getStock(int productId) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery(String.format("SELECT * FROM stocks WHERE product_id = '%d' LIMIT 1", productId));
                if (rs.next()) {
                    int index = 1;
                    Stock stock = new Stock();
                    stock.setId(rs.getInt(index++));
                    stock.setProductId(rs.getInt(index++));
                    stock.setQuantity(rs.getInt(index++));
                    stock.setQuantityOut(rs.getInt(index++));
                    stock.setInStock(rs.getInt(index++));
                    return stock;
                }
            }
        }
        return null;
    }
    
    public Shipper getShipper(String name) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM shippers WHERE name='" + name + "' LIMIT 1");
                if (rs.next()) {
                    Shipper s = new Shipper();
                    s.setId(rs.getInt(1));
                    s.setName(rs.getString(2));
                    return s;
                }
            }
        }
        return null;
    }
    
    public ArrayList<Shipper> getShippers() throws SQLException {
        ArrayList<Shipper> shippers = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM shippers");
                while (rs.next()) {
                    Shipper s = new Shipper();
                    s.setId(rs.getInt(1));
                    s.setName(rs.getString(2));
                    shippers.add(s);
                }
            }
        }
        return shippers;
    }
    
    public ArrayList<ShipperStock> getShipperStocks() throws SQLException {
        ArrayList<ShipperStock> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM shipper_stocks INNER JOIN products ON "
                        + "products.id = shipper_stocks.product_id");
                while (rs.next()) {
                    int index = 1;
                    ShipperStock stock = new ShipperStock();
                    stock.setId(rs.getInt(index++));
                    stock.setShipperId(rs.getInt(index++));
                    stock.setProductId(rs.getInt(index++));
                    stock.setInStock(rs.getInt(index++));
                    stock.setDelivered(rs.getInt(index++));
                    stock.setSales(rs.getDouble(index++));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(index++));
                    product.setName(rs.getString(index++));
                    product.setSku(rs.getString(index++));
                    product.setSupplier(rs.getString(index++));
                    product.setUnit(rs.getString(index++));
                    product.setUnitPrice(rs.getDouble(index++));
                    product.setRetailPrice(rs.getDouble(index++));
                    stock.setProduct(product);
                    
                    stocks.add(stock);
                }
            }
        }
        return stocks;
    }
    
    public ArrayList<ShipperStock> getShipperStocks(int shipperId) throws SQLException {
        ArrayList<ShipperStock> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM shipper_stocks INNER JOIN products ON "
                        + "products.id = shipper_stocks.product_id WHERE shipper_stocks.shipper_id = '" + shipperId + "'");
                while (rs.next()) {
                    int index = 1;
                    ShipperStock stock = new ShipperStock();
                    stock.setId(rs.getInt(index++));
                    stock.setShipperId(rs.getInt(index++));
                    stock.setProductId(rs.getInt(index++));
                    stock.setInStock(rs.getInt(index++));
                    stock.setDelivered(rs.getInt(index++));
                    stock.setSales(rs.getDouble(index++));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(index++));
                    product.setName(rs.getString(index++));
                    product.setSku(rs.getString(index++));
                    product.setSupplier(rs.getString(index++));
                    product.setUnit(rs.getString(index++));
                    product.setUnitPrice(rs.getDouble(index++));
                    product.setRetailPrice(rs.getDouble(index++));
                    stock.setProduct(product);
                    
                    stocks.add(stock);
                }
            }
        }
        return stocks;
    }
    
    public ShipperStock getShipperStock(int shipperId, int productId) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM shipper_stocks WHERE shipper_id = '%d' AND "
                        + "product_id = '%d'", shipperId, productId);
                ResultSet rs = statement.executeQuery(sql);
                if (rs.next()) {
                    int index = 1;
                    ShipperStock stock = new ShipperStock();
                    stock.setId(rs.getInt(index++));
                    stock.setShipperId(rs.getInt(index++));
                    stock.setProductId(rs.getInt(index++));
                    stock.setInStock(rs.getInt(index++));
                    stock.setDelivered(rs.getInt(index++));
                    stock.setSales(rs.getDouble(index++));
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
                ResultSet rs = statement.executeQuery("SELECT * FROM orders INNER JOIN shippers ON "
                        + "shippers.id = orders.shipper_id");
                while (rs.next()) {
                    int index = 1;
                    Order order = new Order();
                    order.setId(rs.getInt(index++));
                    order.setDate(rs.getDate(index++).toLocalDate());
                    order.setShipperId(rs.getInt(index++));
                    order.setTotal(rs.getDouble(index++));
                    order.setSales(rs.getDouble(index++));
                    
                    Shipper shipper = new Shipper();
                    shipper.setId(rs.getInt(index++));
                    shipper.setName(rs.getString(index++));
                    order.setShipper(shipper);
                    
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
                String sql = "SELECT * FROM orders INNER JOIN shippers ON shippers.id = orders.shipper_id WHERE orders.id IN "
                        + "(SELECT order_id FROM order_items WHERE order_items.product_id = '"+ productId + "')";
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    Order order = new Order();
                    order.setId(rs.getInt(index++));
                    order.setDate(rs.getDate(index++).toLocalDate());
                    order.setShipperId(rs.getInt(index++));
                    order.setTotal(rs.getDouble(index++));
                    order.setSales(rs.getDouble(index++));
                    
                    Shipper shipper = new Shipper();
                    shipper.setId(rs.getInt(index++));
                    shipper.setName(rs.getString(index++));
                    order.setShipper(shipper);

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
                String sql = "SELECT * FROM order_items "
                        + "INNER JOIN shipper_stocks ON shipper_stocks.product_id = order_items.product_id "
                        + "INNER JOIN products ON products.id = order_items.product_id "
                        + "WHERE order_items.order_id = '" + orderId + "'";
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt(index++));
                    item.setOrderId(rs.getInt(index++));
                    item.setProductId(rs.getInt(index++));
                    item.setQuantity(rs.getInt(index++));
                    item.setListPrice(rs.getDouble(index++));
                    
                    ShipperStock stock = new ShipperStock();
                    stock.setId(rs.getInt(index++));
                    stock.setShipperId(rs.getInt(index++));
                    stock.setProductId(rs.getInt(index++));
                    stock.setInStock(rs.getInt(index++));
                    stock.setDelivered(rs.getInt(index++));
                    stock.setSales(rs.getDouble(index++));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(index++));
                    product.setName(rs.getString(index++));
                    product.setSku(rs.getString(index++));
                    product.setSupplier(rs.getString(index++));
                    product.setUnit(rs.getString(index++));
                    product.setUnitPrice(rs.getDouble(index++));
                    product.setRetailPrice(rs.getDouble(index++));
                    
                    stock.setProduct(product);
                    item.setProduct(product);
                    item.setShipperStock(stock);
                    orderItems.add(item);
                }
            }
        }
        return orderItems;
    }
    
    public boolean deliveryInvoiceExists(String id) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM delivery_invoices WHERE id='" + id + "'");
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    public ArrayList<DeliveryInvoice> getDeliveryInvoices() throws SQLException {
        ArrayList<DeliveryInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM delivery_invoices");
                while (rs.next()) {
                    int index = 1;
                    DeliveryInvoice invoice = new DeliveryInvoice();
                    invoice.setId(rs.getString(index++));
                    invoice.setShipperId(index++);
                    invoice.setDate(rs.getDate(index++).toLocalDate());
                    invoice.setCustomer(rs.getString(index++));
                    invoice.setAddress(rs.getString(index++));
                    invoice.setTotal(rs.getDouble(index++));
                    invoice.setPaymentType(rs.getString(index++));
                    invoices.add(invoice);
                }
            }
        }
        return invoices;
    }
    
    public ArrayList<DeliveryInvoiceItem> getDeliveryInvoiceItems(String invoiceId) throws SQLException {
        ArrayList<DeliveryInvoiceItem> items = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM delivery_invoice_items INNER JOIN "
                        + "products ON delivery_invoice_items.product_id = products.id WHERE invoice_id='%s'", invoiceId);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    DeliveryInvoiceItem item = new DeliveryInvoiceItem();
                    item.setId(rs.getInt(index++));
                    item.setInvoiceId(rs.getString(index++));
                    item.setProductId(rs.getInt(index++));
                    item.setQuantity(rs.getInt(index++));
                    item.setDiscount(rs.getDouble(index++));
                    item.setDiscountedPrice(rs.getDouble(index++));
                    item.setListPrice(rs.getDouble(index++));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(index++));
                    product.setName(rs.getString(index++));
                    product.setSku(rs.getString(index++));
                    product.setSupplier(rs.getString(index++));
                    product.setUnit(rs.getString(index++));
                    product.setUnitPrice(rs.getDouble(index++));
                    product.setRetailPrice(rs.getDouble(index++));
                    
                    item.setProduct(product);
                    items.add(item);
                }
            }
        }
        return items;
    }
    
    public boolean purchaseInvoiceExists(String id) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM purchase_invoices WHERE id='" + id + "'");
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    
    public ArrayList<PurchaseInvoice> getPurchaseInvoices() throws SQLException {
        ArrayList<PurchaseInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM purchase_invoices");
                while (rs.next()) {
                    int index = 1;
                    PurchaseInvoice invoice = new PurchaseInvoice();
                    invoice.setId(rs.getString(index++));
                    invoice.setDate(rs.getDate(index++).toLocalDate());
                    invoice.setSupplier(rs.getString(index++));
                    invoice.setTotal(rs.getDouble(index++));
                    invoices.add(invoice);
                }
            }
        }
        return invoices;
    }
    
    public ArrayList<PurchaseInvoiceItem> getPurchaseInvoiceItems(String purchaseInvoiceId) throws SQLException {
        ArrayList<PurchaseInvoiceItem> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM purchase_invoice_items "
                        + "INNER JOIN products ON products.id = purchase_invoice_items.product_id "
                        + "INNER JOIN stocks ON stocks.product_id = purchase_invoice_items.product_id "
                        + "WHERE invoice_id='" + purchaseInvoiceId + "'");
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    PurchaseInvoiceItem pp = new PurchaseInvoiceItem();
                    pp.setId(rs.getInt(index++));
                    pp.setInvoiceId(rs.getString(index++));
                    pp.setProductId(rs.getInt(index++));
                    pp.setUnitPrice(rs.getDouble(index++));
                    pp.setQuantity(rs.getInt(index++));
                    pp.setTotal(rs.getDouble(index++));
                    
                    Product product = new Product();
                    product.setId(rs.getInt(index++));
                    product.setName(rs.getString(index++));
                    product.setSku(rs.getString(index++));
                    product.setSupplier(rs.getString(index++));
                    product.setUnit(rs.getString(index++));
                    product.setUnitPrice(rs.getDouble(index++));
                    product.setRetailPrice(rs.getDouble(index++));
                    
                    // TODO fix
                    // same, product has 1 extra column
                    
                    Stock stock = new Stock();
                    stock.setId(rs.getInt(index++));
                    stock.setProductId(rs.getInt(index++));
                    stock.setQuantity(rs.getInt(index++));
                    stock.setQuantityOut(rs.getInt(index++));
                    stock.setInStock(rs.getInt(index++));
                    product.setStock(stock);

                    pp.setProduct(product);
                    stocks.add(pp);
                }
            }
        }
        return stocks;
    }
    
    public ArrayList<Supplier> getSuppliers() throws SQLException {
        ArrayList<Supplier> suppliers = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM suppliers");
                while (rs.next()) {
                    Supplier s = new Supplier();
                    s.setId(rs.getInt(1));
                    s.setName(rs.getString(2));
                    suppliers.add(s);
                }
            }
        }
        return suppliers;
    }
    
    public Supplier getSupplier(String name) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM suppliers WHERE name='" + name + "'");
                if (rs.next()) {
                    Supplier s = new Supplier();
                    s.setId(rs.getInt(1));
                    s.setName(rs.getString(2));
                    return s;
                }
            }
        }
        return null;
    }
    
    public ArrayList<Customer> getCustomers() throws SQLException {
        ArrayList<Customer> customers = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM suppliers");
                while (rs.next()) {
                    Customer c = new Customer();
                    c.setId(rs.getInt(1));
                    c.setName(rs.getString(2));
                    c.setAddress(rs.getString(3));
                    customers.add(c);
                }
            }
        }
        return customers;
    }
    
    public Customer getCustomer(String name) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM customers WHERE name='" + name + "'");
                if (rs.next()) {
                    Customer c = new Customer();
                    c.setId(rs.getInt(1));
                    c.setName(rs.getString(2));
                    c.setAddress(rs.getString(3));
                    return c;
                }
            }
        }
        return null;
    }
    
}
