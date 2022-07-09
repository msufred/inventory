package org.gemseeker.app.data;

import java.time.LocalDate;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class PurchaseInvoice implements IEntry {
    
    private String id;
    private LocalDate date;
    private String supplier;
    private double total;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO purchase_invoices (id, date, supplier, total) "
                + "VALUES ('%s', '%s', '%s', '%f')",
                id, date, supplier, total);
    }
    
    public String updateSQL() {
        return String.format("UPDATE purchase_invoices SET date='%s', supplier='%s', total='%f' WHERE id='%s'",
                date, supplier, total, id);
    }

}
