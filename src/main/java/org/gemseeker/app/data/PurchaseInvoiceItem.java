package org.gemseeker.app.data;

import java.time.LocalDate;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class PurchaseInvoiceItem implements IEntry {
    
    private int id = -1;
    private LocalDate date;
    private String invoiceId; // purchase invoice
    private int productId = -1;
    private double unitPrice;
    private int quantity;
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

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
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
        return String.format("INSERT INTO purchase_invoice_items "
                + "(date, invoice_id, product_id, unit_price, quantity, total) "
                + "VALUES ('%s', '%s', '%d', '%f', '%d', '%f')",
                date, invoiceId, productId, unitPrice, quantity, total);
    }

    public String updateSQL() {
        return String.format("UPDATE purchase_invoice_items SET "
                + "date='%s', unit_price='%f', quantity='%d', total='%f' "
                + "WHERE id='%d'", date, unitPrice, quantity, total, id);
    }
}
