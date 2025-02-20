package io.zak.inventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.User;

public class HomeActivity extends AppCompatActivity {

    private static final String DEBUG_NAME = "Home";

    // Widgets
    private TextView tvUsername;

    private CompositeDisposable disposables;
    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        tvUsername = findViewById(R.id.text_user);

        disposables = new CompositeDisposable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int id = getLoginId();
        if (id == -1) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else {
            getUser(id);
        }
    }

    private void getUser(int id) {
        disposables.add(Single.fromCallable(() -> {
            Log.d(DEBUG_NAME, "Fetch user: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).userDao().getUser(id);
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(users -> {
            if (users.isEmpty()) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            } else {
                mUser = users.get(0);
                displayUserInfo();
            }
        }));
    }

    private void displayUserInfo() {
        if (mUser == null) return;
        tvUsername.setText(mUser.username);
    }

    private int getLoginId() {
        SharedPreferences sp = getSharedPreferences("login", MODE_PRIVATE);
        return sp.getInt("id", -1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
