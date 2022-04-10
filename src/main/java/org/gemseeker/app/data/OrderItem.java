package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class OrderItem implements IEntry {

    private int id;
    private int orderId;
    private int productId;
    private int quantity;
    private double listPrice;   // quantity * unitPrice

    private Product product;
    private ShipperStock shipperStock;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public double getListPrice() {
        return listPrice;
    }

    public void setListPrice(double listPrice) {
        this.listPrice = listPrice;
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
                + "order_id, product_id, quantity, list_price) VALUES ("
                + "'%d', '%d', '%d', '%f')",
                orderId, productId, quantity, listPrice);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", product.getName(), product.getSupplier());
    }
}
