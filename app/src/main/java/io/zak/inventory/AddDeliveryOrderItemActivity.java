package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.WarehouseSpinnerAdapter;
import io.zak.inventory.adapters.WarehouseStockDetailSpinnerAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.data.entities.DeliveryOrderItem;
import io.zak.inventory.data.entities.Warehouse;
import io.zak.inventory.data.entities.WarehouseStock;
import io.zak.inventory.data.relations.WarehouseStockDetails;

public class AddDeliveryOrderItemActivity extends AppCompatActivity {

    private static final String TAG = "AddDeliveryOrderItem";

    // Widgets
    private Spinner warehouseSpinner, productSpinner;
    private TextView emptyWarehouseSpinner, emptyProductSpinner, tvRemainingStocks;
    private ImageButton btnBack, btnMinus, btnPlus;
    private EditText etQuantity;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private List<Warehouse> warehouseList;                          // fetched in onResume
    private List<WarehouseStockDetails> warehouseStockDetailsList;  // fetched after selecting Warehouse from warehouseSpinner
    private Warehouse mWarehouse;                                   // selected Warehouse (from warehouseSpinner)
    private WarehouseStockDetails mWarehouseStockDetail;            // selected WarehouseStockDetail (from productsSpinner)
    private int mCurrentRemainingStocks;                            // depends on mWarehouseStockDetail
    private int mQuantity;                                          // set quantity to add to the DeliveryOrder

    private DeliveryOrder mDeliveryOrder;                           // this DeliveryOrderItem is added to

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_delivery_order_item);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        warehouseSpinner = findViewById(R.id.warehouse_spinner);
        productSpinner = findViewById(R.id.product_spinner);
        emptyWarehouseSpinner = findViewById(R.id.empty_warehouses_spinner);
        emptyProductSpinner = findViewById(R.id.empty_products_spinner);
        tvRemainingStocks = findViewById(R.id.tv_remaining_stocks);
        btnBack = findViewById(R.id.btn_back);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        etQuantity = findViewById(R.id.et_quantity);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        warehouseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (warehouseList != null) {
                    mWarehouse = warehouseList.get(position);
                    loadWarehouseStockDetails(mWarehouse);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (warehouseStockDetailsList != null) {
                    mWarehouseStockDetail = warehouseStockDetailsList.get(position);
                    // NOTE: remaining stocks is calculated by subtracting the takenOut from quantity
                    WarehouseStock warehouseStock = mWarehouseStockDetail.warehouseStock;
                    mCurrentRemainingStocks = warehouseStock.quantity - warehouseStock.takenOut;
                    tvRemainingStocks.setText(String.valueOf(mCurrentRemainingStocks));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        btnMinus.setOnClickListener(v -> decrementQuantity());
        btnPlus.setOnClickListener(v -> incrementQuantity());
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (mWarehouseStockDetail == null) {
                dialogBuilder.setTitle("Invalid Action")
                        .setMessage("No selected Product.")
                        .setPositiveButton("OK", (d, w) -> d.dismiss());
                dialogBuilder.create().show();
            } else {
                checkAndSave();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // First, check if DeliveryOrder ID is passed via Intent
        int id = getIntent().getIntExtra("delivery_order_id", -1);
        if (id == -1) {
            // no passed id, return to HomeActivity
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Delivery Order ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return; // exit
        }

        // Get Database
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());

        // At this point, DeliveryOrder ID is valid. Fetch the actual DeliveryOrder entry and
        // the list of all Warehouse.
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            // Background Thread (set in subscribeOn(Schedulers.io()) in this chain)
            Log.d(TAG, "Fetching DeliveryOrder entry");
            return database.deliveryOrders().getDeliveryOrder(id);
        }).flatMap(deliveryOrders -> {
            // Still in the background thread. Set the DeliveryOrder and return the list of Warehouse entries.
            Log.d(TAG, "Returned with list size=" + deliveryOrders.size());
            mDeliveryOrder = deliveryOrders.get(0); // list size is ALWAYS 1

            Log.d(TAG, "Fetching all Warehouse entries");
            return Single.fromCallable(() -> database.warehouses().getAll());
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(warehouses -> {
            // Application Thread (set in observeOn(AndroidSchedulers.mainThread() of this chain)
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + warehouses.size());

            // setup warehouseSpinner here
            warehouseList = warehouses;
            warehouseSpinner.setAdapter(new WarehouseSpinnerAdapter(this, warehouseList));
            emptyWarehouseSpinner.setVisibility(warehouses.isEmpty() ? View.VISIBLE : View.INVISIBLE);
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

    /**
     * Called when a Warehouse is selected from the warehouseSpinner. Retrieve all WarehouseStockDetails
     * stored in that Warehouse.
     * @param warehouse Warehouse
     */
    private void loadWarehouseStockDetails(Warehouse warehouse) {
        if (warehouse != null) {
            progressGroup.setVisibility(View.VISIBLE);
            disposables.add(Single.fromCallable(() -> {
                Log.d(TAG, "Fetching all WarehouseStockDetails");
                return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouseStocks().getWarehouseStocks(warehouse.warehouseId);
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(stockDetails -> {
                Log.d(TAG, "Returned with list size=" + stockDetails.size());
                progressGroup.setVisibility(View.GONE);

                // setup productSpinner
                warehouseStockDetailsList = stockDetails;
                productSpinner.setAdapter(new WarehouseStockDetailSpinnerAdapter(this, warehouseStockDetailsList));
                emptyProductSpinner.setVisibility(stockDetails.isEmpty() ? View.VISIBLE : View.INVISIBLE);
            }, err -> {
                Log.e(TAG, "Database error: " + err);
                progressGroup.setVisibility(View.GONE);
                dialogBuilder.setTitle("Database Error")
                        .setMessage("Error while retrieving Warehouse Stock Details: " + err)
                        .setPositiveButton("Dismiss", (dialog, which) -> {
                            dialog.dismiss();
                            goBack();
                        });
                dialogBuilder.create().show();
            }));
        }
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

    /**
     * Check if the product already exist in the delivery item list.
     */
    private void checkAndSave() {
        DeliveryOrderItem orderItem = new DeliveryOrderItem();
        orderItem.fkDeliveryOrderId = mDeliveryOrder.deliveryOrderId;
        orderItem.fkWarehouseStockId = mWarehouseStockDetail.warehouseStock.warehouseStockId;
        orderItem.fkProductId = mWarehouseStockDetail.warehouseStock.fkProductId;

        String str = etQuantity.getText().toString().trim();
        int qty = str.isBlank() || str.equalsIgnoreCase("0") ? 1 : Integer.parseInt(str);
        if (qty > mCurrentRemainingStocks) qty = mCurrentRemainingStocks;

        orderItem.quantity = qty;
        orderItem.subtotal = qty * mWarehouseStockDetail.product.price;

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Checking if DeliveryOrderItem already exists.");
            return AppDatabaseImpl
                    .getDatabase(getApplicationContext())
                    .deliveryOrderItems()
                    .getDeliveryOrderItemByProduct(mDeliveryOrder.deliveryOrderId, orderItem.fkProductId);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(deliveryOrderItems -> {
            progressGroup.setVisibility(View.GONE);
            // if already exist, prompt user
            if (!deliveryOrderItems.isEmpty()) {
                dialogBuilder.setTitle("Invalid Action").setMessage("The selected product is already in the delivery item list.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                dialogBuilder.create().show();
            } else {
                saveAndClose(orderItem);
            }
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while checking Delivery Order Item: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void saveAndClose(DeliveryOrderItem orderItem) {
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving new DeliveryOrderItem");
            return database.deliveryOrderItems().insert(orderItem);
        }).flatMap(id -> {
            Log.d(TAG, "Returned with id=" + id.intValue());
            Log.d(TAG, "Updating WarehouseStock takenOut value");
            WarehouseStock warehouseStock = mWarehouseStockDetail.warehouseStock;
            warehouseStock.takenOut = warehouseStock.takenOut + mQuantity;
            return Single.fromCallable(() -> database.warehouseStocks().update(warehouseStock));
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with row count=" + rowCount);
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while adding Delivery Order Item: " + err)
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
