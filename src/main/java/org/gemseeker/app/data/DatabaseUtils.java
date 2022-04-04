package org.gemseeker.app.data;

/**
 *
 * @author Gem
 */
public class DatabaseUtils {

    public static String[] tables() {
        return new String[] {
            createProductsTable(), createStocksTable(), createOrdersTable(),
            createOrderItemsTable(), createInvoicesTable(), createInvoiceItemsTable()
        };
    }
    
    public static String createProductsTable() {
        return "CREATE TABLE IF NOT EXISTS products ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "name VARCHAR(255) NOT NULL, "
                + "sku VARCHAR(255), "
                + "supplier VARCHAR(255), "
                + "unit VARCHAR(100) NOT NULL, "
                + "unit_price DOUBLE DEFAULT 0, "
                + "PRIMARY KEY (id)"
                + ")";
    }
    
    public static String createStocksTable() {
        return "CREATE TABLE IF NOT EXISTS stocks ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "product_id INT NOT NULL, "
                + "quantity INT NOT NULL, "
                + "quantity_out INT NOT NULL, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_product_1 FOREIGN KEY (product_id) REFERENCES products (id)"
                + ")";
    }
    
    public static String createOrdersTable() {
        return "CREATE TABLE IF NOT EXISTS orders ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "date DATE NOT NULL, "
                + "name VARCHAR(255) NOT NULL, "    // truck no or driver name
                + "total DOUBLE DEFAULT 0, "        // total amount of order
                + "PRIMARY KEY (id)"
                + ")";
    }
    
    public static String createOrderItemsTable() {
        return "CREATE TABLE IF NOT EXISTS order_items ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "order_id INT NOT NULL, "
                + "product_id INT NOT NULL, "
                + "discount DOUBLE DEFAULT 0, "             // discount per unit
                + "discounted_price DOUBLE DEFAULT 0, "     // product price * discount per unit
                + "quantity INT DEFAULT 0, "
                + "list_price DOUBLE DEFAULT 0, "           // discounted price * quantity (projected total)
                + "quantity_out INT DEFAULT 0, "
                + "total_out DOUBLE DEFAULT 0, "            // actual total of item out
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_order_1 FOREIGN KEY (order_id) REFERENCES orders (id), "
                + "CONSTRAINT fk_product_2 FOREIGN KEY (product_Id) REFERENCES products (id)"
                + ")";
    }
    
    public static String createInvoicesTable() {
        return "CREATE TABLE IF NOT EXISTS invoices ("
                + "id VARCHAR(50) NOT NULL, "
                + "order_id INT NOT NULL, "
                + "date DATE NOT NULL, "
                + "customer VARCHAR(255) NOT NULL, "
                + "address VARCHAR(255), "
                + "total DOUBLE DEFAULT 0, "
                + "payment_type VARCHAR(100) NOT NULL, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_order_2 FOREIGN KEY (order_id) REFERENCES orders (id)"
                + ")";
    }
    
    public static String createInvoiceItemsTable() {
        return "CREATE TABLE IF NOT EXISTS invoice_items ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "invoice_id VARCHAR(50) NOT NULL, "
                + "product_id INT NOT NULL, "
                + "quantity INT DEFAULT 0, "
                + "discount DOUBLE DEFAULT 0, "
                + "discounted_price DOUBLE DEFAULT 0, "
                + "list_price DOUBLE DEFAULT 0, "
                + "PRIMARY KEY (id), "
                + "CONSTRAINT fk_invoice_1 FOREIGN KEY (invoice_id) REFERENCES invoices (id), "
                + "CONSTRAINT fk_product_3 FOREIGN KEY (product_id) REFERENCES products (id)"
                + ")";
    }
}
