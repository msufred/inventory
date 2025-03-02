package io.zak.inventory.data.relations;

import androidx.room.Embedded;

import io.zak.inventory.data.entities.Consumer;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Order;
import io.zak.inventory.data.entities.Vehicle;

public class OrderDetails {

    @Embedded
    public Order order;

    @Embedded
    public Vehicle vehicle;

    @Embedded
    public Employee employee;

}
