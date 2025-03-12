package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.zak.inventory.firebase.UserEntry;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfile";

    // Widgets
    private EditText etName, etPosition, etAddress, etEmail, etContact;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private AlertDialog.Builder dialogBuilder;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser mUser;     // logged in user (authenticated in Firebase)
    private UserEntry mUserEntry;   // "users" entry related to the logged in user

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        getWidgets();
        setListeners();
        mAuth = FirebaseAuth.getInstance();
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etPosition = findViewById(R.id.et_position);
        etAddress = findViewById(R.id.et_address);
        etEmail = findViewById(R.id.et_email);
        etContact = findViewById(R.id.et_contact);
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
    protected void onStart() {
        super.onStart();
        // fetch current user
        mUser = mAuth.getCurrentUser();
        if (mUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

        // fetch user info (mUser is assumed to be not null)
        progressGroup.setVisibility(View.VISIBLE);
        mDatabase.child("users")
                .child(mUser.getUid())
                .get()
                .addOnCompleteListener(this, task -> {
                    progressGroup.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        mUserEntry = task.getResult().getValue(UserEntry.class);
                        displayInfo(mUserEntry);
                    } else {
                        Log.w(TAG, "fetch user info failure", task.getException());
                        goBack();
                    }
                });
    }

    private void displayInfo(UserEntry user) {
        if (user != null) {
            etName.setText(user.fullName);
            etPosition.setText(user.position);
            etAddress.setText(user.address);
            etEmail.setText(user.email);
            etContact.setText(user.contactNo);
        }
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
        if (etPosition.getText().toString().trim().isEmpty()) {
            etPosition.setError("Required");
            isValid = false;
        }
        if (etContact.getText().toString().trim().isEmpty()) {
            etContact.setError("Required");
            isValid = false;
        } else if (!etContact.getText().toString().matches("^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$")){
            etContact.setError("Invalid Contact Number");
            isValid = false;
        }
        if (etAddress.getText().toString().trim().isEmpty()) {
            etAddress.setError("Required");
            isValid = false;
        }
        if (etEmail.getText().toString().isBlank()) {
            etEmail.setError("Required");
            isValid = false;
        }

        return isValid;
    }

    private void saveAndClose() {
        mUserEntry.fullName = Utils.normalize(etName.getText().toString());
        mUserEntry.position = Utils.normalize(etPosition.getText().toString());
        mUserEntry.address = Utils.normalize(etAddress.getText().toString());
        mUserEntry.email = Utils.normalize(etEmail.getText().toString());
        mUserEntry.contactNo = Utils.normalize(etContact.getText().toString());

        progressGroup.setVisibility(View.VISIBLE);
        mDatabase.child("users")
                .child(mUser.getUid())
                .setValue(mUserEntry)
                .addOnCompleteListener(this, task -> {
                    progressGroup.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show();
                        goBack();
                    } else {
                        Log.w(TAG, "profile update failure", task.getException());
                        dialogBuilder.setTitle("Failure Update")
                                .setMessage("Failed to update Profile info: " + task.getException())
                                .setPositiveButton("Dismiss", (dialog, which) -> {
                                    dialog.dismiss();
                                    goBack();
                                });
                        dialogBuilder.create().show();
                    }
                });
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }
}
