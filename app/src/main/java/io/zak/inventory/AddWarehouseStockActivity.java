package io.zak.inventory;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.ProductSpinnerAdapter;
import io.zak.inventory.adapters.SupplierSpinnerAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.entities.Supplier;
import io.zak.inventory.data.entities.WarehouseStock;

public class AddWarehouseStockActivity extends AppCompatActivity {

    private static final String TAG = "AddWarehouseStock";

    // Widgets
    private Spinner supplierSpinner, productSpinner;
    private TextView emptySupplierSpinner, emptyProductSpinner; // if spinners are empty, show these
    private EditText etQuantity, etDate;
    private ImageButton btnBack, btnMinus, btnPlus, btnPickDate;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    // list reference
    private List<Supplier> supplierList;
    private List<Product> productList;

    // selected items
    private Supplier mSupplier;
    private Product mProduct;
    private Date mDateAcquired;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;
    private DatePickerDialog datePickerDialog;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_warehouse_stock);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        supplierSpinner = findViewById(R.id.supplier_spinner);
        emptySupplierSpinner = findViewById(R.id.empty_supplier_spinner);
        productSpinner = findViewById(R.id.product_spinner);
        emptyProductSpinner = findViewById(R.id.empty_products_spinner);
        etQuantity = findViewById(R.id.et_quantity);
        etDate = findViewById(R.id.et_date);
        btnBack = findViewById(R.id.btn_back);
        btnMinus = findViewById(R.id.btn_minus);
        btnPlus = findViewById(R.id.btn_plus);
        btnPickDate = findViewById(R.id.btn_pick_date);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        dialogBuilder = new AlertDialog.Builder(this);
        datePickerDialog = new DatePickerDialog(this);
        datePickerDialog.setOnDateSetListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            mDateAcquired = calendar.getTime();
            etDate.setText(dateFormat.format(mDateAcquired));
        });

        progressGroup = findViewById(R.id.progress_group);
    }

    private void setListeners() {
        supplierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (supplierList != null && !supplierList.isEmpty()) {
                    mSupplier = supplierList.get(position);
                    loadProducts(mSupplier.id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (productList != null && !productList.isEmpty()) {
                    mProduct = productList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        btnMinus.setOnClickListener(v -> decrementQuantity());
        btnPlus.setOnClickListener(v -> incrementQuantity());
        etDate.setOnClickListener(v -> datePickerDialog.show());
        btnPickDate.setOnClickListener(v -> datePickerDialog.show());
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Supplier entries: " + Thread.currentThread());
            return database.suppliers().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(suppliers -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size " + suppliers.size() + " " + Thread.currentThread());
            supplierList = suppliers;
            supplierSpinner.setAdapter(new SupplierSpinnerAdapter(this, supplierList));
            emptySupplierSpinner.setVisibility(suppliers.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Supplier entries: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }));
    }

    private void loadProducts(int id) {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Product entries with supplierId=" + id + " " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).products().getProductsFromSupplier(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(products -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + products.size() + " " + Thread.currentThread());
            productList = products;
            productSpinner.setAdapter(new ProductSpinnerAdapter(this, productList));
            emptyProductSpinner.setVisibility(products.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Database Error: " + err);

            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Products from Supplier with ID=" + id + ": " + err)
                    .setPositiveButton("OK", ((dialog, which) -> dialog.dismiss()));
            dialogBuilder.create().show();
        }));
    }

    private void decrementQuantity() {
        String str = etQuantity.getText().toString();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str.trim());
        qty -= 1;
        if (qty < 1) qty = 1;
        etQuantity.setText(String.valueOf(qty));
    }

    private void incrementQuantity() {
        String str = etQuantity.getText().toString();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str.trim());
        qty += 1;
        etQuantity.setText(String.valueOf(qty));
    }

    private boolean validated() {
        return mProduct != null;
    }

    private void saveAndClose() {
        // check intent
        int warehouseId = getIntent().getIntExtra("warehouse_id", -1);

        // if no warehouse_id passed to this activity, prompt user
        if (warehouseId == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Warehouse ID")
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
            return;
        }

        WarehouseStock stock = new WarehouseStock();
        stock.warehouseId = warehouseId;
        stock.productId = mProduct.id;
        String str = etQuantity.getText().toString().trim();
        int qty = str.isBlank() ? 1 : Integer.parseInt(str);
        if (qty < 1) qty = 1;
        stock.quantity = qty;
        stock.takenOut = 0;
        stock.dateAcquired = mDateAcquired.getTime();
        stock.totalAmount = (qty * mProduct.price);

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving WarehouseStock entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouseStocks().insert(stock);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with ID " + id + " " + Thread.currentThread());
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving WarehouseStock entry: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
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
