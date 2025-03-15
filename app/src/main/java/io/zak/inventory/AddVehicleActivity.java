package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Vehicle;
import io.zak.inventory.firebase.VehicleEntry;

public class AddVehicleActivity extends AppCompatActivity {

    private static final String TAG = "AddVehicle";

    // Widgets
    private EditText etName, etPlateNo;
    private Spinner typeSpinner, statusSpinner;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private AlertDialog.Builder dialogBuilder;

    private CompositeDisposable disposables;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);
        getWidgets();
        setListeners();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etPlateNo = findViewById(R.id.et_plate_no);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        // Types
        typeSpinner = findViewById(R.id.type_spinner);
        ArrayAdapter<CharSequence> typeSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_types, android.R.layout.simple_spinner_item);
        typeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeSpinnerAdapter);

        // Status
        statusSpinner = findViewById(R.id.status_spinner);
        ArrayAdapter<CharSequence> statusSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_status, android.R.layout.simple_spinner_item);
        statusSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusSpinnerAdapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
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
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private boolean validated() {
        boolean isValid = true;

        if (etName.getText().toString().trim().isEmpty()) {
            etName.setError("Required");
            isValid = false;
        }else if (!etName.getText().toString().matches("^[^0-9]+$")) {
            etName.setError("Invalid Name");
            isValid = false;
        }
        if (etPlateNo.getText().toString().trim().isEmpty()) {
            etPlateNo.setError("Required");
            isValid = false;
        }
        return isValid;
    }

    private void saveAndClose() {
        Vehicle vehicle = new Vehicle();
        vehicle.vehicleName = Utils.normalize(etName.getText().toString());
        vehicle.vehicleType = typeSpinner.getSelectedItem().toString();
        vehicle.plateNo = Utils.normalize(etPlateNo.getText().toString());
        vehicle.vehicleStatus = statusSpinner.getSelectedItem().toString();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Vehicle entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().insert(vehicle);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Done. Returned with ID: " + id + " " + Thread.currentThread());

            // save online data
            VehicleEntry entry = new VehicleEntry(
                    id.intValue(),
                    vehicle.vehicleName,
                    vehicle.vehicleType,
                    vehicle.plateNo,
                    vehicle.vehicleStatus
            );
            mDatabase.child("vehicles").child(String.valueOf(id.intValue())).setValue(entry);

            progressGroup.setVisibility(View.GONE);
            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
            dialogBuilder.setTitle("Database Error").setMessage("Failed to save Vehicle entry: " + err);
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
