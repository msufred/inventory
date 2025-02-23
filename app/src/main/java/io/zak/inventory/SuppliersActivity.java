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
import io.zak.inventory.adapters.SupplierListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Supplier;

public class SuppliersActivity extends AppCompatActivity implements SupplierListAdapter.OnItemClickListener {

    private static final String TAG = "Suppliers";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private Button btnBack, btnAdd;

    // RecyclerView adapter
    private SupplierListAdapter adapter;

    // list reference for search filter
    private List<Supplier> supplierList;

    // Comparator used for search filter; passed to SupplierListAdapter
    private final Comparator<Supplier> comparator = Comparator.comparing(supplier -> supplier.name);

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suppliers);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SupplierListAdapter(comparator, this);
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

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddSupplierActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Supplier entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).suppliers().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
            supplierList = list;
            adapter.replaceAll(list);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Supplier supplier = adapter.getItem(position);
            if (supplier != null) {
                Log.d(TAG, "Supplier selected: " + supplier.name);
                // TODO
            }
        }
    }

    private void onSearch(String query) {
        List<Supplier> filteredList = filter(supplierList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Supplier> filter(List<Supplier> suppliers, String query) {
        String str = query.toLowerCase();
        List<Supplier> list = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            if (supplier.name.toLowerCase().contains(str)) {
                list.add(supplier);
            }
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
