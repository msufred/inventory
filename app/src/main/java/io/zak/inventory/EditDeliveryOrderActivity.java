package io.zak.inventory;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.EmployeeSpinnerAdapter;
import io.zak.inventory.adapters.VehicleSpinnerAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Vehicle;
import io.zak.inventory.data.entities.WarehouseStock;
import io.zak.inventory.data.relations.DeliveryItemDetails;

public class EditDeliveryOrderActivity extends AppCompatActivity {

    private static final String TAG = "EditDeliveryOrder";

    // Widgets
    private ImageButton btnBack, btnDelete;
    private EditText etTrackingNo;
    private Spinner vehicleSpinner, employeeSpinner;
    private TextView emptyVehicles, emptyEmployees;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private Drawable errorDrawable;

    // Spinner reference lists
    private List<Vehicle> vehicleList;
    private List<Employee> employeeList;

    // reference for tracking no
    private List<DeliveryOrder> deliveryOrders;

    private Vehicle mVehicle;   // selected vehicle
    private Employee mEmployee; // selected employee

    private DeliveryOrder mDeliveryOrder; // DeliveryOrder to edit/delete (fetched in onResume)
    private List<DeliveryItemDetails> deliveryItemList; // reference list if ever DeliveryOrder is to be deleted

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_delivery_order);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        TextView title = findViewById(R.id.title);
        title.setText(R.string.edit_delivery);

        btnBack = findViewById(R.id.btn_back);

        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE);

        etTrackingNo = findViewById(R.id.et_tracking_no);
        vehicleSpinner = findViewById(R.id.vehicle_spinner);
        employeeSpinner = findViewById(R.id.employee_spinner);
        emptyVehicles = findViewById(R.id.empty_vehicle_spinner);
        emptyEmployees = findViewById(R.id.empty_employees_spinner);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        errorDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_x_circle);
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        vehicleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (vehicleList != null) {
                    mVehicle = vehicleList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });
        employeeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (employeeList != null) {
                    mEmployee = employeeList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
        btnDelete.setOnClickListener(v -> {
            dialogBuilder.setTitle("Confirm Delete")
                    .setMessage("This will delete all data related to this Delivery Order. " +
                            "Are you sure you want to delete this entry?")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        dialog.dismiss();
                        deleteDeliveryOrder();
                    });
            dialogBuilder.create().show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check id
        int id = getIntent().getIntExtra("delivery_order_id", -1);
        if (id == -1) {
            // no passed id, return to HomeActivity
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Delivery Order ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return; // exit
        }

        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryOrder entry.");
            return database.deliveryOrders().getDeliveryOrder(id);
        }).flatMap(orders -> {
            Log.d(TAG, "Returned with list size=" + orders.size());
            mDeliveryOrder = orders.get(0);
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching delivery items.");
                return database.deliveryOrderItems().getDeliveryItemsWithDetails(id);
            });
        }).flatMap(deliveryItemDetails -> {
            Log.d(TAG, "Returned with list size=" + deliveryItemDetails.size());
            deliveryItemList = deliveryItemDetails;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching vehicle entries.");
                return database.vehicles().getAll();
            });
        }).flatMap(vehicles -> {
            Log.d(TAG, "Returned with list size=" + vehicles.size());
            vehicleList = vehicles;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching employee entries.");
                return database.employees().getAll();
            });
        }).flatMap(employees -> {
            Log.d(TAG, "Returned with list size=" + employees.size());
            employeeList = employees;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching delivery orders.");
                return database.deliveryOrders().getAll();
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + orders.size());
            deliveryOrders = orders;
            setupSpinnerAdapters();
            displayInfo();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
        }));
    }

    private void setupSpinnerAdapters() {
        vehicleSpinner.setAdapter(new VehicleSpinnerAdapter(this, vehicleList));
        emptyVehicles.setVisibility(vehicleList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        employeeSpinner.setAdapter(new EmployeeSpinnerAdapter(this, employeeList));
        emptyEmployees.setVisibility(employeeList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
    }

    private void displayInfo() {
        etTrackingNo.setText(mDeliveryOrder.trackingNo);
        int vehiclePosition;
        for (vehiclePosition = 0; vehiclePosition < vehicleList.size(); vehiclePosition++) {
            if (vehicleList.get(vehiclePosition).vehicleId == mDeliveryOrder.fkVehicleId) break;
        }
        vehicleSpinner.setSelection(vehiclePosition);

        int employeePosition;
        for (employeePosition = 0; employeePosition < employeeList.size(); employeePosition++) {
            if (employeeList.get(employeePosition).employeeId == mDeliveryOrder.fkEmployeeId) break;
        }
        employeeSpinner.setSelection(employeePosition);
    }

    private boolean validated() {
        // clear drawables
        etTrackingNo.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        String trackingNo = etTrackingNo.getText().toString();
        if (trackingNo.isBlank()) {
            etTrackingNo.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
            return false;
        }

        if (mVehicle == null || mEmployee == null) {
            return false;
        }

        // check if tracking no exists
        for (DeliveryOrder order : deliveryOrders) {
            if (order.deliveryOrderId == mDeliveryOrder.deliveryOrderId) continue;
            if (order.trackingNo.equalsIgnoreCase(trackingNo)) {
                etTrackingNo.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
                return false;
            }
        }

        return true;
    }

    private void saveAndClose() {
        mDeliveryOrder.trackingNo = Utils.normalize(etTrackingNo.getText().toString());
        mDeliveryOrder.fkVehicleId = mVehicle.vehicleId;
        mDeliveryOrder.fkEmployeeId = mEmployee.employeeId;
        mDeliveryOrder.deliveryDate = new Date().getTime();
        mDeliveryOrder.totalAmount = 0;
        mDeliveryOrder.deliveryOrderStatus = "Processing";

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().update(mDeliveryOrder);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            if (rowCount > 0) {
                Toast.makeText(this, "Delivery Order upadted.", Toast.LENGTH_SHORT).show();
            }
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            goBack();
        }));
    }

    private void deleteDeliveryOrder() {
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        progressGroup.setVisibility(View.VISIBLE);

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting delivery order.");
            return database.deliveryOrders().delete(mDeliveryOrder);
        }).flatMap(rowCount -> {
            Log.d(TAG, "Returned with row count=" + rowCount);
            if (rowCount > 0) {
                Log.d(TAG, "Updating warehouse stocks");
                for (DeliveryItemDetails details : deliveryItemList) {
                    WarehouseStock stock = database.warehouseStocks().getWarehouseStock(details.deliveryOrderItem.fkWarehouseStockId).get(0);
                    stock.takenOut = stock.takenOut - details.deliveryOrderItem.quantity;
                    database.warehouseStocks().update(stock);
                }
            }
            return Single.just(rowCount);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            if (rowCount > 0) {
                Toast.makeText(this, "Delivery Order deleted.", Toast.LENGTH_SHORT).show();
            }

            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            goBack();
        }));
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
