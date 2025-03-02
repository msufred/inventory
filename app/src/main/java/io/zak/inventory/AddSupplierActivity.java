package io.zak.inventory;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Supplier;

public class AddSupplierActivity extends AppCompatActivity {

    private static final String TAG = "AddSupplier";

    // Widgets
    private EditText etName, etContact, etEmail, etAddress;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_supplier);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etContact = findViewById(R.id.et_contact);
        etEmail = findViewById(R.id.et_email);
        etAddress = findViewById(R.id.et_address);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);


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
        if (etContact.getText().toString().trim().isEmpty()) {
            etContact.setError("Required");
            isValid = false;
        }
        if (etAddress.getText().toString().trim().isEmpty()) {
            etAddress.setError("Required");
            isValid = false;
        }
        return isValid;
    }

    private void saveAndClose() {
        Supplier supplier = new Supplier();
        supplier.supplierName = Utils.normalize(etName.getText().toString());
        supplier.supplierContactNo = Utils.normalize(etContact.getText().toString());
        supplier.supplierEmail = Utils.normalize(etEmail.getText().toString());
        supplier.supplierAddress = Utils.normalize(etAddress.getText().toString());

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Supplier entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).suppliers().insert(supplier);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Returned with ID: " + id + " " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
            dialogBuilder.setTitle("Database Error").setMessage("Error while saving Supplier entry: " + err);
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
