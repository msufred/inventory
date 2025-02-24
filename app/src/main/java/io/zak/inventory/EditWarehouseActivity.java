package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Warehouse;

public class EditWarehouseActivity extends AppCompatActivity {

    private static final String TAG = "AddWarehouse";

    // Widgets
    private EditText etName, etContact, etAddress;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Warehouse mWarehouse; // to edit

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_warehouse);
        getWidgets();
        setListeners();
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etContact = findViewById(R.id.et_contact);
        etAddress = findViewById(R.id.et_address);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
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

        // check id from Intent
        int id = getIntent().getIntExtra("warehouse_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Warehouse ID. Try again.")
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }

        // fetch Warehouse entry
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Warehouse entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouses().getWarehouse(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size() + " " + Thread.currentThread());
            mWarehouse = list.get(0);
            displayInfo(mWarehouse);
        }, err -> {
            Log.d(TAG, "Database Error: " + err);
        }));
    }

    private void displayInfo(Warehouse warehouse) {
        etName.setText(warehouse.name);
        etAddress.setText(warehouse.address);
        etContact.setText(warehouse.contactNo);
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private boolean validated() {
        return !etName.getText().toString().isBlank();
    }

    private void saveAndClose() {
        Warehouse warehouse = new Warehouse();
        warehouse.name = Utils.normalize(etName.getText().toString());
        warehouse.address = Utils.normalize(etAddress.getText().toString());
        warehouse.contactNo = Utils.normalize(etContact.getText().toString());

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Warehouse entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouses().insert(warehouse);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
