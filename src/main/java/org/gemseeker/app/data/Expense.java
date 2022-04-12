package org.gemseeker.app.data;

import java.time.LocalDate;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author RAFIS-DIMAISIP
 */
public class Expense implements IEntry {
    
    private int id;
    private LocalDate date;
    private String category;
    private String remarks;
    private String ref;
    private double amount;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO expenses (date, category, remarks, ref, amount) "
                + "VALUES ('%s', '%s', '%s', '%s', '%f')",
                date, category, remarks, ref, amount);
    }
    
}
