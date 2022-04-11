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
    private int inStock = 0;
    private int delivered = 0;
    private double sales = 0;
    
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

    public int getInStock() {
        return inStock;
    }

    public void setInStock(int inStock) {
        this.inStock = inStock;
    }

    public int getDelivered() {
        return delivered;
    }

    public void setDelivered(int delivered) {
        this.delivered = delivered;
    }

    public double getSales() {
        return sales;
    }

    public void setSales(double sales) {
        this.sales = sales;
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
                + "in_stock, delivered, sales) "
                + "VALUES ('%d', '%d', '%d', '%d', '%f')",
                shipperId, productId, inStock, delivered, sales);
    }
    
    public String updateSQL() {
        return String.format("UPDATE shipper_stocks SET in_stock='%d', delivered='%d', "
                + "sales='%f' WHERE id='%d'", inStock, delivered, sales, id);
    }

    @Override
    public String toString() {
        return product.getName() + " (" + product.getSupplier() + ")";
    }
}
