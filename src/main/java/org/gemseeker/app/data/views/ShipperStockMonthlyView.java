package org.gemseeker.app.data.views;

import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.ShipperStock;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class ShipperStockMonthlyView {
    
    private Product product;
    private ShipperStock stock;
    private int ordered;
    private int delivered;
    private double sales;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public ShipperStock getStock() {
        return stock;
    }

    public void setStock(ShipperStock stock) {
        this.stock = stock;
    }

    public int getOrdered() {
        return ordered;
    }

    public void setOrdered(int ordered) {
        this.ordered = ordered;
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
    
    
}
