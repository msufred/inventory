package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Comparator;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.EmployeeListAdapter;
import io.zak.inventory.adapters.WarehouseListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Warehouse;

public class EditEmployeeActivity extends AppCompatActivity {

    private static final String TAG = "EditEmployee";

    // Widgets
    private TextView tvTitle;
    private EditText etName, etPosition, etContact, etAddress, etLicense;
    private Spinner statusSpinner;
    private ImageButton btnBack, btnDelete;
    private Button btnCancel, btnSave;


    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Employee mEmployee; // to edit

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);
        getWidgets();
        setListeners();
        setupSpinner();
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void getWidgets() {
        tvTitle = findViewById(R.id.title);
        tvTitle.setText(R.string.edit_employee);
        etName = findViewById(R.id.et_name);
        etPosition = findViewById(R.id.et_position);
        etContact = findViewById(R.id.et_contact);
        etAddress = findViewById(R.id.et_address);
        etLicense = findViewById(R.id.et_license);
        statusSpinner = findViewById(R.id.status_spinner);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE);


    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });

        btnDelete.setOnClickListener(v -> {
            if (mEmployee != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("Are you sure you want to delete this Employee entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteEmployee();
                        });
                dialogBuilder.create().show();
            }
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check id from Intent
        int id = getIntent().getIntExtra("employee_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Employee ID. Try again.")
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }

        // fetch Employee entry
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Employee entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().getEmployee(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size() + " " + Thread.currentThread());
            mEmployee = list.get(0);
            displayInfo(mEmployee);
        }, err -> {
            Log.d(TAG, "Database Error: " + err);
        }));
    }

    private void displayInfo(Employee employee) {
        etName.setText(employee.name);
        etPosition.setText(employee.position);
        etContact.setText(employee.contactNo);
        etAddress.setText(employee.address);
        etLicense.setText(employee.licenseNo);
        statusSpinner.setSelection(getIndex(statusSpinner, employee.status));
    }

    private int getIndex(Spinner spinner, String myString) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)) {
                return i;
            }
        }
        return 0;
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private boolean validated() {
        return !etName.getText().toString().isBlank() && !etPosition.getText().toString().isBlank();
    }

    private void saveAndClose() {
        mEmployee.name = Utils.normalize(etName.getText().toString());
        mEmployee.position = Utils.normalize(etPosition.getText().toString());
        mEmployee.contactNo = Utils.normalize(etContact.getText().toString());
        mEmployee.address = Utils.normalize(etAddress.getText().toString());
        mEmployee.licenseNo = Utils.normalize(etLicense.getText().toString());
        mEmployee.status = statusSpinner.getSelectedItem().toString();

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating Employee entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().update(mEmployee);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Done. Returned with ID " + id + ": " + Thread.currentThread());
            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);

            // show dialog
            dialogBuilder.setTitle("Database Error").setMessage(err.toString());
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        }));
    }

    private void deleteEmployee() {
        disposables.add(Completable.fromAction(() -> {
                    Log.d(TAG, "Deleting Employee entry: " + Thread.currentThread());
                    AppDatabaseImpl.getDatabase(getApplicationContext()).employees().delete(mEmployee);
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {
                    Log.d(TAG, "Employee deleted successfully: " + Thread.currentThread());
                    Toast.makeText(this, "Employee Entry Deleted", Toast.LENGTH_SHORT).show();

                    // return to EmployeesActivity
                    startActivity(new Intent(this, EmployeesActivity.class));
                    finish();
                }, err -> {
                    Log.e(TAG, "Database Error: " + err);

                    // dialog
                    dialogBuilder.setTitle("Database Error")
                            .setMessage("Error while deleting Employee entry: " + err)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                    dialogBuilder.create().show();
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}

