package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrderItem;
import io.zak.inventory.data.entities.WarehouseStock;
import io.zak.inventory.data.relations.DeliveryItemDetails;

public class EditDeliveryOrderItemActivity extends AppCompatActivity {

    private static final String TAG = "AddDeliveryOrderItem";

    // Widgets
    private TextView tvRemainingStocks, tvProductName;
    private ImageButton btnBack, btnMinus, btnPlus, btnDelete;
    private EditText etQuantity;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private WarehouseStock mWarehouseStock;                         // WarehouseStock the DeliveryOrderItem is taken out
    private int mCurrentRemainingStocks;
    private DeliveryItemDetails mDeliveryItemDetails;               // DeliveryOrderItem (aka DeliveryItemDetails) to edit/delete
    private int mQuantity;                                          // current quantity

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_delivery_order_item);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        tvRemainingStocks = findViewById(R.id.tv_remaining_stocks);
        tvProductName = findViewById(R.id.tv_product_name);
        btnBack = findViewById(R.id.btn_back);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        btnDelete = findViewById(R.id.btn_delete);
        etQuantity = findViewById(R.id.et_quantity);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnMinus.setOnClickListener(v -> decrementQuantity());
        btnPlus.setOnClickListener(v -> incrementQuantity());
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> updateDeliveryOrderItem());
        btnDelete.setOnClickListener(v -> {
            dialogBuilder.setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this Delivery Order Item?")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        dialog.dismiss();
                        deleteDeliveryOrderItem();
                    });
            dialogBuilder.create().show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // First, check if DeliveryOrder ID is passed via Intent
        int id = getIntent().getIntExtra("delivery_order_item_id", -1);
        if (id == -1) {
            // no passed id, return to HomeActivity
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Delivery Order Item ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return; // exit
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryOrderItem entry with ID=" + id);
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrderItems().getDeliveryItemWithDetails(id);
        }).flatMap(deliveryItemDetails -> {
            Log.d(TAG, "Returned with delivery item list size=" + deliveryItemDetails.size());
            mDeliveryItemDetails = deliveryItemDetails.get(0); // list size is ALWAYS 1
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching WarehouseStock");
                return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouseStocks().getWarehouseStock(mDeliveryItemDetails.deliveryOrderItem.fkWarehouseStockId);
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(warehouseStocks -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + warehouseStocks.size());
            mWarehouseStock = warehouseStocks.get(0);

            // display info
            tvProductName.setText(mDeliveryItemDetails.product.productName);
            mQuantity = mDeliveryItemDetails.deliveryOrderItem.quantity;
            mCurrentRemainingStocks = mWarehouseStock.quantity - mWarehouseStock.takenOut;
            etQuantity.setText(String.valueOf(mQuantity));
            tvRemainingStocks.setText(String.valueOf(mCurrentRemainingStocks));
        }, err -> {
            // If error occurs, inform user here.
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while retrieving Delivery Orders and list of Warehouses: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void incrementQuantity() {
        String str = etQuantity.getText().toString().trim();
        mQuantity = str.isBlank() ? 1 : Integer.parseInt(str);
        mQuantity += 1; // add 1
        if (mQuantity > mCurrentRemainingStocks) mQuantity = mCurrentRemainingStocks;
        etQuantity.setText(String.valueOf(mQuantity));
    }

    private void decrementQuantity() {
        String str = etQuantity.getText().toString().trim();
        mQuantity = str.isBlank() ? 1 : Integer.parseInt(str);
        mQuantity -= 1; // subtract 1
        if (mQuantity < 1) mQuantity = 1;
        etQuantity.setText(String.valueOf(mQuantity));
    }

    private void updateDeliveryOrderItem() {
        DeliveryOrderItem deliveryOrderItem = mDeliveryItemDetails.deliveryOrderItem;

        // for WarehouseStock
        int diff = mQuantity - deliveryOrderItem.quantity;

        deliveryOrderItem.quantity = mQuantity;
        deliveryOrderItem.subtotal = mQuantity * mDeliveryItemDetails.product.price;

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating Delivery Order Item");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrderItems().update(deliveryOrderItem);
        }).flatMap(rowCount -> {
            if (rowCount > 0) Log.d(TAG, "DeliveryOrderItem updated. Updating WarehouseStock");
            mWarehouseStock.takenOut = mWarehouseStock.takenOut + diff;
            return Single.fromCallable(() -> AppDatabaseImpl.getDatabase(getApplicationContext()).warehouseStocks().update(mWarehouseStock));
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "WarehouseStock updated");
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while updating Delivery Order Item: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void deleteDeliveryOrderItem() {
        DeliveryOrderItem deliveryOrderItem = mDeliveryItemDetails.deliveryOrderItem;
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting DeliveryOrderItem");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrderItems().delete(deliveryOrderItem);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "DeliveryOrderItem deleted.");
            if (rowCount > 0) {
                Toast.makeText(this, "Delivery Order Item deleted.", Toast.LENGTH_SHORT).show();
            }
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while deleting Delivery Order Item: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources...");
        disposables.dispose();
    }
}
