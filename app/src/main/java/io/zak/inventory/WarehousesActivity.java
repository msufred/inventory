package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.WarehouseListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Warehouse;

public class WarehousesActivity extends AppCompatActivity implements WarehouseListAdapter.OnItemClickListener {

    private static final String TAG = "Warehouses";

    private SearchView searchView;
    private RecyclerView recyclerView;
    private Button btnBack, btnAdd;

    // for RecyclerView
    private WarehouseListAdapter adapter;

    // list reference for search filtering
    private List<Warehouse> warehouseList;

    // used for search filter
    private final Comparator<Warehouse> comparator = Comparator.comparing(o -> o.name);

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouses);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);

        // set up RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WarehouseListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);
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

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddWarehouseActivity.class));
        });
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Warehouse warehouse = adapter.getItem(position);
            if (warehouse != null) {
                Intent intent = new Intent(this, WarehouseStocksActivity.class);
                intent.putExtra("warehouse_id", warehouse.id);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Warehouse items: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).warehouses().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
            warehouseList = list;
            adapter.replaceAll(list);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
        }));
    }

    private void onSearch(String query) {
        final List<Warehouse> filteredList = filter(warehouseList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Warehouse> filter(List<Warehouse> ref, String query) {
        String str = query.toLowerCase();
        final List<Warehouse> list = new ArrayList<>();
        for (Warehouse warehouse : ref) {
            if (warehouse.name.toLowerCase().contains(str)) {
                list.add(warehouse);
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
        Log.d(TAG, "Destroying resources");
        disposables.dispose();
    }
}
