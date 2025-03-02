package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.data.relations.DeliveryDetails;
import io.zak.inventory.data.relations.DeliveryItemDetails;

public class ViewDeliveryOrderActivity extends AppCompatActivity implements DeliveryItemListAdapter.OnItemClickListener {

    private static final String TAG = "DeliveryOrderItems";

    // Widgets
    private TextView tvTrackingNo, tvItemCount, tvTotalAmount;
    private ImageButton btnBack, btnEdit;
    private RecyclerView recyclerView;
    private Button btnAddItem;
    private Button btnLoadToVehicle; // renamed to Checkout
    private RelativeLayout progressGroup;
    private LinearLayout buttonGroup;

    // for RecyclerView
    private DeliveryItemListAdapter adapter;
    private List<DeliveryItemDetails> deliveryItemList;

    // sort items by product name
    private final Comparator<DeliveryItemDetails> comparator = Comparator.comparing(deliveryItemDetails -> deliveryItemDetails.product.productName);

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private DeliveryDetails mDeliveryDetails; // fetched in onResume

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_delivery_order);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        tvTrackingNo = findViewById(R.id.tv_tracking_no);
        tvItemCount = findViewById(R.id.tv_item_count);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        recyclerView = findViewById(R.id.recycler_view);
        btnAddItem = findViewById(R.id.btn_add_item);
        btnLoadToVehicle = findViewById(R.id.btn_load_to_vehicle);
        progressGroup = findViewById(R.id.progress_group);
        buttonGroup = findViewById(R.id.button_group);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeliveryItemListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());

        btnEdit.setOnClickListener(v -> {
            if (mDeliveryDetails != null) {
                Intent intent = new Intent(this, EditDeliveryOrderActivity.class);
                intent.putExtra("delivery_order_id", mDeliveryDetails.deliveryOrder.deliveryOrderId);
                startActivity(intent);
            }
        });

        btnAddItem.setOnClickListener(v -> {
            if (mDeliveryDetails != null) {
                Intent intent = new Intent(this, AddDeliveryOrderItemActivity.class);
                intent.putExtra("delivery_order_id", mDeliveryDetails.deliveryOrder.deliveryOrderId);
                startActivity(intent);
            }
        });

        btnLoadToVehicle.setOnClickListener(v -> {
            // NOTE: If status is set to "Delivered", user can't add more products.
            dialogBuilder.setTitle("Checkout Delivery")
                    .setMessage("Are you sure you want to checkout this delivery? You can't add more products once checked out.")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        dialog.dismiss();
                        checkoutDelivery();
                    });
            dialogBuilder.create().show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // Check ID
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

        // ID exists, fetch all Warehouse entries and DeliveryDetail
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryDetails entry.");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().getDeliveryOrderDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(deliveries -> {
            Log.d(TAG, "Returned with list size=" + deliveries.size());
            showProgress(false);

            if (deliveries.isEmpty()) {
                dialogBuilder.setMessage("No Delivery Order found.")
                        .setPositiveButton("Dismiss", (dialog, which) -> {
                            dialog.dismiss();
                            goBack();
                        });
                dialogBuilder.create().show();
                return;
            }

            // set DeliveryOrder (aka DeliverDetails) and display information
            mDeliveryDetails = deliveries.get(0);
            displayInfo(mDeliveryDetails);

            // get all items of this delivery order
            fetchOrderItems(mDeliveryDetails.deliveryOrder.deliveryOrderId);

            String status = mDeliveryDetails.deliveryOrder.deliveryOrderStatus;
            if (status.equalsIgnoreCase("Delivered")) {
                buttonGroup.setVisibility(View.GONE);
                btnEdit.setVisibility(View.GONE);
            }
        }, err -> {
            showProgress(false);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching DeliveryDetails entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(DeliveryDetails deliveryDetails) {
        if (deliveryDetails != null) {
            tvTrackingNo.setText(deliveryDetails.deliveryOrder.trackingNo);
            tvTotalAmount.setText(String.valueOf(deliveryDetails.deliveryOrder.totalAmount));
        }
    }

    private void fetchOrderItems(int id) {
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryItemDetails for delivery order with id=" + id);
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrderItems().getDeliveryItemsWithDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size());
            deliveryItemList = list;
            adapter.replaceAll(deliveryItemList);
            tvItemCount.setText(String.valueOf(list.size()));
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
            DeliveryItemDetails mSelectedDeliveryItem = adapter.getItem(position);
            if (mSelectedDeliveryItem == null) return;
            if (mDeliveryDetails.deliveryOrder.deliveryOrderStatus.equalsIgnoreCase("Processing")) {
                Intent intent = new Intent(this, EditDeliveryOrderItemActivity.class);
                intent.putExtra("delivery_order_item_id", mSelectedDeliveryItem.deliveryOrderItem.deliveryOrderItemId);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, ViewDeliveryOrderItemActivity.class);
                intent.putExtra("delivery_order_item_id", mSelectedDeliveryItem.deliveryOrderItem.deliveryOrderItemId);
                startActivity(intent);
            }
        }
    }

    private void checkoutDelivery() {
        if (mDeliveryDetails != null) {
            DeliveryOrder deliveryOrder = mDeliveryDetails.deliveryOrder;
            deliveryOrder.deliveryOrderStatus = "Delivered";

            progressGroup.setVisibility(View.VISIBLE);
            disposables.add(Single.fromCallable(() -> {
                Log.d(TAG, "Updating delivery order.");
                return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrders().update(deliveryOrder);
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
                progressGroup.setVisibility(View.GONE);
                if (rowCount > 0) {
                    Toast.makeText(this, "Delivery Order updated.", Toast.LENGTH_SHORT).show();
                }
                goBack();
            }, err -> {
                progressGroup.setVisibility(View.GONE);
                Log.e(TAG, "Database Error: " +  err);
                dialogBuilder.setTitle("Database Error")
                        .setMessage("Error while updating Delivery Order: " + err)
                        .setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                            goBack();
                        });
                dialogBuilder.create().show();
            }));
        }
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private void showProgress(boolean show) {
        progressGroup.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
