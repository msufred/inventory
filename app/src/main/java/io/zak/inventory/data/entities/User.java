package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;

    public String password;

    public String fullName;

    public String position;

    public String address;

    public String contactNo;
}
