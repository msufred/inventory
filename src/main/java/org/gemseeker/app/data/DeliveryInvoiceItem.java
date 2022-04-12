package org.gemseeker.app.data;

import java.time.LocalDate;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class DeliveryInvoiceItem implements IEntry {

    private int id;
    private LocalDate date;
    private String invoiceId;
    private int productId;
    private int quantity;
    private double discount;
    private double discountedPrice;
    private double total;

    private Product product;
    
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

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
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

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(double discountedPrice) {
        this.discountedPrice = discountedPrice;
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
    
    @Override
    public String insertSQL() {
        return String.format("INSERT INTO delivery_invoice_items "
                + "(date, invoice_id, product_id, quantity, discount, discounted_price, total) "
                + "VALUES ('%s', '%s', '%d', '%d', '%f', '%f', '%f')",
                date, invoiceId, productId, quantity, discount, discountedPrice, total);
    }

}
