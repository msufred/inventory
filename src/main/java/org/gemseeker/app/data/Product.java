package org.gemseeker.app.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class Product implements IEntry {

    private int id;
    private LocalDate date;
    private String name;
    private String sku;
    private String supplier;
    private String unit;
    private double unitPrice;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO products ("
                + "date, name, sku, supplier, unit, unit_price) "
                + "VALUES ('%s', '%s', '%s', '%s', '%s', '%f')",
                date, name, sku, supplier, unit, unitPrice);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
