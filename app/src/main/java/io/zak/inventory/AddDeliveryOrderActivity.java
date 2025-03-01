package io.zak.inventory;

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

import androidx.annotation.Nullable;
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

public class AddDeliveryOrderActivity extends AppCompatActivity {

    private static final String TAG = "AddDeliveryOrder";

    // Widgets
    private ImageButton btnBack;
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

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_delivery_order);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnBack = findViewById(R.id.btn_back);
        etTrackingNo = findViewById(R.id.et_tracking_no);
        vehicleSpinner = findViewById(R.id.vehicle_spinner);
        employeeSpinner = findViewById(R.id.employee_spinner);
        emptyVehicles = findViewById(R.id.empty_vehicle_spinner);
        emptyEmployees = findViewById(R.id.empty_employees_spinner);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        errorDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_x_circle);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching vehicle entries.");
            return database.vehicles().getAll();
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
            if (order.trackingNo.equalsIgnoreCase(trackingNo)) {
                etTrackingNo.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
                return false;
            }
        }

        return true;
    }

    private void saveAndClose() {
        DeliveryOrder order = new DeliveryOrder();
        order.trackingNo = Utils.normalize(etTrackingNo.getText().toString());
        order.fkVehicleId = mVehicle.vehicleId;
        order.fkEmployeeId = mEmployee.employeeId;
        order.deliveryDate = new Date().getTime();
        order.totalAmount = 0;
        order.deliveryOrderStatus = "Processing";

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving delivery order.");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().insert(order);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with id=" + id);
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
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
