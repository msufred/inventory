package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class ShipperStock implements IEntry {
    
    private int id;
    private int shipperId;
    private int productId;
    private int quantity;
    private double total;
    private int quantityOut = 0;
    private double totalOut = 0;
    
    private Shipper shipper;
    private Product product;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getShipperId() {
        return shipperId;
    }

    public void setShipperId(int shipperId) {
        this.shipperId = shipperId;
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

    public Shipper getShipper() {
        return shipper;
    }

    public void setShipper(Shipper shipper) {
        this.shipper = shipper;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO shipper_stocks (shipper_id, product_id, "
                + "quantity, total, quantity_out, total_out) "
                + "VALUES ('%d', '%d', '%d', '%f', '%d', '%f')",
                shipperId, productId, quantity, total, quantityOut, totalOut);
    }
    
    public String updateSQL() {
        return String.format("UPDATE shipper_stocks SET quantity='%d', total='%f', quantity_out='%d', "
                + "total_out='%f' WHERE id='%d'", quantity, total, quantityOut, totalOut, id);
    }

    @Override
    public String toString() {
        return product.getName() + "(" + product.getSupplier() + ")";
    }
}
