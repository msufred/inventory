package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.User;

public class RegisterActivity extends AppCompatActivity {

    private static final String DEBUG_NAME = "Register";

    private EditText etUsername, etPassword, etPasswordConfirm;
    private Button btnRegister, btnLogin;
    private ProgressBar progress;

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        btnRegister = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);
        progress = findViewById(R.id.progress_circular);
        progress.setVisibility(View.INVISIBLE);
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
        Log.d(DEBUG_NAME, "Validating fields...");
        boolean validated = !etUsername.getText().toString().isEmpty() && !etPassword.getText().toString().isEmpty() &&
                !etPasswordConfirm.getText().toString().isEmpty() && (
                        etPasswordConfirm.getText().toString().equals(etPassword.getText().toString())
                );
        Log.d(DEBUG_NAME, "Validated: " + validated);
        return validated;
    }

    private void registerAndClose() {
        User user = new User();
        user.username = Utils.normalize(etUsername.getText().toString());
        user.password = Utils.normalize(etPassword.getText().toString()); // TODO hash password

        // Register and start MainActivity
        progress.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(DEBUG_NAME, "Registering new User: " + Thread.currentThread());
            AppDatabaseImpl.getDatabase(getApplicationContext()).userDao().insertAll(user);
            return true;
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(done -> {
            Log.d(DEBUG_NAME, "DONE: " + Thread.currentThread());
            progress.setVisibility(View.INVISIBLE);
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }, err -> {
            Log.e(DEBUG_NAME, "Database Error: " + err);
            progress.setVisibility(View.INVISIBLE);
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
        Log.d(DEBUG_NAME, "Destroying resources...");
        disposables.dispose();
    }
}
