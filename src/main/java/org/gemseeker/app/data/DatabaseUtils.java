package org.gemseeker.app.data;

/**
 *
 * @author Gem
 */
public class DatabaseUtils {

    public static String[] tables() {
        return new String[] {
            createProductsTable(),              // Products
            createStocksTable(),                // Stocks
            createShippersTable(),              // Shippers
            createShipperStocksTable(),         // Shipper Stocks
            createOrdersTable(),                // Orders
            createOrderItemsTable(),            // Order Items
            createDeliveryInvoicesTable(),      // Delivery Invoices
            createDeliveryInvoiceItemsTable(),  // Delivery Invoice Items
            createPurchaseInvoicesTable(),      // Purchase Invoices
            createPurchaseInvoiceItemsTable(),  // Purchase Invoice Products
            createSuppliersTable(),             // Suppliers
            createCustomersTable(),             // Customers
        };
    }
    
    public static String[] dropTables() {
        return new String[]{
            // 1.0.0-beta-02
            "DROP TABLE IF EXISTS customers",
            "DROP TABLE IF EXISTS suppliers",
            "DROP TABLE IF EXISTS purchase_invoice_items",
            "DROP TABLE IF EXISTS purchase_invoices",
            "DROP TABLE IF EXISTS delivery_invoice_items",
            "DROP TABLE IF EXISTS delivery_invoices",
            "DROP TABLE IF EXISTS order_items",
            "DROP TABLE IF EXISTS orders",
            "DROP TABLE IF EXISTS shipper_stocks",
            "DROP TABLE IF EXISTS shippers",
            "DROP TABLE IF EXISTS stocks",
            "DROP TABLE IF EXISTS products",
            
            // 1.0.0-beta-01
            "DROP TABLE IF EXISTS invoice_items",
            "DROP TABLE IF EXISTS invoices",
        };
    }
    
    public static String createProductsTable() {
        return "CREATE TABLE IF NOT EXISTS products ("
                + "id INT NOT NULL AUTO_INCREMENT, "    // 1    id
                + "name VARCHAR(255) NOT NULL, "        // 2    name
                + "sku VARCHAR(255), "                  // 3    sku
                + "supplier VARCHAR(255), "             // 4    supplier
                + "unit VARCHAR(100) NOT NULL, "        // 5    unit
                + "unit_price DOUBLE DEFAULT 0, "       // 6    unit_price
                + "retail_price DOUBLE DEFAULT 0, "     // 7    retail_price
                + "PRIMARY KEY (id)"
                + ")";
    }
    
    public static String createStocksTable() {
        return "CREATE TABLE IF NOT EXISTS stocks ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "product_id INT NOT NULL, "
                + "quantity INT DEFAULT 0, "
                + "quantity_out INT DEFAULT 0, "
                + "in_stock INT DEFAULT 0, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_product_1 FOREIGN KEY (product_id) REFERENCES products (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ")";
    }
    
    public static String createShippersTable() {
        return "CREATE TABLE IF NOT EXISTS shippers ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "name VARCHAR(255), "
                + "PRIMARY KEY (id)"
                + ")";
    }
    
    public static String createShipperStocksTable() {
        return "CREATE TABLE IF NOT EXISTS shipper_stocks ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "shipper_id INT NOT NULL, "
                + "product_id INT NOT NULL, "
                + "in_stock INT DEFAULT 0, "
                + "delivered INT DEFAULT 0, "
                + "sales DOUBLE DEFAULT 0, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_shipper_1 FOREIGN KEY (shipper_id) REFERENCES shippers (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "CONSTRAINT fk_product_2 FOREIGN KEY (product_id) REFERENCES products (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ")";
    }
    
    public static String createOrdersTable() {
        return "CREATE TABLE IF NOT EXISTS orders ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "date DATE NOT NULL, "
                + "shipper_id INT NOT NULL, "    // truck no or driver name
                + "total DOUBLE DEFAULT 0, "        // total amount of ordered products
                + "sales DOUBLE DEFAULT 0, "        // total sales from this order
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_shipper_2 FOREIGN KEY (shipper_id) REFERENCES shippers (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ")";
    }
    
    public static String createOrderItemsTable() {
        return "CREATE TABLE IF NOT EXISTS order_items ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "order_id INT NOT NULL, "
                + "product_id INT NOT NULL, "
                + "quantity INT DEFAULT 0, "
                + "list_price DOUBLE DEFAULT 0, "           // discounted price * quantity (projected total
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_order_1 FOREIGN KEY (order_id) REFERENCES orders (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "CONSTRAINT fk_product_3 FOREIGN KEY (product_Id) REFERENCES products (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ")";
    }
    
    public static String createDeliveryInvoicesTable() {
        return "CREATE TABLE IF NOT EXISTS delivery_invoices ("
                + "id VARCHAR(50) NOT NULL, "
                + "shipper_id INT NOT NULL, "
                + "date DATE NOT NULL, "
                + "customer VARCHAR(255) NOT NULL, "
                + "address VARCHAR(255), "
                + "total DOUBLE DEFAULT 0, "
                + "payment_type VARCHAR(100) NOT NULL, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_shipper_3 FOREIGN KEY (shipper_id) REFERENCES shippers (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ")";
    }
    
    public static String createDeliveryInvoiceItemsTable() {
        return "CREATE TABLE IF NOT EXISTS delivery_invoice_items ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "invoice_id VARCHAR(50) NOT NULL, "   // delivery invoice id
                + "product_id INT NOT NULL, "
                + "quantity INT DEFAULT 0, "
                + "discount DOUBLE DEFAULT 0, "
                + "discounted_price DOUBLE DEFAULT 0, "
                + "list_price DOUBLE DEFAULT 0, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_invoice_1 FOREIGN KEY (invoice_id) REFERENCES delivery_invoices (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "CONSTRAINT fk_product_4 FOREIGN KEY (product_id) REFERENCES products (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ")";
    }
    
    public static String createPurchaseInvoicesTable() {
        return "CREATE TABLE IF NOT EXISTS purchase_invoices ("
                + "id VARCHAR(100) NOT NULL, "
                + "date DATE NOT NULL, "
                + "supplier VARCHAR(255) NOT NULL, "
                + "total DOUBLE DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ")";
    }
    
    public static String createPurchaseInvoiceItemsTable() {
        return "CREATE TABLE IF NOT EXISTS purchase_invoice_items ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "invoice_id VARCHAR(100) NOT NULL, "
                + "product_id INT NOT NULL, "
                + "unit_price DOUBLE DEFAULT 0, "
                + "quantity INT DEFAULT 0, "
                + "total DOUBLE DEFAULT 0, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_product_5 FOREIGN KEY (product_id) REFERENCES products (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "CONSTRAINT fk_purchase_invoice_1 FOREIGN KEY (invoice_id) REFERENCES purchase_invoices (id) "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ")";
    }
    
    public static String createSuppliersTable() {
        return "CREATE TABLE IF NOT EXISTS suppliers ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "name VARCHAR(255) NOT NULL, "
                + "PRIMARY KEY (id)"
                + ")";
    }
    
    public static String createCustomersTable() {
        return "CREATE TABLE IF NOT EXISTS customers ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "name VARCHAR(255) NOT NULL, "
                + "address VARCHAR(255), "
                + "PRIMARY KEY (id)"
                + ")";
    }
}
