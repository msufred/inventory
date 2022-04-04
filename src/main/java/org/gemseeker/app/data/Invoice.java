package org.gemseeker.app.data;

import java.time.LocalDate;
import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class Invoice implements IEntry {

    private String id;
    private int orderId;
    private LocalDate date;
    private String customer;
    private String address;
    private double total;
    private String paymentType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }
    
    @Override
    public String insertSQL() {
        return String.format("INSERT INTO invoices "
                + "(id, order_id, date, customer, address, total, payment_type) "
                + "VALUES ('%s', '%d', '%s', '%s', '%s', '%f', '%s')",
                id, orderId, date, customer, address, total, paymentType);
    }

}
