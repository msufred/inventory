package io.zak.inventory.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.zak.inventory.data.dao.AllowanceDao;
import io.zak.inventory.data.dao.BrandDao;
import io.zak.inventory.data.dao.CategoryDao;
import io.zak.inventory.data.dao.ConsumerDao;
import io.zak.inventory.data.dao.DamageStockDao;
import io.zak.inventory.data.dao.DeliveryOrderDao;
import io.zak.inventory.data.dao.DeliveryOrderItemDao;
import io.zak.inventory.data.dao.EmployeeDao;
import io.zak.inventory.data.dao.OrderDao;
import io.zak.inventory.data.dao.OrderItemDao;
import io.zak.inventory.data.dao.ProductDao;
import io.zak.inventory.data.dao.ReturnedStockDao;
import io.zak.inventory.data.dao.SupplierDao;
import io.zak.inventory.data.dao.UserDao;
import io.zak.inventory.data.dao.VehicleDao;
import io.zak.inventory.data.dao.VehicleStockDao;
import io.zak.inventory.data.dao.WarehouseDao;
import io.zak.inventory.data.dao.WarehouseStockDao;
import io.zak.inventory.data.entities.Allowance;
import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.data.entities.Consumer;
import io.zak.inventory.data.entities.DamageStock;
import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.data.entities.DeliveryOrderItem;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Order;
import io.zak.inventory.data.entities.OrderItem;
import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.entities.ReturnedStock;
import io.zak.inventory.data.entities.Supplier;
import io.zak.inventory.data.entities.User;
import io.zak.inventory.data.entities.Vehicle;
import io.zak.inventory.data.entities.VehicleStock;
import io.zak.inventory.data.entities.Warehouse;
import io.zak.inventory.data.entities.WarehouseStock;

@Database(entities = {
        User.class,
        Employee.class,
        Brand.class,
        Category.class,
        Warehouse.class,
        Vehicle.class,
        Supplier.class,
        Consumer.class,
        Allowance.class,
        Product.class,
        WarehouseStock.class,
        DeliveryOrder.class,
        DeliveryOrderItem.class,
        VehicleStock.class,
        Order.class,
        OrderItem.class,
        ReturnedStock.class,
        DamageStock.class
}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao users();
    public abstract EmployeeDao employees();
    public abstract BrandDao brands();
    public abstract CategoryDao categories();
    public abstract WarehouseDao warehouses();
    public abstract VehicleDao vehicles();
    public abstract SupplierDao suppliers();
    public abstract ConsumerDao consumers();
    public abstract AllowanceDao allowances();
    public abstract ProductDao products();
    public abstract WarehouseStockDao warehouseStocks();
    public abstract VehicleStockDao vehicleStocks();
    public abstract DeliveryOrderDao deliveryOrders();
    public abstract DeliveryOrderItemDao deliveryOrderItems();
    public abstract OrderDao orders();
    public abstract OrderItemDao orderItems();
    public abstract ReturnedStockDao returnedStocks();
    public abstract DamageStockDao damagedStocks();
}
