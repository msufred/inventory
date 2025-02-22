package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "consumers")
public class Consumer {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String address;
    public String email;
    public String contactNo;
}
