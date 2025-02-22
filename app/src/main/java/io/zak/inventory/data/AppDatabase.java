package io.zak.inventory.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.zak.inventory.data.dao.BrandDao;
import io.zak.inventory.data.dao.CategoryDao;
import io.zak.inventory.data.dao.EmployeeDao;
import io.zak.inventory.data.dao.UserDao;
import io.zak.inventory.data.dao.VehicleDao;
import io.zak.inventory.data.dao.WarehouseDao;
import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.User;
import io.zak.inventory.data.entities.Vehicle;
import io.zak.inventory.data.entities.Warehouse;

@Database(entities = {
        User.class,
        Employee.class,
        Brand.class,
        Category.class,
        Warehouse.class,
        Vehicle.class
}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao users();

    public abstract EmployeeDao employees();

    public abstract BrandDao brands();

    public abstract CategoryDao categories();

    public abstract WarehouseDao warehouses();

    public abstract VehicleDao vehicles();

}
