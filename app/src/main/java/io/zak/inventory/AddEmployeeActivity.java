package io.zak.inventory;

import android.graphics.drawable.Drawable;
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
import androidx.appcompat.content.res.AppCompatResources;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Employee;

public class AddEmployeeActivity extends AppCompatActivity {

    private static final String TAG = "AddEmployee";

    // Widgets
    private EditText etName, etPosition, etContact, etAddress, etLicense;
    private Spinner statusSpinner;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private Drawable errorDrawable;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etPosition = findViewById(R.id.et_position);
        etContact = findViewById(R.id.et_contact);
        etAddress = findViewById(R.id.et_address);
        etLicense = findViewById(R.id.et_license);
        statusSpinner = findViewById(R.id.status_spinner);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        errorDrawable =AppCompatResources.getDrawable(this, R.drawable.ic_x_circle);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) {
                saveAndClose();
            }
        });
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private boolean validated() {
        // remove all drawable in EditText
        etName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        etPosition.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        // check required fields
        if (etName.getText().toString().isBlank()) {
            etName.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        }
        if (etPosition.getText().toString().isBlank()) {
            etPosition.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        }

        return !etName.getText().toString().isBlank() && !etPosition.getText().toString().isBlank();
    }

    private void saveAndClose() {
        Employee employee = new Employee();
        employee.name = Utils.normalize(etName.getText().toString());
        employee.position = Utils.normalize(etPosition.getText().toString());
        employee.contactNo = Utils.normalize(etContact.getText().toString());
        employee.address = Utils.normalize(etAddress.getText().toString());
        employee.status = statusSpinner.getSelectedItem().toString();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Employee entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().insert(employee);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Done. Returned with ID " + id + ": " + Thread.currentThread());
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // show dialog
            dialogBuilder.setTitle("Database Error").setMessage(err.toString());
            AlertDialog dialog = dialogBuilder.create();
            dialog.show();
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
