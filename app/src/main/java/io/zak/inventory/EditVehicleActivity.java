package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Vehicle;

public class EditVehicleActivity extends AppCompatActivity {

    private static final String TAG = "EditVehicle";

    private EditText etName, etPlateno;
    private Spinner typeSpinner, statusSpinner;
    private ImageButton btnBack, btnDelete;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Vehicle mVehicle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);
        getWidgets();
        setupSpinners();
        setListeners();
    }

    private void getWidgets() {
        TextView tvTitle = findViewById(R.id.title);
        tvTitle.setText(R.string.edit_vehicle);
        etName = findViewById(R.id.et_name);
        etPlateno = findViewById(R.id.et_plate_no);
        typeSpinner = findViewById(R.id.type_spinner);
        statusSpinner = findViewById(R.id.status_spinner);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.vehicle_status, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
        btnDelete.setOnClickListener(v -> {
            if (mVehicle != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will delete all data related to this Vehicle entry. " +
                                "Are you sure you want to delete this entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteVehicle();
                        });
                dialogBuilder.create().show();
            }
        });
    }

    private void deleteVehicle() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() ->
                        AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().delete(mVehicle))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(result -> {
                    progressGroup.setVisibility(View.GONE);
                    Toast.makeText(this, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show();
                    setResultAndFinish();
                }, err -> {
                    progressGroup.setVisibility(View.GONE);
                    dialogBuilder.setTitle("Database Error")
                            .setMessage("Error while deleting Vehicle entry: " + err)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                    dialogBuilder.create().show();
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = getIntent().getIntExtra("vehicleId", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Vehicle ID: " + id)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() ->
                        AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().getVehicleById(id))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(vehicle -> {
                    progressGroup.setVisibility(View.GONE);
                    mVehicle = (Vehicle) vehicle;
                    displayInfo(mVehicle);
                }, err -> {
                    progressGroup.setVisibility(View.GONE);
                    dialogBuilder.setTitle("Database Error")
                            .setMessage("Error while fetching Vehicle entry: " + err)
                            .setPositiveButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                                goBack();
                            });
                    dialogBuilder.create().show();
                }));
    }

    private void displayInfo(Vehicle vehicle) {
        if (vehicle != null) {
            etName.setText(vehicle.vehicleName);
            etPlateno.setText(vehicle.plateNo);
            typeSpinner.setSelection(getIndex(typeSpinner, vehicle.vehicleType));
            statusSpinner.setSelection(getIndex(statusSpinner, vehicle.vehicleStatus));
        }
    }

    private int getIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(value)) {
                return i;
            }
        }
        return 0;
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
        if (etPlateno.getText().toString().trim().isEmpty()) {
            etPlateno.setError("Required");
            isValid = false;
        }
        return isValid;
    }

    private void saveAndClose() {
        mVehicle.vehicleName = etName.getText().toString().trim();
        mVehicle.plateNo = etPlateno.getText().toString().trim();
        mVehicle.vehicleType = typeSpinner.getSelectedItem().toString();
        mVehicle.vehicleStatus = statusSpinner.getSelectedItem().toString();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() ->
                        AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().update(mVehicle))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(id -> {
                    progressGroup.setVisibility(View.GONE);
                    Toast.makeText(this, "Vehicle updated successfully", Toast.LENGTH_SHORT).show();
                    setResultAndFinish();
                }, err -> {
                    progressGroup.setVisibility(View.GONE);
                    dialogBuilder.setTitle("Database Error")
                            .setMessage("Error while updating Vehicle entry: " + err)
                            .setPositiveButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                            });
                    dialogBuilder.create().show();
                }));
    }

    private void setResultAndFinish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("vehicleId", mVehicle.vehicleId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void goBack() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposables != null) {
            disposables.dispose();
        }
    }
}