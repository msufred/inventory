package io.zak.inventory.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.zak.inventory.data.dao.UserDao;
import io.zak.inventory.data.entities.User;

@Database(entities = {
        User.class
}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();

}
