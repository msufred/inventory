package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.OrderListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Order;
import io.zak.inventory.data.relations.OrderDetails;

public class OrdersFragment extends Fragment implements OrderListAdapter.OnItemClickListener {

    private static final String TAG = "Orders";

    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView emptyOrders;
    private Button btnScan;
    private RelativeLayout progressGroup;

    // Dialog Widgets
    private TextView tvOrNo, tvVehicle, tvEmployee, tvConsumer, tvAddress, tvContact, tvDate, tvAmount;
    private Button btnCancel, btnSave;
    private AlertDialog resultDialog;

    private OrderListAdapter adapter;
    private List<OrderDetails> orderDetailsList;

    // Scan Result Variables
    private int orderId;
    private String orNo, consumerName, consumerAddress, consumerContact;
    private int vehicleId, employeeId;
    private long dateOrdered;
    private double totalAmount;

    // sort orders by date
    private final Comparator<OrderDetails> comparator = Comparator.comparing(orderDetails -> orderDetails.order.dateOrdered);

    private ActivityResultLauncher<ScanOptions> qrCodeLauncher;

    private ScanOptions scanOptions;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);
        getWidgets(view);
        setListeners();

        // register qrCodeLauncher
        qrCodeLauncher  = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                processScanResult(result.getContents());
            }
        });

        return view;
    }

    private void getWidgets(View view) {
        searchView = view.findViewById(R.id.search_view);
        recyclerView = view.findViewById(R.id.recycler_view);
        emptyOrders = view.findViewById(R.id.tv_no_orders);
        btnScan = view.findViewById(R.id.btn_scan);
        progressGroup = view.findViewById(R.id.progress_group);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new OrderListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        // dialog widgets
        createResultDialog();

        dialogBuilder = new AlertDialog.Builder(requireActivity());
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
        btnScan.setOnClickListener(v -> scanQrCode());

        // scan result dialog
        btnCancel.setOnClickListener(v -> resultDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            if (validated()) {
                if (hasDuplicate(orderId)) {
                    resultDialog.dismiss();
                    dialogBuilder.setTitle("Invalid").setMessage("Duplicate Order").setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                    dialogBuilder.create().show();
                } else {
                    resultDialog.dismiss();
                    saveResult();
                }
            } else {
                dialogBuilder.setTitle("Invalid Action").setMessage("Incomplete Data/Invalid QR Code")
                        .setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
                dialogBuilder.create().show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        refresh();
    }

    private void refresh() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching order entries.");
            return AppDatabaseImpl.getDatabase(getActivity()).orders().getOrdersWithDetails();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orders -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + orders.size());
            orderDetailsList = orders;
            adapter.replaceAll(orderDetailsList);
            emptyOrders.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while retrieving Order entries: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                    });
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        OrderDetails orderDetails = adapter.getItem(position);
        if (orderDetails != null) {
            Intent intent = new Intent(getActivity(), ViewOrderActivity.class);
            intent.putExtra("order_id", orderDetails.order.orderId);
            startActivity(intent);
        }
    }

    private void onSearch(String query) {
        List<OrderDetails> filteredList = filter(orderDetailsList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<OrderDetails> filter(List<OrderDetails> orderDetailsList, String query) {
        List<OrderDetails> list = new ArrayList<>();
        String str = query.toLowerCase();
        for (OrderDetails details : orderDetailsList) {
            if (details.vehicle.vehicleName.toLowerCase().contains(str) ||
                details.employee.employeeName.toLowerCase().contains(str) ||
                details.order.consumerName.toLowerCase().contains(str)) {
                list.add(details);
            }
        }
        return list;
    }

    private void scanQrCode() {
        qrCodeLauncher.launch(getScanOptions());
    }

    private void processScanResult(String qrCodeResult) {
        if (qrCodeResult.contains("order")) {
            resetScanResultValues();

            // Break down text (use ; as delimiter)
            String[] strArr = qrCodeResult.split(";");
            for (String str : strArr) {
                String[] arr = str.split("=");
                String key = arr[0];
                String value = arr[1];

                switch (key) {
                    case "order": orderId = Integer.parseInt(value); break;
                    case "orno": orNo = value; break;
                    case "vehicle": vehicleId = Integer.parseInt(value.trim()); break;
                    case "employee": employeeId = Integer.parseInt(value.trim()); break;
                    case "consumer": consumerName = value; break;
                    case "address": consumerAddress = value; break;
                    case "contact": consumerContact = value; break;
                    case "date": dateOrdered = Long.parseLong(value.trim()); break;
                    case "total": totalAmount = Double.parseDouble(value.trim()); break;
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

            resultDialog.show();
        }
    }

    private ScanOptions getScanOptions() {
        if (scanOptions == null) scanOptions = new ScanOptions();
        scanOptions.setCaptureActivity(PortraitCaptureActivity.class);
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setCameraId(0);
        scanOptions.setPrompt("Scan QR Code");
        return scanOptions;
    }

    private void createResultDialog() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_scan_order_result, null);

        tvOrNo = view.findViewById(R.id.tv_orno);
        tvVehicle = view.findViewById(R.id.tv_vehicle);
        tvEmployee = view.findViewById(R.id.tv_employee);
        tvConsumer = view.findViewById(R.id.tv_consumer);
        tvAddress = view.findViewById(R.id.tv_address);
        tvContact = view.findViewById(R.id.tv_contact);
        tvDate = view.findViewById(R.id.tv_date_ordered);
        tvAmount = view.findViewById(R.id.tv_total_amount);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnSave = view.findViewById(R.id.btn_save);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        resultDialog = builder.setView(view).create();
    }

    private void resetScanResultValues() {
        orderId = -1;
        orNo = consumerName = consumerAddress = consumerContact = "";
        vehicleId = employeeId = -1;
        dateOrdered = 0;
        totalAmount = 0;
    }

    private boolean validated() {
        return orderId != -1 && !orNo.isBlank() && vehicleId != -1 && employeeId != -1 && !consumerName.isBlank()
                && dateOrdered != 0;
    }

    private void saveResult() {
        Order order = new Order();
        order.orderId = orderId;
        order.orNo = orNo;
        order.fkVehicleId = vehicleId;
        order.fkEmployeeId = employeeId;
        order.consumerName = consumerName;
        order.consumerAddress = consumerAddress;
        order.consumerContact = consumerContact;
        order.dateOrdered = dateOrdered;
        order.totalAmount = totalAmount;
        order.orderStatus = "Processing";

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Order entry.");
            return AppDatabaseImpl.getDatabase(requireActivity().getApplicationContext()).orders().insert(order);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with id=" + id.intValue());
            refresh();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while adding Order entry: " + err)
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                    });
            dialogBuilder.create().show();
        }));
    }

    private boolean hasDuplicate(int id) {
        for (OrderDetails details : orderDetailsList) {
            if (details.order.orderId == id) return true;
        }
        return false;
    }
}
