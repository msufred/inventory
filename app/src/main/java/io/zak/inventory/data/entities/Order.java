package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Order represents a single order transaction. Order holds data for the vehicle and employee that
 * handles the delivery, the details of the consumer that ordered the products, the date or order,
 * total amount of order transaction. The "status" field only represents the processing status of the current
 * order in the application. "Processing" status means the order is not yet complete and some items
 * are not yet added to the list of order. "Completed" status means the order is complete. All order
 * items are added and checked.
 */
@Entity(tableName = "orders", foreignKeys = {
        @ForeignKey(entity = Vehicle.class, parentColumns = "vehicleId", childColumns = "fkVehicleId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Employee.class, parentColumns = "employeeId", childColumns = "fkEmployeeId")
})
public class Order {

    @PrimaryKey
    public int orderId;             // id is based on the Order ID entry in the Delivery App of the driver
    public String orNo;             // official receipt no; REQUIRED
    public int fkVehicleId;         // from assigned vehicle
    public int fkEmployeeId;        // by assigned employee/driver

    public String consumerName;
    public String consumerAddress;
    public String consumerContact;

    public long dateOrdered;        // Date converted to long value (use getTime() of Date)
    public double totalAmount;      // total amount of the order
    public String orderStatus;      // i.e. Processing, Completed
}
