package io.zak.inventory;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class LoginActivity extends AppCompatActivity {

    private static final String DEBUG_NAME = "Login";

    // Widgets
    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;
    private ProgressBar progress;

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        progress = findViewById(R.id.progress_circular);
        progress.setVisibility(View.INVISIBLE);
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> login());

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            finish();
        });
    }

    private void login() {
        // validate
        String username = Utils.normalize(etUsername.getText().toString());
        String password = Utils.normalize(etPassword.getText().toString());
        boolean validated = !username.isBlank() && !password.isBlank();
        Log.d(DEBUG_NAME, "Validating login credentials: " + validated);

        if (validated) {
            // find User
            progress.setVisibility(View.VISIBLE);
            disposables.add(Single.fromCallable(() -> {
                Log.d(DEBUG_NAME, String.format("Find User (%s, %s): %s", username, password, Thread.currentThread()));
                return AppDatabaseImpl.getDatabase(getApplicationContext()).userDao().getUser(username, password);
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(users -> {
                progress.setVisibility(View.INVISIBLE);
                Log.d(DEBUG_NAME, String.format("DONE\nReturned list with size:%d %s", users.size(), Thread.currentThread()));

                // NOTE! Assumes that users list size=1
                if (!users.isEmpty()) {
                    // save user id to SharedPreferences
                    Utils.saveLoginId(getApplicationContext(), users.get(0).id);
                    // go to home activity
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    // destroy this activity
                    finish();
                }
            }, err -> {
                progress.setVisibility(View.INVISIBLE);
                Log.e(DEBUG_NAME, "Database Error: " + err);
            }));
        }
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
