package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "consumers")
public class Consumer {

    @PrimaryKey(autoGenerate = true)
    public int consumerId;
    public String consumerName;
    public String consumerAddress;
    public String consumerEmail;
    public String consumerContactNo;
}
