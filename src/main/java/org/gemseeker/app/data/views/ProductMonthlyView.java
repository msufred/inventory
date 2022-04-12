package org.gemseeker.app.data.views;

import org.gemseeker.app.data.Product;
import org.gemseeker.app.data.Stock;

/**
 *
 * @author Gem
 */
public class ProductMonthlyView {

    private Product product;
    private Stock stock;
    private int purchased;          // quantity purchased this month
    private int ordered;            // quantity ordered this month
    private double purchasedTotal;  // purchase total amount
    private double orderedTotal;    // ordered total amount

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public int getPurchased() {
        return purchased;
    }

    public void setPurchased(int purchased) {
        this.purchased = purchased;
    }

    public int getOrdered() {
        return ordered;
    }

    public void setOrdered(int ordered) {
        this.ordered = ordered;
    }

    public double getPurchasedTotal() {
        return purchasedTotal;
    }

    public void setPurchasedTotal(double purchasedTotal) {
        this.purchasedTotal = purchasedTotal;
    }

    public double getOrderedTotal() {
        return orderedTotal;
    }

    public void setOrderedTotal(double orderedTotal) {
        this.orderedTotal = orderedTotal;
    }
    
}
