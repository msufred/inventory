package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.DeliveryItemListAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.relations.DeliveryDetails;
import io.zak.inventory.data.relations.DeliveryItemDetails;

public class ViewDeliveryOrderActivity extends AppCompatActivity implements DeliveryItemListAdapter.OnItemClickListener {

    private static final String TAG = "DeliveryOrderItems";

    // Widgets
    private TextView tvVehicleName, tvPlateNo, tvEmployeeName;
    private RecyclerView recyclerView;
    private Button btnAddItem;
    private TextView tvItemCount, tvTotalAmount;
    private Button btnLoadToVehicle;

    // for RecyclerView
    private DeliveryItemListAdapter adapter;
    private List<DeliveryItemDetails> deliveryItemList;
    private final Comparator<DeliveryItemDetails> comparator = Comparator.comparing(deliveryItemDetails -> deliveryItemDetails.product.productName);

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

    private void getWidgets() {
        tvVehicleName = findViewById(R.id.tv_vehicle_name);
        tvPlateNo = findViewById(R.id.tv_plate_no);
        tvEmployeeName = findViewById(R.id.tv_employee_name);
        recyclerView = findViewById(R.id.recycler_view);
        btnAddItem = findViewById(R.id.btn_add_item);
        tvItemCount = findViewById(R.id.tv_item_count);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        btnLoadToVehicle = findViewById(R.id.btn_load_to_vehicle);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeliveryItemListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {

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
            fetchOrderItems(mDeliveryDetails.deliveryOrder.deliveryOrderId);
        }));
    }

    private void displayInfo(DeliveryDetails deliveryDetails) {
        if (deliveryDetails != null) {
            tvVehicleName.setText(deliveryDetails.vehicle.vehicleName);
            tvPlateNo.setText(deliveryDetails.vehicle.plateNo);
            tvEmployeeName.setText(deliveryDetails.employee.employeeName);
        }
    }

    private void fetchOrderItems(int id) {
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryItemDetails: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrderItems().getDeliveryItemDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size());
            deliveryItemList = list;
            adapter.replaceAll(deliveryItemList);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching DeliveryOrderItem entries: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            DeliveryItemDetails details = adapter.getItem(position);
            if (details != null) {
                Log.d(TAG, "Selected item: " + details.product.productName);
                // TODO
            }
        }
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
