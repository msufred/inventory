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

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Login";

    // Widgets
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private ProgressBar progress;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWidgets();
        setListeners();
        mAuth = FirebaseAuth.getInstance();
    }

    private void getWidgets() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        progress = findViewById(R.id.progress_circular);
        progress.setVisibility(View.INVISIBLE);
        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> {
            if (validated()) login();
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }

    private boolean validated() {
        boolean isValid = true;
        if (etEmail.getText().toString().isBlank()) {
            etEmail.setError("Required");
            isValid = false;
        }
        if (etPassword.getText().toString().isBlank()) {
            etPassword.setError("Required");
            isValid = false;
        }
        return isValid;
    }

    private void login() {
        // validate
        String email = Utils.normalize(etEmail.getText().toString());
        String password = Utils.normalize(etPassword.getText().toString());

        progress.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progress.setVisibility(View.INVISIBLE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "User Signed In", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        dialogBuilder.setTitle("Sign In Failure")
                                .setMessage("Invalid Username and/or Password")
                                .setPositiveButton("Try Again", (dialog, which) -> dialog.dismiss());
                        dialogBuilder.create().show();
                    }
                });
    }
}
