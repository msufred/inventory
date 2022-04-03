package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class InvoiceItem implements IEntry {

    private int id;
    private int invoiceId;
    private int productId;
    private int quantity;
    private double discount;
    private double listPrice;

    private Product product;
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
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
                + "(invoice_id, product_id, quantity, discount, list_price) "
                + "VALUES ('%d', '%d', '%d', '%f', '%f')",
                invoiceId, productId, quantity, discount, listPrice);
    }

}
