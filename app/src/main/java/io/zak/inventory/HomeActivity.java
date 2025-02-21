package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.User;

public class HomeActivity extends AppCompatActivity {

    private static final String DEBUG_NAME = "Home";

    // Widgets
    // private TextView tvUsername;
    private FrameLayout frameLayout;
    private BottomNavigationView bottomNavigationView;

    // Fragments
    private DashboardFragment dashboardFragment;
    private DeliveryFragment deliveryFragment;
    private OrdersFragment ordersFragment;
    private ProfileFragment profileFragment;

    private CompositeDisposable disposables;
    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWidgets();
        setListeners();

        // create Fragments
        dashboardFragment = new DashboardFragment();
        deliveryFragment = new DeliveryFragment();
        ordersFragment = new OrdersFragment();
        profileFragment = new ProfileFragment();

        setView(dashboardFragment);
    }

    private void getWidgets() {
        // tvUsername = findViewById(R.id.text_user);
        frameLayout = findViewById(R.id.frame_layout);
        bottomNavigationView = findViewById(R.id.bottom_nav);
    }

    private void setListeners() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_trucks) {
                setView(deliveryFragment);
            } else if (item.getItemId() == R.id.menu_orders) {
                setView(ordersFragment);
            } else if (item.getItemId() == R.id.menu_profile) {
                setView(profileFragment);
            } else {
                setView(dashboardFragment);
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = Utils.getLoginId(getApplicationContext());
        if (id == -1) {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        } else {
            getUser(id);
        }
    }

    private void setView(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
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
                Log.d(DEBUG_NAME, "User " + mUser.username);
                displayUserInfo();
            }
        }));
    }

    private void displayUserInfo() {
        if (mUser == null) return;
        // tvUsername.setText(mUser.username);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(DEBUG_NAME, "Destroying resources...");
        disposables.dispose();
    }
}
