package org.gemseeker.app.data;

import java.time.LocalDate;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class OrderItem implements IEntry {

    private int id;
    private LocalDate date;
    private int orderId;
    private int productId;
    private int quantity;
    private double total;   // quantity * unitPrice

    private Product product;
    private ShipperStock shipperStock;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
    

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ShipperStock getShipperStock() {
        return shipperStock;
    }

    public void setShipperStock(ShipperStock shipperStock) {
        this.shipperStock = shipperStock;
    }
    
    @Override
    public String insertSQL() {
        return String.format("INSERT INTO order_items ("
                + "date, order_id, product_id, quantity, total) VALUES ("
                + "'%s', '%d', '%d', '%d', '%f')",
                date, orderId, productId, quantity, total);
    }
    
    public String updateSQL() {
        return String.format("UPDATE order_items SET date='%s', quantity='%d', total='%f' WHERE id='%d'",
                date, quantity, total, id);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", product.getName(), product.getSupplier());
    }
}
