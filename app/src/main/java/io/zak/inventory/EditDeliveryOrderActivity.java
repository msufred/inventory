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
import android.widget.Toast;

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
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.firebase.AssignedVehicleEntry;
import io.zak.inventory.firebase.UserEntry;
import io.zak.inventory.firebase.VehicleEntry;

public class EditDeliveryOrderActivity extends AppCompatActivity {

    private static final String TAG = "EditDeliveryOrder";

    // Widgets
    private ImageButton btnBack, btnDelete;
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

    private VehicleEntry mVehicleEntry;
    private UserEntry mUserEntry;
    private DeliveryOrder mDeliveryOrder; // DeliveryOrder to edit/delete (fetched in onResume)

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
        mVehiclesRef = mDatabase.child("vehicles");
        mUsersRef = mDatabase.child("users");
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
            if (validated()) saveAndClose();
        });

        btnDelete.setOnClickListener(v -> {
            if (mDeliveryOrder != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will delete all data related to this Delivery Order. " +
                                "Are you sure you want to delete this entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteDeliveryOrder();
                        });
                dialogBuilder.create().show();
            }
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
                Log.d(TAG, "Fetching delivery orders.");
                return database.deliveryOrders().getAll();
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
            Log.d(TAG, "Returned with list size=" + orders.size());

            // fetch vehicles & users online
            mVehiclesRef.addValueEventListener(vehiclesValueEventListener);
            mUsersRef.addValueEventListener(usersValueEventListener);

            deliveryOrders = orders;
            progressGroup.setVisibility(View.GONE);
            displayInfo();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
        }));
    }

    private void fetchVehicles(DataSnapshot dataSnapshot) {
        Log.d(TAG, "fetch vehicles online");
        progressGroup.setVisibility(View.VISIBLE);
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
    }

    private void fetchRegisteredUsers(DataSnapshot dataSnapshot) {
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
    }


    private void displayInfo() {
        etTrackingNo.setText(mDeliveryOrder.trackingNo);
        int vehiclePosition;
        for (vehiclePosition = 0; vehiclePosition < vehicleEntryList.size(); vehiclePosition++) {
            if (vehicleEntryList.get(vehiclePosition).id == mDeliveryOrder.fkVehicleId) break;
        }
        vehicleSpinner.setSelection(vehiclePosition);

        int userPosition = 0;
        for (int i = 0; i < userEntryList.size(); i++) {
            if (userEntryList.get(i).uid.equals(mDeliveryOrder.userId)) {
                userPosition = i;
                break;
            }
        }
        employeeSpinner.setSelection(userPosition);
    }

    private boolean validated() {
        String trackingNo = etTrackingNo.getText().toString();
        if (trackingNo.isBlank()) {
            etTrackingNo.setError("Required");
            return false;
        }

        if (mVehicleEntry == null || mUsersRef == null) {
            return false;
        }

        // check if tracking no. exists
        for (DeliveryOrder order : deliveryOrders) {
            if (order.deliveryOrderId == mDeliveryOrder.deliveryOrderId) continue;
            if (order.trackingNo.equalsIgnoreCase(trackingNo)) {
                etTrackingNo.setError("Already exists!");
                return false;
            }
        }

        return true;
    }

    private void saveAndClose() {
        mDeliveryOrder.trackingNo = Utils.normalize(etTrackingNo.getText().toString());
        mDeliveryOrder.userId = mUserEntry.uid;
        mDeliveryOrder.userName = mUserEntry.fullName;
        mDeliveryOrder.fkVehicleId = mVehicleEntry.id;
        mDeliveryOrder.vehicleName = mVehicleEntry.name;
        mDeliveryOrder.vehiclePlateNo = mVehicleEntry.plateNo;
        mDeliveryOrder.deliveryDate = new Date().getTime();
        mDeliveryOrder.totalAmount = 0;
        mDeliveryOrder.deliveryOrderStatus = "Processing";

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().update(mDeliveryOrder);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            if (rowCount > 0) {
                Toast.makeText(this, "Delivery Order updated.", Toast.LENGTH_SHORT).show();
            }

            // update assigned vehicle online
            AssignedVehicleEntry entry = new AssignedVehicleEntry(
                    mUserEntry.uid, mDeliveryOrder.fkVehicleId, mDeliveryOrder.vehicleName, mDeliveryOrder.vehiclePlateNo
            );
            mDatabase.child("assigned_vehicles").child(entry.userId).setValue(entry);

            progressGroup.setVisibility(View.GONE);
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            goBack();
        }));
    }

    private void deleteDeliveryOrder() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting delivery order entry.");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().delete(mDeliveryOrder);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rows -> {
            Log.d(TAG, "Returned with row count=" + rows);
            // update assigned vehicle online
            mDatabase.child("assigned_vehicles").child(mDeliveryOrder.userId).setValue(null);
            progressGroup.setVisibility(View.GONE);
            goBack();
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            progressGroup.setVisibility(View.GONE);
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
