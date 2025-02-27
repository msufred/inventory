package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.WarehouseStockListAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Warehouse;
import io.zak.inventory.data.relations.WarehouseStockDetails;

public class WarehouseStocksActivity extends AppCompatActivity implements WarehouseStockListAdapter.OnItemClickListener {

    private static final String TAG = "WarehouseStocks";

    // Widgets
    private SearchView searchView;
    private TextView tvName, tvStocksCount, tvStocksAmount, tvNoStocks;
    private RecyclerView recyclerView;
    private ImageButton btnClose, btnEdit;
    private Button btnAddStock;
    private RelativeLayout progressGroup;

    private WarehouseStockListAdapter adapter;
    private List<WarehouseStockDetails> stockDetailsList;
    private final Comparator<WarehouseStockDetails> comparator = Comparator.comparing(stockDetails -> stockDetails.product.productName);

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Warehouse mWarehouse;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouse_stocks);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        tvName = findViewById(R.id.tv_name);
        tvStocksCount = findViewById(R.id.tv_stocks_count);
        tvStocksAmount = findViewById(R.id.tv_stocks_amount);
        tvNoStocks = findViewById(R.id.tv_no_stocks);
        recyclerView = findViewById(R.id.recycler_view);
        btnClose = findViewById(R.id.btn_close);
        btnEdit = findViewById(R.id.btn_edit);
        btnAddStock = findViewById(R.id.btn_add_stock);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WarehouseStockListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
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

        btnClose.setOnClickListener(v -> goBack());

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditWarehouseActivity.class);
            intent.putExtra("warehouse_id", mWarehouse.warehouseId);
            startActivity(intent);
        });

        btnAddStock.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWarehouseStockActivity.class);
            intent.putExtra("warehouse_id", mWarehouse.warehouseId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check if Intent has warehouse_id extra; return if none
        int warehouseId = getIntent().getIntExtra("warehouse_id", -1);
        if (warehouseId == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Unknown Warehouse ID")
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }

        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        disposables.add(Single.fromCallable(() -> {
            // get warehouse entry
            Log.d(TAG, "Fetching Warehouse entry: " + Thread.currentThread());
            return database.warehouses().getWarehouse(warehouseId);
        }).flatMap(warehouseList -> {
            Log.d(TAG, "Returned with list size " + warehouseList.size() + " " + Thread.currentThread());
            mWarehouse = warehouseList.get(0); // always get first item
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching WarehouseStockDetail entries: " + Thread.currentThread());
                return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouseStocks().getWarehouseStocks(warehouseId);
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with list size " + list.size() + " " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);

            // populate list
            stockDetailsList = list;
            adapter.replaceAll(list);
            tvNoStocks.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);

            // set warehouse name
            tvName.setText(mWarehouse.warehouseName);

            // compute and display stocks
            tvStocksCount.setText(String.valueOf(list.size()));
            double amount = 0.0;
            for (WarehouseStockDetails details : list) {
                amount += details.warehouseStock.totalAmount;
            }
            tvStocksAmount.setText(Utils.toStringMoneyFormat(amount));
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching WarehouseStockDetail entries: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            WarehouseStockDetails stockDetails = adapter.getItem(position);
            if (stockDetails != null) {
                Log.d(TAG, "Selected " + stockDetails.product.productName);
                // TODO
            }
        }
    }

    private void onSearch(String query) {
        List<WarehouseStockDetails> filteredList = filter(stockDetailsList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<WarehouseStockDetails> filter(List<WarehouseStockDetails> detailsList, String query) {
        String str = query.toLowerCase();
        List<WarehouseStockDetails> list = new ArrayList<>();
        for (WarehouseStockDetails details : detailsList) {
            if (details.product.productName.toLowerCase().contains(str)) {
                list.add(details);
            }
        }
        return list;
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        mWarehouse = null;
        disposables.dispose();
    }
}
