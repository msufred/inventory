package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * MainActivity is the entry point of the app. Checks user login.
 * Tasks:
 *  a. If no user logged in, go to Login activity.
 *  b. If user is already logged in, go to Home activity.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Main";
    private CompositeDisposable disposables;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: forcing light mode for now, implement dark mode support later
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // check signed in user in Firebase database
        Log.d(TAG, "checking current user");
        if (mAuth.getCurrentUser() == null) {
            showLogin();
        } else {
            showHome();
        }
    }

    private void showLogin() {
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }

    private void showHome() {
        startActivity(new Intent(getApplicationContext(), HomeActivity.class));
        finish();
    }
}