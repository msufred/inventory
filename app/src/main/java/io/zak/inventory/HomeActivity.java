package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.User;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "Home";

    private BottomNavigationView bottomNavigationView;

    // Fragments
    private DashboardFragment dashboardFragment;
    private DeliveryFragment deliveryFragment;
    private OrdersFragment ordersFragment;
    private ProfileFragment profileFragment;

    private CompositeDisposable disposables;
    private User mUser;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        getWidgets();
        setListeners();

        mAuth = FirebaseAuth.getInstance();

        // create Fragments
        dashboardFragment = new DashboardFragment();
        deliveryFragment = new DeliveryFragment();
        ordersFragment = new OrdersFragment();
        profileFragment = new ProfileFragment();

        setView(dashboardFragment);
    }

    private void getWidgets() {
        // Widgets
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

    /**
     * Change the current view in the FrameLayout.
     * @param fragment Fragment view
     */
    private void setView(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, fragment).commit();
    }
}
