package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class InvoiceItem implements IEntry {

    private int id;
    private String invoiceId;
    private int productId;
    private int quantity;
    private double discount;
    private double discountedPrice;
    private double listPrice;

    private Product product;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
    
    @Override
    public String insertSQL() {
        return String.format("INSERT INTO invoice_items "
                + "(invoice_id, product_id, quantity, discount, discounted_price, list_price) "
                + "VALUES ('%s', '%d', '%d', '%f', '%f', '%f')",
                invoiceId, productId, quantity, discount, discountedPrice, listPrice);
    }

}
