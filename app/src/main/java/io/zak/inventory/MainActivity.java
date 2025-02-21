package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;

/**
 * MainActivity is the entry point of the app. Checks user login.
 * Tasks:
 *  a. If no user logged in and no User registered, go to Register activity.
 *  b. If no user logged in but at least one User registered, go to Login activity.
 *  c. If user is already logged in, go to Home activity.
 */
public class MainActivity extends AppCompatActivity {

    // for debugging only
    private static final String DEBUG_NAME = "Main";

    // collects all reactive calls
    private CompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        Log.d(DEBUG_NAME, "Checking user login...");
        // if no user login, check database if any user exits, if no user exist, go to Register activity
        if (Utils.getLoginId(this) == -1) {
            disposables.add(Single.fromCallable(() -> AppDatabaseImpl.getDatabase(getApplicationContext()).userDao().getSize())
                    .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(size -> {
                        if (size > 0) showLogin();
                        else showRegister();
                    }, err -> {
                        throw new RuntimeException("Database Error:\n" + err);
                    }));
        } else {
            // user login exists, go to Home activity
            showHome();
        }
    }

    private void showLogin() {
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }

    private void showRegister() {
        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
        finish();
    }

    private void showHome() {
        startActivity(new Intent(getApplicationContext(), HomeActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(DEBUG_NAME, "Destroying resources...");
        disposables.dispose();
    }
}