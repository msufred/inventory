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
import io.zak.inventory.data.entities.Consumer;

public class AddConsumerActivity extends AppCompatActivity {

    private static final String TAG = "AddConsumer";

    // Widgets
    private EditText etName, etContact, etEmail, etAddress;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;

    private Drawable errorDrawable;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_consumer);
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

        errorDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_x_circle);

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
        // clear drawables
        etName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        boolean isBlank = etName.getText().toString().isBlank();
        if (isBlank) {
            etName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }

        return !isBlank;
    }

    private void saveAndClose() {
        Consumer consumer = new Consumer();
        consumer.name = Utils.normalize(etName.getText().toString());
        consumer.contactNo = Utils.normalize(etContact.getText().toString());
        consumer.email = Utils.normalize(etEmail.getText().toString());
        consumer.address = Utils.normalize(etAddress.getText().toString());

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Consumer entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).consumers().insert(consumer);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Returned with ID: " + id + " " + Thread.currentThread());
            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);

            dialogBuilder.setTitle("Database Error").setMessage("Error while saving Consumer entry: " + err);
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
