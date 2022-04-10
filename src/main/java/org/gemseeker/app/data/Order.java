package org.gemseeker.app.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class Order implements IEntry {

    private int id;
    private LocalDate date;
    private int shipperId;
    private double total;
    private double sales;
    
    private Shipper shipper;

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

    public int getShipperId() {
        return shipperId;
    }

    public void setShipperId(int shipperId) {
        this.shipperId = shipperId;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getSales() {
        return sales;
    }

    public void setSales(double sales) {
        this.sales = sales;
    }

    public Shipper getShipper() {
        return shipper;
    }

    public void setShipper(Shipper shipper) {
        this.shipper = shipper;
    }
    
    @Override
    public String insertSQL() {
        return String.format("INSERT INTO orders ("
                + "date, shipper_id, total, sales) VALUES ('%s', '%d', '%f', '%f')",
                date, shipperId, total, sales);
    }

    @Override
    public String toString() {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        return String.format("%s - %s", dateFormat.format(date), shipper.getName());
    }
}
