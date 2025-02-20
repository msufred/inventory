package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;

public class MainActivity extends AppCompatActivity {

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        disposables = new CompositeDisposable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * Check Users in database. If no user show Login view, otherwise show Register view.
         */
        disposables.add(Single.fromCallable(() -> AppDatabaseImpl.getDatabase(getApplicationContext()).userDao().getSize())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(size -> {
                    if (size > 0) showLogin();
                    else showRegister();
                }, err -> {
                    throw new RuntimeException("Database Error:\n" + err);
                }));
    }

    private void showLogin() {
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
    }

    private void showRegister() {
        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}