package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "suppliers")
public class Supplier {

    @PrimaryKey(autoGenerate = true)
    public int supplierId;
    public String supplierName;
    public String supplierAddress;
    public String supplierEmail;
    public String supplierContactNo;
}
