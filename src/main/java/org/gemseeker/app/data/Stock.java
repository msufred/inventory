package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class Stock implements IEntry {
    
    private int id;
    private int productId;
    private int quantity;
    private int quantityOut;
    private int inStock;

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

    public int getInStock() {
        return inStock;
    }

    public void setInStock(int inStock) {
        this.inStock = inStock;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO stocks ("
                + "product_id, quantity, quantity_out, in_stock) "
                + "VALUES ('%d', '%d', '%d', '%d')",
                productId, quantity, quantityOut, inStock);
    }
    
    public String updateSQL() {
        return String.format("UPDATE stocks SET quantity='%d', quantity_out='%d', in_stock='%d'",
                quantity, quantityOut, inStock);
    }

}
