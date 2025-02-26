package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.relations.DeliveryDetails;

public class DeliveryOrderItemsActivity extends AppCompatActivity {

    private static final String TAG = "DeliveryOrderItems";

    // Widgets
    private TextView tvVehicleName, tvPlateNo, tvEmployeeName;
    private RecyclerView recyclerView;
    private Button btnAddItem;
    private TextView tvItemCount, tvTotalAmount;
    private Button btnLoadToTruck;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private DeliveryDetails mDeliveryDetails;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_order_items);
        getWidgets();
        setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = getIntent().getIntExtra("delivery_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Delivery Order ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryDetails entry: " + Thread.currentThread());
            return database.deliveryOrders().getDeliveryOrderDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
            Log.d(TAG, "Returned with list size=" + orders.size() + " " + Thread.currentThread());
            if (orders.isEmpty()) {
                dialogBuilder.setMessage("No Delivery Order found.")
                        .setPositiveButton("Dismiss", (dialog, which) -> {
                            dialog.dismiss();
                            goBack();
                        });
                dialogBuilder.create().show();
                return;
            }
            mDeliveryDetails = orders.get(0);
            displayInfo(mDeliveryDetails);
            fetchOrderItems(mDeliveryDetails.deliveryOrder.id);
        }));
    }

    private void displayInfo(DeliveryDetails deliveryDetails) {
        if (deliveryDetails != null) {
            tvVehicleName.setText(deliveryDetails.vehicleName);
            tvPlateNo.setText(deliveryDetails.plateNo);
            tvEmployeeName.setText(deliveryDetails.deliveryOrder.employeeName);
        }
    }

    private void fetchOrderItems(int id) {
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryItemDetails: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrderItems().getDeliveryItemDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            // TODO
        }));
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
