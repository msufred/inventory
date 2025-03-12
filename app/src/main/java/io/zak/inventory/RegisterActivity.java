package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.zak.inventory.firebase.UserEntry;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "Register";

    private EditText etEmail, etPassword, etPasswordConfirm;
    private Button btnRegister, btnLogin;
    private ProgressBar progress;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getWidgets();
        setListeners();
        mAuth = FirebaseAuth.getInstance();
    }

    private void getWidgets() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);
        progress = findViewById(R.id.progress_circular);
        progress.setVisibility(View.INVISIBLE);
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnRegister.setOnClickListener(evt -> {
            if (validated()) registerAndClose();
        });

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });
    }

    private boolean validated() {
        boolean isValid = true;
        String email = etEmail.getText().toString();
        String pass = etPassword.getText().toString();
        String passConfirm = etPasswordConfirm.getText().toString();

        if (email.isBlank()) {
            etEmail.setError("Required");
            isValid = false;
        }

        if (pass.isBlank()) {
            etPassword.setError("Required");
            isValid = false;
        }

        if (passConfirm.isBlank() || !passConfirm.contentEquals(pass)) {
            etPasswordConfirm.setError("Do not match");
            isValid = false;
        }

        return isValid;
    }

    private void registerAndClose() {
        String email = Utils.normalize(etEmail.getText().toString());
        String password = Utils.normalize(etPassword.getText().toString());

        progress.setVisibility(View.VISIBLE);
        Log.d(TAG, "Registering user");
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progress.setVisibility(View.INVISIBLE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "User Registered", Toast.LENGTH_SHORT).show();
                        createUserEntry(email);
                    } else {
                        Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Registration failure", task.getException());
                    }
                });
    }

    private void createUserEntry(String email) {
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser != null) {
            progress.setVisibility(View.VISIBLE);
            Log.d(TAG, "creating new user entry [Firebase]");
            UserEntry userEntry = new UserEntry();
            userEntry.fullName = "New User";
            userEntry.position = "Employee";
            userEntry.email = email;

            // get Firebase database reference
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            database.child("users").child(fUser.getUid()).setValue(userEntry)
                    .addOnCompleteListener(this, task -> {
                        progress.setVisibility(View.INVISIBLE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "user created");
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "user creation failure", task.getException());
                            dialogBuilder.setTitle("Database Error")
                                    .setMessage("Error while creating user entry: " + task.getException())
                                    .setPositiveButton("Dismiss", (dialog, which) -> {
                                        dialog.dismiss();
                                    });
                            dialogBuilder.create().show();
                        }
                    });
        }
    }
}
