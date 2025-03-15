package io.zak.inventory;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.UserSpinnerAdapter;
import io.zak.inventory.adapters.VehicleSpinnerAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.firebase.AssignedVehicleEntry;
import io.zak.inventory.firebase.UserEntry;
import io.zak.inventory.firebase.VehicleEntry;

public class AddDeliveryOrderActivity extends AppCompatActivity {

    private static final String TAG = "AddDeliveryOrder";

    // Widgets
    private ImageButton btnBack;
    private EditText etTrackingNo;
    private Spinner vehicleSpinner, employeeSpinner;
    private TextView emptyVehicles, emptyEmployees;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    // Spinner reference lists
    private List<VehicleEntry> vehicleEntryList;
    private List<UserEntry> userEntryList;

    // reference for tracking no
    private List<DeliveryOrder> deliveryOrders;

    // selected items (Spinners)
    private VehicleEntry mVehicleEntry;
    private UserEntry mUserEntry;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private DatabaseReference mDatabase;
    private DatabaseReference mVehiclesRef;
    private DatabaseReference mUsersRef;

    private final ValueEventListener usersValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            fetchRegisteredUsers(snapshot);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled", error.toException());
        }
    };

    private final ValueEventListener vehiclesValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            fetchVehicles(snapshot);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled", error.toException());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_delivery_order);
        getWidgets();
        setListeners();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mUsersRef = mDatabase.child("users");
        mVehiclesRef = mDatabase.child("vehicles");
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

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        vehicleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (vehicleEntryList != null) mVehicleEntry = vehicleEntryList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });
        employeeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userEntryList != null) mUserEntry = userEntryList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (mVehicleEntry == null || mUserEntry == null) {
                showInfoDialog("Invalid Action", "No selected Vehicle and/or Employee.");
                return;
            }

            if (mVehicleEntry.status.equalsIgnoreCase("On Delivery")) {
                showInfoDialog("Invalid Action", "Vehicle status is \"On Delivery\". Select another vehicle and try again.");
                return;
            }

            if (validated()) saveAndClose();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetch Deliver Order entries");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
            Log.d(TAG, "Returned with list size=" + orders.size());
            deliveryOrders  = orders;

            // get vehicles online
            mVehiclesRef.addValueEventListener(vehiclesValueEventListener);
            mUsersRef.addValueEventListener(usersValueEventListener);
        }));
    }

    private void fetchVehicles(DataSnapshot dataSnapshot) {
        progressGroup.setVisibility(View.VISIBLE);
        Log.d(TAG, "fetch vehicles online");
        vehicleEntryList = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            VehicleEntry entry = snapshot.getValue(VehicleEntry.class);
            if (entry != null) {
                vehicleEntryList.add(entry);
            }
        }
        vehicleSpinner.setAdapter(new VehicleSpinnerAdapter(this, vehicleEntryList));
        emptyVehicles.setVisibility(vehicleEntryList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        mVehiclesRef.removeEventListener(vehiclesValueEventListener);
        progressGroup.setVisibility(View.GONE);
    }

    private void fetchRegisteredUsers(DataSnapshot dataSnapshot) {
        progressGroup.setVisibility(View.VISIBLE);
        Log.d(TAG, "fetch users online");
        userEntryList = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            UserEntry entry = snapshot.getValue(UserEntry.class);
            if (entry != null) {
                entry.uid = snapshot.getKey();
                userEntryList.add(entry);
            }
        }
        employeeSpinner.setAdapter(new UserSpinnerAdapter(this, userEntryList));
        emptyEmployees.setVisibility(userEntryList.isEmpty() ? View.VISIBLE : View.GONE);
        mUsersRef.removeEventListener(usersValueEventListener);
        progressGroup.setVisibility(View.GONE);
    }

    private boolean validated() {

        String trackingNo = etTrackingNo.getText().toString();
        if (trackingNo.isBlank()) {
            etTrackingNo.setError("Required");
            return false;
        }else if (!trackingNo.matches("^\\d+$")) {
            etTrackingNo.setError("Invalid Tracking Number");
            return false;
        }

        // check if tracking no exists
        for (DeliveryOrder order : deliveryOrders) {
            if (order.trackingNo.equalsIgnoreCase(trackingNo)) {
                etTrackingNo.setError("Tracking Number already exists");
                return false;
            }
        }

        return true;
    }

    private void saveAndClose() {
        DeliveryOrder order = new DeliveryOrder();
        order.trackingNo = Utils.normalize(etTrackingNo.getText().toString());
        order.userId = mUserEntry.uid;
        order.userName = mUserEntry.fullName;
        order.fkVehicleId = mVehicleEntry.id;
        order.vehicleName = mVehicleEntry.name;
        order.vehiclePlateNo = mVehicleEntry.plateNo;
        order.deliveryDate = new Date().getTime();
        order.totalAmount = 0;
        order.deliveryOrderStatus = "Processing";

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving delivery order.");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().insert(order);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Returned with id=" + id);

            // assign vehicle for user
            AssignedVehicleEntry entry = new AssignedVehicleEntry(
                    mUserEntry.uid, order.fkVehicleId, order.vehicleName, order.vehiclePlateNo
            );
            mDatabase.child("assigned_vehicles").child(entry.userId).setValue(entry);

            progressGroup.setVisibility(View.GONE);
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

    private void showInfoDialog(String title, String message) {
        dialogBuilder.setTitle(title).setMessage(message).setPositiveButton("OK", (d, w) -> d.dismiss());
        dialogBuilder.create().show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
