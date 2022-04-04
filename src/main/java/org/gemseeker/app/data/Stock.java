package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class Stock implements IEntry {
    
    private int id;
    private int productId;
    private int quantity = 0;
    private int quantityOut = 0;
    
    private Product product;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getQuantityOut() {
        return quantityOut;
    }

    public void setQuantityOut(int quantityOut) {
        this.quantityOut = quantityOut;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO stocks ("
                + "product_id, quantity, quantity_out) "
                + "VALUES ('%d', '%d', '%d')",
                productId, quantity, quantityOut);
    }

    @Override
    public String toString() {
        return product.getName();
    }
}
