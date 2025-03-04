package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.OrderItemListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.relations.OrderItemDetails;

public class ViewOrderActivity extends AppCompatActivity implements OrderItemListAdapter.OnItemClickListener {

    private static final String TAG = "ViewOrder";

    // Widgets
    private TextView tvItemsCount, tvTotalAmount, emptyOrderItems;
    private RecyclerView recyclerView;
    private Button btnAddItem, btnConfirm;
    private ImageButton btnBack;
    private RelativeLayout progressGroup;

    private OrderItemListAdapter adapter;
    private List<OrderItemDetails> orderItemDetailsList;

    // sort by product name
    private final Comparator<OrderItemDetails> comparator = Comparator.comparing(orderItem -> orderItem.product.productName);

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    // for QR Code scanning
    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            processScanResult(result.getContents());
        } else {
            dialogBuilder.setTitle("Error").setMessage("Invalid QR Code.").setPositiveButton("Dismiss", (dialog, which) -> {
                dialog.dismiss();
            });
            dialogBuilder.create().show();

        }
    });

    private ScanOptions scanOptions;

    // scan result variables
    private int orderItemId;
    private int orderId;
    private int productId;
    private double sellingPrice;
    private int quantity;
    private double subtotal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_order);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        tvItemsCount = findViewById(R.id.tv_item_count);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        emptyOrderItems = findViewById(R.id.empty_order_items);
        recyclerView = findViewById(R.id.recycler_view);
        btnAddItem = findViewById(R.id.btn_add_item);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnBack = findViewById(R.id.btn_back);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnAddItem.setOnClickListener(v -> scanQrCode());
        btnConfirm.setOnClickListener(v -> confirmOrder());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> AppDatabaseImpl.getDatabase(getApplicationContext()).orderItems().orderItemsWithDetails())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(list -> {
                    progressGroup.setVisibility(View.GONE);
                    orderItemDetailsList = list;
                    adapter.replaceAll(orderItemDetailsList);
                    emptyOrderItems.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
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
        // TODO
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
            tvOrNo.setText(orNo);
            tvVehicle.setText(String.valueOf(vehicleId));
            tvEmployee.setText(String.valueOf(employeeId));
            tvConsumer.setText(consumerName);
            tvAddress.setText(consumerAddress);
            tvContact.setText(consumerContact);
            tvDate.setText(Utils.dateFormat.format(new Date(dateOrdered)));
            tvAmount.setText(Utils.toStringMoneyFormat(totalAmount));

            resultDailog.show();
        } else {
            dialogBuilder.setTitle("Invalid QR Code").setMessage("Can't process QR Code.").setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }
    }

    private void resetScanResultValues() {

    }

    private void confirmOrder() {
        // TODO
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
