package org.gemseeker.app.data;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import org.gemseeker.app.Settings;
import org.gemseeker.app.Utils;
import org.gemseeker.app.data.frameworks.IEntry;
import org.xml.sax.SAXException;

/**
 *
 * @author Gem
 */
public class EmbeddedDatabase {
    
    private static EmbeddedDatabase instance;
    private Connection connection;
    private Properties properties;
    
    private boolean mReset = false;
    
    private EmbeddedDatabase() throws 
            ClassNotFoundException, 
            SQLException, 
            ParserConfigurationException, 
            SAXException, 
            IOException {
        initProperties();
        openDatabase();
        createTables();
        updateDatabase();
    }

    public static EmbeddedDatabase getInstance() throws 
            ClassNotFoundException, 
            SQLException, 
            ParserConfigurationException, 
            SAXException, 
            IOException {
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
    
    private void initProperties() throws ParserConfigurationException, SAXException, IOException {
        Settings settings = Settings.getInstance();
        properties = new Properties();
        properties.put("user", settings.getDatabaseValue("user"));
        properties.put("password", settings.getDatabaseValue("password"));
    }
    
    private void openDatabase() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        String dbUrl = "jdbc:h2:file:" + Utils.DATABASE_PATH;
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
            for (String sql : DatabaseUtils.alterTables()) {
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
            try (PreparedStatement statement = connection.prepareStatement(entry.insertSQL(),
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.executeUpdate();
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }
    
    public boolean updateEntry(String table, String col, Object colValue, String keyCol, Object keyValue) 
            throws SQLException {
        String sql = String.format("UPDATE %s SET %s='%s' WHERE %s='%s'", table, col, colValue, keyCol, keyValue);
        return executeQuery(sql);
    }
    
    public boolean deleteEntry(String table, String keyColumn, Object keyValue) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE %s='%s'", table, keyColumn, keyValue);
        return executeQuery(sql);
    }
    
    // =========================================================================
    // Products
    
    /**
     * Retrieve the list of Product and their Stock data.
     * 
     * @return Product data entry list
     * @throws SQLException 
     */
    public ArrayList<Product> getProducts() throws SQLException {
        ArrayList<Product> products = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM products INNER JOIN stocks ON "
                        + "stocks.product_id = products.id");
                while (rs.next()) {
                    Product p = fetchProductAndStockInfo(rs);
                    products.add(p);
                }
            }
        }
        return products;
    }
    
    /**
     * Retrieves the Product entry with its Stock data.
     * 
     * @param id Product ID
     * @return Product data entry
     * @throws SQLException 
     */
    public Product getProductById(int id) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM products INNER JOIN stocks ON "
                        + "stocks.product_id = products.id WHERE products.id = '" + id + "' LIMIT 1");
                if (rs.next()) return fetchProductAndStockInfo(rs);
            }
        }
        return null;
    }
    
    private Product fetchProductAndStockInfo(ResultSet rs) throws SQLException {
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

        return product;
    }
    
    // =========================================================================
    // Stocks
    
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
    
    
    // =========================================================================
    // Shippers
    
    public Shipper getShipperByName(String name) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM shippers WHERE name='" + name + "' LIMIT 1");
                if (rs.next()) return fetchShipperInfo(rs);
            }
        }
        return null;
    }
    
    public Shipper getShipperById(int id) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM shippers WHERE id='%d' LIMIT 1", id);
                ResultSet rs = statement.executeQuery(sql);
                if (rs.next()) return fetchShipperInfo(rs);
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
                    Shipper s = fetchShipperInfo(rs);
                    shippers.add(s);
                }
            }
        }
        return shippers;
    }
    
    private Shipper fetchShipperInfo(ResultSet rs) throws SQLException {
        Shipper s = new Shipper();
        s.setId(rs.getInt(1));
        s.setName(rs.getString(2));
        return s;
    }
    
    // =========================================================================
    // ShipperStocks
    
    public ArrayList<ShipperStock> getShipperStocks() throws SQLException {
        ArrayList<ShipperStock> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM shipper_stocks INNER JOIN products ON "
                        + "products.id = shipper_stocks.product_id");
                while (rs.next()) {
                    ShipperStock stock = fetchShipperStockInfo(rs);
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
                    ShipperStock stock = fetchShipperStockInfo(rs);
                    stocks.add(stock);
                }
            }
        }
        return stocks;
    }
    
    public ShipperStock getShipperStock(int shipperId, int productId) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM shipper_stocks INNER JOIN products ON "
                        + "products.id = shipper_stocks.product_id WHERE shipper_stocks.shipper_id = '%d' AND "
                        + "shipper_stocks.product_id = '%d'", shipperId, productId);
                ResultSet rs = statement.executeQuery(sql);
                if (rs.next()) return fetchShipperStockInfo(rs);
            }
        }
        return null;
    }
    
    public ShipperStock getShipperStockById(int id) throws SQLException {
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM shipper_stocks INNER JOIN products ON "
                        + "products.id = shipper_stocks.product_id WHERE shipper_stocks.id = '%d'", id);
                ResultSet rs = statement.executeQuery(sql);
                if (rs.next()) return fetchShipperStockInfo(rs);
            }
        }
        return null;
    }
    
    private ShipperStock fetchShipperStockInfo(ResultSet rs) throws SQLException {
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
        
        return stock;
    }
    
    // =========================================================================
    // Orders
    
    public ArrayList<Order> getOrders() throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM orders INNER JOIN shippers ON "
                        + "shippers.id = orders.shipper_id");
                while (rs.next()) {
                    Order order = fetchOrderInfo(rs);
                    orders.add(order);
                }
            }
        }
        return orders;
    }
    
    public ArrayList<Order> getOrdersByProductId(int productId) throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = "SELECT * FROM orders INNER JOIN shippers ON shippers.id = orders.shipper_id WHERE orders.id IN "
                        + "(SELECT order_id FROM order_items WHERE order_items.product_id = '"+ productId + "')";
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    Order order = fetchOrderInfo(rs);
                    orders.add(order);
                }
            }
        }
        return orders;
    }
    
    public ArrayList<Order> getOrdersByShipperIdAndDate(int shipperId, LocalDate date) throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                LocalDate first = date.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate last = date.with(TemporalAdjusters.lastDayOfMonth());
                String sql = String.format("SELECT * FROM orders INNER JOIN shippers ON "
                        + "shippers.id = orders.shipper_id "
                        + "WHERE orders.shipper_id='%d' AND orders.date <= '%s' AND orders.date >= '%s'",
                        shipperId, last, first);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    Order order = fetchOrderInfo(rs);
                    orders.add(order);
                }
            }
        }
        return orders;
    }
    
    public ArrayList<Order> getOrdersByDateRange(LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM orders INNER JOIN shippers ON shippers.id = orders.shipper_id "
                        + "WHERE orders.date <= '%s' AND orders.date >= '%s'", dateTo, dateFrom);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    Order order = fetchOrderInfo(rs);
                    orders.add(order);
                }
            }
        }
        return orders;
    }
    
    private Order fetchOrderInfo(ResultSet rs) throws SQLException {
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
        return order;
    }
    
    // =========================================================================
    // OrderItems
    
    public ArrayList<OrderItem> getAllOrderItems() throws SQLException {
        ArrayList<OrderItem> orderItems = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = "SELECT * FROM order_items";
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    OrderItem item = fetchOrderItemInfo(rs);
                    orderItems.add(item);
                }
            }
        }
        return orderItems;
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
                    item.setDate(rs.getDate(index++).toLocalDate());
                    item.setOrderId(rs.getInt(index++));
                    item.setProductId(rs.getInt(index++));
                    item.setQuantity(rs.getInt(index++));
                    item.setTotal(rs.getDouble(index++));
                    
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
    
    public ArrayList<OrderItem> getOrderItemsByDate(LocalDate date) throws SQLException {
        ArrayList<OrderItem> orderItems = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                LocalDate first = date.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate last = date.with(TemporalAdjusters.lastDayOfMonth());
                String sql = String.format("SELECT * FROM order_items "
                        + "WHERE date <= '%s' AND date >= '%s'", last, first);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    OrderItem item = fetchOrderItemInfo(rs);
                    orderItems.add(item);
                }
            }
        }
        return orderItems;
    }
    
    public ArrayList<OrderItem> getOrderItemsByDateRange(LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        ArrayList<OrderItem> orderItems = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM order_items "
                        + "WHERE date <= '%s' AND date >= '%s'", dateTo, dateFrom);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    OrderItem item = fetchOrderItemInfo(rs);
                    orderItems.add(item);
                }
            }
        }
        return orderItems;
    }
    
    private OrderItem fetchOrderItemInfo(ResultSet rs) throws SQLException {
        int index = 1;
        OrderItem item = new OrderItem();
        item.setId(rs.getInt(index++));
        item.setDate(rs.getDate(index++).toLocalDate());
        item.setOrderId(rs.getInt(index++));
        item.setProductId(rs.getInt(index++));
        item.setQuantity(rs.getInt(index++));
        item.setTotal(rs.getDouble(index++));
        return item;
    }
    
    // =========================================================================
    // DeliveryInvoices
    
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
    
    public ArrayList<DeliveryInvoice> getDeliveryInvoices(LocalDate date) throws SQLException {
        ArrayList<DeliveryInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                LocalDate first = date.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate last = date.with(TemporalAdjusters.lastDayOfMonth());
                String sql = String.format("SELECT * FROM delivery_invoices "
                        + "WHERE date <= '%s' AND date >= '%s'", last, first);
                ResultSet rs = statement.executeQuery(sql);
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
    
    public ArrayList<DeliveryInvoice> getDeliveryInvoices(LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        ArrayList<DeliveryInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM delivery_invoices "
                        + "WHERE date <= '%s' AND date >= '%s'", dateTo, dateFrom);
                ResultSet rs = statement.executeQuery(sql);
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
    
    public ArrayList<DeliveryInvoice> getDeliveryInvoices(int shipperId, LocalDate date) throws SQLException {
        ArrayList<DeliveryInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                LocalDate first = date.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate last = date.with(TemporalAdjusters.lastDayOfMonth());
                String sql = String.format("SELECT * FROM delivery_invoices "
                        + "WHERE shipper_id='%d' AND date <= '%s' AND date >= '%s'", shipperId, last, first);
                ResultSet rs = statement.executeQuery(sql);
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
                    item.setDate(rs.getDate(index++).toLocalDate());
                    item.setInvoiceId(rs.getString(index++));
                    item.setProductId(rs.getInt(index++));
                    item.setQuantity(rs.getInt(index++));
                    item.setDiscount(rs.getDouble(index++));
                    item.setDiscountedPrice(rs.getDouble(index++));
                    item.setTotal(rs.getDouble(index++));

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
    
    public ArrayList<DeliveryInvoiceItem> getDeliveryInvoiceItems(LocalDate date) throws SQLException {
        ArrayList<DeliveryInvoiceItem> items = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                LocalDate first = date.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate last = date.with(TemporalAdjusters.lastDayOfMonth());
                String sql = String.format("SELECT * FROM delivery_invoice_items "
                        + "WHERE date <= '%s' AND date >= '%s'", last, first);

                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    DeliveryInvoiceItem item = new DeliveryInvoiceItem();
                    item.setId(rs.getInt(index++));
                    item.setDate(rs.getDate(index++).toLocalDate());
                    item.setInvoiceId(rs.getString(index++));
                    item.setProductId(rs.getInt(index++));
                    item.setQuantity(rs.getInt(index++));
                    item.setDiscount(rs.getDouble(index++));
                    item.setDiscountedPrice(rs.getDouble(index++));
                    item.setTotal(rs.getDouble(index++));
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
    
    public ArrayList<PurchaseInvoice> getPurchaseInvoices(LocalDate date) throws SQLException {
        ArrayList<PurchaseInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                LocalDate first = date.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate last = date.with(TemporalAdjusters.lastDayOfMonth());
                String sql = String.format("SELECT * FROM purchase_invoices "
                        + "WHERE date <= '%s' AND date >= '%s'", last, first);
                ResultSet rs = statement.executeQuery(sql);
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
    
    public ArrayList<PurchaseInvoice> getPurchaseInvoices(LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        ArrayList<PurchaseInvoice> invoices = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM purchase_invoices "
                        + "WHERE date <= '%s' AND date >= '%s'", dateTo, dateFrom);
                ResultSet rs = statement.executeQuery(sql);
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
    
    public ArrayList<PurchaseInvoiceItem> getAllPurchaseInvoiceItems() throws SQLException {
        ArrayList<PurchaseInvoiceItem> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = "SELECT * FROM purchase_invoice_items";
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    PurchaseInvoiceItem pp = new PurchaseInvoiceItem();
                    pp.setId(rs.getInt(index++));
                    pp.setDate(rs.getDate(index++).toLocalDate());
                    pp.setInvoiceId(rs.getString(index++));
                    pp.setProductId(rs.getInt(index++));
                    pp.setUnitPrice(rs.getDouble(index++));
                    pp.setQuantity(rs.getInt(index++));
                    pp.setTotal(rs.getDouble(index++));
                    stocks.add(pp);
                }
            }
        }
        
        return stocks;
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
                    pp.setDate(rs.getDate(index++).toLocalDate());
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
    
    public ArrayList<PurchaseInvoiceItem> getPurchaseInvoiceItems(LocalDate date) throws SQLException {
        ArrayList<PurchaseInvoiceItem> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                LocalDate first = date.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate last = date.with(TemporalAdjusters.lastDayOfMonth());
                String sql = String.format("SELECT * FROM purchase_invoice_items "
                        + "WHERE date <= '%s' AND date >= '%s'", last, first);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    PurchaseInvoiceItem pp = new PurchaseInvoiceItem();
                    pp.setId(rs.getInt(index++));
                    pp.setDate(rs.getDate(index++).toLocalDate());
                    pp.setInvoiceId(rs.getString(index++));
                    pp.setProductId(rs.getInt(index++));
                    pp.setUnitPrice(rs.getDouble(index++));
                    pp.setQuantity(rs.getInt(index++));
                    pp.setTotal(rs.getDouble(index++));
                    stocks.add(pp);
                }
            }
        }
        return stocks;
    }
    
    public ArrayList<PurchaseInvoiceItem> getPurchaseInvoiceItems(LocalDate dateFrom, LocalDate dateTo) throws SQLException {
        ArrayList<PurchaseInvoiceItem> stocks = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("SELECT * FROM purchase_invoice_items "
                        + "WHERE date <= '%s' AND date >= '%s'", dateTo, dateFrom);
                ResultSet rs = statement.executeQuery(sql);
                while (rs.next()) {
                    int index = 1;
                    PurchaseInvoiceItem pp = new PurchaseInvoiceItem();
                    pp.setId(rs.getInt(index++));
                    pp.setDate(rs.getDate(index++).toLocalDate());
                    pp.setInvoiceId(rs.getString(index++));
                    pp.setProductId(rs.getInt(index++));
                    pp.setUnitPrice(rs.getDouble(index++));
                    pp.setQuantity(rs.getInt(index++));
                    pp.setTotal(rs.getDouble(index++));
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
                ResultSet rs = statement.executeQuery("SELECT * FROM customers");
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
    
    public ArrayList<Expense> getExpenses() throws SQLException {
        ArrayList<Expense> expenses = new ArrayList<>();
        if (connection != null) {
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT * FROM expenses");
                while (rs.next()) {
                    int index = 1;
                    Expense e = new Expense();
                    e.setId(rs.getInt(index++));
                    e.setDate(rs.getDate(index++).toLocalDate());
                    e.setCategory(rs.getString(index++));
                    e.setRemarks(rs.getString(index++));
                    e.setRef(rs.getString(index++));
                    e.setAmount(rs.getDouble(index++));
                    expenses.add(e);
                }
            }
        }
        return expenses;
    }
}
