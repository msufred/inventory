package io.zak.inventory.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.zak.inventory.data.dao.UserDao;
import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.User;

@Database(entities = {
        User.class,
        Employee.class,
        Brand.class,
        Category.class
}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();

}
