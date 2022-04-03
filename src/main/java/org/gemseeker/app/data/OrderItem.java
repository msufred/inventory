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
    private double discount;    // discount per unit
    private double unitPrice;   // price per unit after discount
    private int quantity;
    private double listPrice;   // quantity * unitPrice
    private int quantityOut;    // actual quantity of item out
    private double totalOut;    // actual total of item out

    private Product product;
    
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
    
    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
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
    
    public int getQuantityOut() {
        return quantityOut;
    }

    public void setQuantityOut(int quantityOut) {
        this.quantityOut = quantityOut;
    }

    public double getTotalOut() {
        return totalOut;
    }

    public void setTotalOut(double totalOut) {
        this.totalOut = totalOut;
    }
    
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
    
    @Override
    public String insertSQL() {
        return String.format("INSERT INTO order_items ("
                + "order_id, product_id, discount, unit_price, quantity, "
                + "list_price, quantity_out, total_out) VALUES ("
                + "'%d', '%d', '%f', '%f', '%d', '%f', '%d', '%f')",
                orderId, productId, discount, unitPrice, quantity,
                listPrice, quantityOut, totalOut);
    }

}
