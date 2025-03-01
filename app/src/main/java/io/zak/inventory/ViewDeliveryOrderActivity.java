package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.DeliveryItemListAdapter;
import io.zak.inventory.adapters.WarehouseSpinnerAdapter;
import io.zak.inventory.adapters.WarehouseStockDetailSpinnerAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrderItem;
import io.zak.inventory.data.entities.Warehouse;
import io.zak.inventory.data.relations.DeliveryDetails;
import io.zak.inventory.data.relations.DeliveryItemDetails;
import io.zak.inventory.data.relations.WarehouseStockDetails;

public class ViewDeliveryOrderActivity extends AppCompatActivity implements DeliveryItemListAdapter.OnItemClickListener {

    private static final String TAG = "DeliveryOrderItems";

    // Widgets
    private ImageButton btnBack;
    private TextView tvVehicleName, tvPlateNo, tvEmployeeName;
    private RecyclerView recyclerView;
    private Button btnAddItem;
    private TextView tvItemCount, tvTotalAmount;
    private Button btnLoadToVehicle;
    private RelativeLayout progressGroup;

    // Add Dialog Widgets
    private Spinner warehouseSpinner, productSpinner;
    private TextView tvRemainingStocks;
    private TextView noProducts;
    private EditText etQuantity;

    // for RecyclerView
    private DeliveryItemListAdapter adapter;
    private List<DeliveryItemDetails> deliveryItemList;
    private final Comparator<DeliveryItemDetails> comparator = Comparator.comparing(deliveryItemDetails -> deliveryItemDetails.product.productName);

    // for Add Dialog
    private List<Warehouse> warehouseList;
    private List<WarehouseStockDetails> warehouseStocks;
    private Warehouse mWarehouse; // selected Warehouse entry
    private WarehouseStockDetails mStockDetails; // selected WarehouseStockDetails entry
    private int currRemainingStocks; // remaining stocks of selected stock

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog addDialog;

    private AppDatabase database;
    private DeliveryDetails mDeliveryDetails;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_delivery_order);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnBack = findViewById(R.id.btn_back);
        tvVehicleName = findViewById(R.id.tv_vehicle_name);
        tvPlateNo = findViewById(R.id.tv_plate_no);
        tvEmployeeName = findViewById(R.id.tv_employee_name);
        recyclerView = findViewById(R.id.recycler_view);
        btnAddItem = findViewById(R.id.btn_add_item);
        tvItemCount = findViewById(R.id.tv_item_count);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        btnLoadToVehicle = findViewById(R.id.btn_load_to_vehicle);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeliveryItemListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());

        btnAddItem.setOnClickListener(v -> {
            if (addDialog == null) createAddItemDialog();
            addDialog.show();
        });

        btnLoadToVehicle.setOnClickListener(v -> {
            // TODO
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check id
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

        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Warehouse entries.");
            return getDatabase().warehouses().getAll();
        }).flatMap(warehouses -> {
            Log.d(TAG, "Returned with list size=" + warehouses.size());
            warehouseList = warehouses;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching DeliveryDetails entry.");
                return getDatabase().deliveryOrders().getDeliveryOrderDetails(id);
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
            Log.d(TAG, "Returned with list size=" + orders.size());
            showProgress(false);
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
            tvVehicleName.setText(deliveryDetails.vehicle.vehicleName);
            tvPlateNo.setText(deliveryDetails.vehicle.plateNo);
            tvEmployeeName.setText(deliveryDetails.employee.employeeName);
        }
    }

    private void fetchOrderItems(int id) {
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryItemDetails: " + Thread.currentThread());
            return getDatabase().deliveryOrderItems().getDeliveryItemDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size=" + list.size());
            deliveryItemList = list;
            adapter.replaceAll(deliveryItemList);
            calculateTotal(list);
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

    private void calculateTotal(List<DeliveryItemDetails> list) {
        int count = 0;
        double total = 0;

        for (DeliveryItemDetails details : list) {
            count++;
            total += details.deliveryOrderItem.subtotal;
        }

        tvItemCount.setText(String.valueOf(count));
        tvTotalAmount.setText(Utils.toStringMoneyFormat(total));
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

    private void createAddItemDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_product, null);

        warehouseSpinner = view.findViewById(R.id.warehouse_spinner);
        TextView noWarehouse = view.findViewById(R.id.empty_warehouse_spinner);
        warehouseSpinner.setAdapter(new WarehouseSpinnerAdapter(this, warehouseList));
        noWarehouse.setVisibility(warehouseList.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        productSpinner = view.findViewById(R.id.product_spinner);
        noProducts = view.findViewById(R.id.empty_products_spinner);

        TextView tvRemainingStocks = view.findViewById(R.id.tv_remaining_stocks);

        ImageButton btnMinus = view.findViewById(R.id.btn_minus);
        ImageButton btnPlus = view.findViewById(R.id.btn_plus);
        etQuantity = view.findViewById(R.id.et_quantity);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnSave = view.findViewById(R.id.btn_save);

        // set listeners
        warehouseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mWarehouse = warehouseList.get(position);
                loadWarehouseStocks(mWarehouse.warehouseId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });
        productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mStockDetails = warehouseStocks.get(position);
                currRemainingStocks = mStockDetails.warehouseStock.quantity;
                tvRemainingStocks.setText(String.valueOf(currRemainingStocks));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });
        btnMinus.setOnClickListener(v -> decrementQuantity());
        btnPlus.setOnClickListener(v -> incrementQuantity());
        btnCancel.setOnClickListener(v -> {
            etQuantity.setText(String.valueOf(1));
            addDialog.dismiss();
        });
        btnSave.setOnClickListener(v -> addDeliveryOrder());

        addDialog = dialogBuilder.setView(view).create();
    }

    private void loadWarehouseStocks(int warehouseId) {
        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching WarehouseStockDetails entry.");
            return getDatabase().warehouseStocks().getWarehouseStocks(warehouseId);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            showProgress(false);
            Log.d(TAG, "Returned with list size=" + list.size());
            warehouseStocks = list;
            productSpinner.setAdapter(new WarehouseStockDetailSpinnerAdapter(this, warehouseStocks));
            noProducts.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            showProgress(false);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching WarehouseStockDetail entries: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    private void incrementQuantity() {
        String str = etQuantity.getText().toString().trim();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str);
        qty += 1;
        if (qty > currRemainingStocks) qty = currRemainingStocks;
        etQuantity.setText(String.valueOf(qty));
    }

    private void decrementQuantity() {
        String str = etQuantity.getText().toString().trim();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str);
        qty -= 1;
        if (qty <= 0) qty = 1;
        etQuantity.setText(String.valueOf(qty));
    }

    private void addDeliveryOrder() {
        if (mWarehouse == null || mStockDetails == null) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("No selected Warehouse or Warehouse Stock")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        DeliveryOrderItem item = new DeliveryOrderItem();
        item.fkDeliveryOrderId = mDeliveryDetails.deliveryOrder.deliveryOrderId;
        item.fkProductId = mStockDetails.product.productId;
        item.fkWarehouseStockId = mStockDetails.warehouseStock.warehouseStockId;
        String str = etQuantity.getText().toString().trim();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str);
        item.quantity = qty;
        item.subtotal = mStockDetails.product.price * qty;

        showProgress(true);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving DeliveryOrderItem entry");
            return getDatabase().deliveryOrderItems().insert(item);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            showProgress(false);
            Log.d(TAG, "Returned with id=" + id);
            addDialog.dismiss();

            DeliveryItemDetails details = new DeliveryItemDetails();
            details.deliveryOrderItem = item;
            details.product = mStockDetails.product;
            deliveryItemList.add(details);
            adapter.replaceAll(deliveryItemList);
            calculateTotal(deliveryItemList);
        }, err -> {
            showProgress(false);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving Delivery Order Item: " + err)
                    .setPositiveButton("OK", (d, w) -> d.dismiss());
            dialogBuilder.create().show();
        }));
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    private void showProgress(boolean show) {
        progressGroup.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private AppDatabase getDatabase() {
        if (database == null) database = AppDatabaseImpl.getDatabase(getApplicationContext());
        return database;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
