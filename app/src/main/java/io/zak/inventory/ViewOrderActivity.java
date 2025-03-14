package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.OrderItemListAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Order;
import io.zak.inventory.data.entities.OrderItem;
import io.zak.inventory.data.entities.WarehouseStock;
import io.zak.inventory.data.relations.OrderItemDetails;
import io.zak.inventory.firebase.OrderEntry;
import io.zak.inventory.firebase.OrderItemEntry;

public class ViewOrderActivity extends AppCompatActivity implements OrderItemListAdapter.OnItemClickListener {

    private static final String TAG = "ViewOrder";

    // Widgets
    private TextView tvItemsCount, tvTotalAmount, emptyOrderItems;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private Button btnAddItem, btnConfirm;
    private ImageButton btnBack;
    private RelativeLayout progressGroup;

    // Scan Result Dialog Widgets
    private TextView tvItemId, tvOrderId, tvProductId, tvSellingPrice, tvQuantity, tvSubtotal;
    private Button btnCancel, btnSave;

    // for RecyclerView
    private OrderItemListAdapter adapter;
    private List<OrderItemDetails> orderItemDetailsList; // reference for search filter

    // sort by product name
    private final Comparator<OrderItemDetails> comparator = Comparator.comparing(orderItem -> orderItem.product.productName);

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog resultDialog;

    // for QR Code scanning
    private ActivityResultLauncher<ScanOptions> qrCodeLauncher;

    private ScanOptions scanOptions;

    // scan result variables
    private int orderItemId;
    private int orderId;
    private int productId;
    private double sellingPrice;
    private int quantity;
    private double subtotal;

    // current Order
    private Order mOrder;

    private DatabaseReference mDatabase;
    private DatabaseReference mItemsRef;
    private OrderEntry mOrderEntry;
    private List<OrderItemEntry> mOrderItemList;

    private final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            progressGroup.setVisibility(View.VISIBLE);
            mOrderItemList = new ArrayList<>();
            for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                OrderItemEntry entry = postSnapshot.getValue(OrderItemEntry.class);
                if (entry != null) mOrderItemList.add(entry);
            }
            processItems(mOrderItemList);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled", error.toException());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_order);
        getWidgets();
        setListeners();

        // register qrCodeLauncher
        qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                processScanResult(result.getContents());
            } else {
                dialogBuilder.setTitle("Error")
                        .setMessage("Invalid QR Code.")
                        .setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
                dialogBuilder.create().show();
            }
        });
    }

    private void getWidgets() {
        tvItemsCount = findViewById(R.id.tv_item_count);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        emptyOrderItems = findViewById(R.id.empty_order_items);
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        btnAddItem = findViewById(R.id.btn_add_item);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnBack = findViewById(R.id.btn_back);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);

        createResultDialog();
    }

    private void createResultDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_scan_order_item_result, null);

        tvItemId = view.findViewById(R.id.tv_item_id);
        tvOrderId = view.findViewById(R.id.tv_order_id);
        tvProductId = view.findViewById(R.id.tv_product_id);
        tvSellingPrice = view.findViewById(R.id.tv_price);
        tvQuantity = view.findViewById(R.id.tv_quantity);
        tvSubtotal = view.findViewById(R.id.tv_subtotal);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSave = view.findViewById(R.id.btn_save);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        resultDialog = builder.setView(view).create();
    }

    private void setListeners() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onSearch(newText);
                return false;
            }
        });
        btnBack.setOnClickListener(v -> goBack());
        btnAddItem.setOnClickListener(v -> scanQrCode());
        btnConfirm.setOnClickListener(v -> {
            dialogBuilder.setTitle("Confirm")
                    .setMessage("Once the Order is completed, you can't add/scan more items. " +
                            "Do you want to proceed?")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        dialog.dismiss();
                        confirmOrder();
                    });
            dialogBuilder.create().show();
        });

        // Result Dialog
        btnCancel.setOnClickListener(v -> resultDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            if (validated()) {
                if (hasDuplicate(orderItemId)) {
                    resultDialog.dismiss();
                    dialogBuilder.setTitle("Invalid").setMessage("Duplicate Order Item")
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                    dialogBuilder.create().show();
                } else {
                    resultDialog.dismiss();
                    saveResult();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

        // get order id
        int id = getIntent().getIntExtra("order_id", -1);
        boolean fetch = getIntent().getBooleanExtra("fetch", false);

        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action").setMessage("Invalid Order ID: " + id)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        // get OrderEntry
        progressGroup.setVisibility(View.VISIBLE);
        Log.d(TAG, "Fetch Order entry with id=" + id);
        mDatabase.child("orders").child(String.valueOf(id)).get()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        mOrderEntry = task.getResult().getValue(OrderEntry.class);
                        if (mOrderEntry != null) {
                            Log.d(TAG, "Success! Fetching order items...");
                            if (mOrderEntry.status.equals("Processing")) {
                                // TODO show action buttons (Process Button)
                                // Process button will iterated all items, and subtract quantity
                                // from Warehouse stocks
                            }
                        }
                    } else {
                        Log.w(TAG, "failed to fetch order", task.getException());
                        dialogBuilder.setTitle("Error")
                                .setMessage("Order entry not found!")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                        dialogBuilder.create().show();
                    }
                });

        // get items
        if (fetch && mItemsRef == null) {
            mItemsRef = mDatabase.child("orders").child(String.valueOf(id)).child("items");
            mItemsRef.addValueEventListener(valueEventListener);
        } else {
            if (mItemsRef != null) mItemsRef.removeEventListener(valueEventListener);
            refresh();
        }

        // order id is not -1, get Order entry
//        progressGroup.setVisibility(View.VISIBLE);
//        disposables.add(Single.fromCallable(() -> {
//            Log.d(TAG, "Retrieving Order entry with ID=" + id);
//            return AppDatabaseImpl.getDatabase(getApplicationContext()).orders().getOrder(id);
//        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
//            progressGroup.setVisibility(View.GONE);
//            Log.d(TAG, "Returned with list size=" + orders.size());
//            mOrder = orders.get(0);
//            if (mOrder != null) {
//                tvTotalAmount.setText(Utils.toStringMoneyFormat(mOrder.totalAmount));
//                refresh();
//            }
//        }));
    }

    /*
     * Add OrderItem entry to local database.
     */
    private void processItems(List<OrderItemEntry> orderItemEntries) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            int count = 0;
            AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
            for (OrderItemEntry entry : orderItemEntries) {
                OrderItem item = new OrderItem();
                item.orderItemId = entry.id;
                item.fkOrderId = entry.orderId;
                item.fkWarehouseStockId = entry.warehouseStockId;
                item.fkProductId = entry.productId;
                item.sellingPrice = entry.sellingPrice;
                item.quantity = entry.quantity;
                item.subtotal = entry.subtotal;
                long id = database.orderItems().insert(item);
                if (id != -1) count += 1;
            }
            return count;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Added " + rowCount + " rows.");
            refresh();
        }));
    }

    private void refresh() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> AppDatabaseImpl.getDatabase(getApplicationContext()).orderItems().orderItemsWithDetails())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    progressGroup.setVisibility(View.GONE);
                    orderItemDetailsList = list;
                    adapter.replaceAll(orderItemDetailsList);
                    emptyOrderItems.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    tvItemsCount.setText(String.valueOf(orderItemDetailsList.size()));
                }, err -> {
                    progressGroup.setVisibility(View.GONE);
                    Log.e(TAG, "Database Error: " + err);
                    dialogBuilder.setTitle("Database Error")
                            .setMessage("Error while fetching Order Item entries: " + err)
                            .setPositiveButton("Dismiss", (dialog, which) -> {
                                dialog.dismiss();
                            });
                    dialogBuilder.create().show();
                }));
    }

    @Override
    public void onItemClick(int position) {
        // TODO view OrderItem details (edit/delete)
    }

    private void onSearch(String query) {
        List<OrderItemDetails> filteredList = filter(orderItemDetailsList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<OrderItemDetails> filter(List<OrderItemDetails> orderItemList, String query) {
        List<OrderItemDetails> list = new ArrayList<>();
        String str = query.toLowerCase();
        for (OrderItemDetails details : orderItemList) {
            if (details.product.productName.toLowerCase().contains(str)) {
                list.add(details);
            }
        }
        return list;
    }

    private void scanQrCode() {
        qrCodeLauncher.launch(getScanOptions());
    }

    private ScanOptions getScanOptions() {
        if (scanOptions == null) scanOptions = new ScanOptions();
        scanOptions.setCaptureActivity(PortraitCaptureActivity.class);
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setCameraId(0);
        scanOptions.setPrompt("Scan QR Code");
        return scanOptions;
    }

    private void processScanResult(String qrCodeResult) {
        if (qrCodeResult.contains("item")) {
            resetScanResultValues();

            // Break down text (use ; as delimiter)
            String[] strArr = qrCodeResult.split(";");
            for (String str : strArr) {
                String[] arr = str.split("=");
                String key = arr[0];
                String value = arr[1];

                switch (key) {
                    case "item": orderItemId = Integer.parseInt(value.trim()); break;
                    case "order": orderId = Integer.parseInt(value.trim()); break;
                    case "product": productId = Integer.parseInt(value.trim()); break;
                    case "price": sellingPrice = Double.parseDouble(value.trim()); break;
                    case "qty": quantity = Integer.parseInt(value.trim()); break;
                    case "total": subtotal = Double.parseDouble(value.trim()); break;
                    default:
                }
            }

            // display
            tvItemId.setText(String.valueOf(orderItemId));
            tvOrderId.setText(String.valueOf(orderId));
            tvProductId.setText(String.valueOf(productId));
            tvSellingPrice.setText(Utils.toStringMoneyFormat(sellingPrice));
            tvQuantity.setText(String.valueOf(quantity));
            tvSubtotal.setText(Utils.toStringMoneyFormat(subtotal));

            resultDialog.show();
        } else {
            dialogBuilder.setTitle("Invalid QR Code")
                    .setMessage("Can't process QR Code.")
                    .setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }
    }

    private void resetScanResultValues() {
        orderItemId = -1;
        orderId = -1;
        productId = -1;
        sellingPrice = 0;
        quantity = 0;
        subtotal = 0;
    }

    private boolean validated() {
        return orderItemId != -1 && orderId != -1 && productId != -1;
    }

    private boolean hasDuplicate(int id) {
        for (OrderItemDetails itemDetails : orderItemDetailsList) {
            if (itemDetails.orderItem.orderItemId == id) return true;
        }
        return false;
    }

    private void saveResult() {
        OrderItem item = new OrderItem();
        item.orderItemId = orderItemId;
        item.fkOrderId = orderId;
        item.fkProductId = productId;
        item.sellingPrice = sellingPrice;
        item.quantity = quantity;
        item.subtotal = subtotal;

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving OrderItem entry.");
            return AppDatabaseImpl.getDatabase(getApplicationContext()).orderItems().insert(item);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with id=" + id.intValue());
            refresh();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving Order Item: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    private void confirmOrder() {
        if (mOrder == null) return; // just making sure

        // Steps:
        // 1) Update OrderItem status to "Complete"
        // 2) if OrderItem is updated successfully, iterate the OrderItemDetails list
        //      a) for each OrderItemDetails, get the WarehouseStock entry
        //      b) subtract the OrderItem's quantity from WarehouseStock's quantity
        //      c) subtract the OrderItem's quantity from WarehouseStock's takenOut
        //      d) update WarehouseStock

        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        mOrder.orderStatus = "Completed";
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating OrderItem status to \"Completed\"");
            return database.orders().update(mOrder);
        }).flatMap(rowCount -> {
            Log.d(TAG, "Returned with row count = " + rowCount);
            Log.d(TAG, "Updating WarehouseStocks: " + Thread.currentThread());
            if (rowCount > 0) { // updated successfully
                for (OrderItemDetails details : orderItemDetailsList) {
                    // a) get WarehouseStock entry
                    List<WarehouseStock> stocks = database.warehouseStocks()
                            .getWarehouseStock(details.orderItem.fkWarehouseStockId);
                    if (!stocks.isEmpty()) {
                        WarehouseStock stock = stocks.get(0);
                        // b) subtract from quantity
                        stock.quantity = stock.quantity - details.orderItem.quantity;
                        // c) subtract from takenOut
                        stock.takenOut = stock.takenOut - details.orderItem.quantity;
                        // d) update WarehouseStock
                        int r = database.warehouseStocks().update(stock);
                    }
                }
            }
            return Single.just(rowCount);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with row count = " + rowCount);
            if (rowCount > 0) Toast.makeText(this, "Order Completed", Toast.LENGTH_SHORT).show();
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while updating Order entry: " + err)
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
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
