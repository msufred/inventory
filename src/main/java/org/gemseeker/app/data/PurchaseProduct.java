package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class PurchaseProduct implements IEntry {
    
    private int id;
    private int productId;
    private String invoiceId; // purchase invoice
    
    private Stock stock;
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

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO purchase_products (product_id, invoice_id) VALUES ('%d', '%s')",
                productId, invoiceId);
    }

}
