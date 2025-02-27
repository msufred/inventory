package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "allowances", foreignKeys = {
        @ForeignKey(entity = Vehicle.class, parentColumns = "vehicleId", childColumns = "fkVehicleId", onDelete = ForeignKey.CASCADE)
})
public class Allowance {

    @PrimaryKey(autoGenerate = true)
    public int allowanceId;
    public int fkVehicleId;
    public String allowanceType;
    public double allowanceAmount;
    public long date;
}
