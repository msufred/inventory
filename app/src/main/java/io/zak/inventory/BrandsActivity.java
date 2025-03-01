package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.BrandListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Brand;

public class BrandsActivity extends AppCompatActivity implements BrandListAdapter.OnItemClickListener {

    private static final String TAG = "Brands";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoBrands;
    private ImageButton btnBack;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    // for RecyclerView
    private BrandListAdapter adapter;

    // list reference for search filter
    private List<Brand> brandList;

    // comparator for search filter
    private final Comparator<Brand> comparator = Comparator.comparing(brand -> brand.brandName);

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog addDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brands);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoBrands = findViewById(R.id.tv_no_brands);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BrandListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        // create dialog builder
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

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            finish();
        });

        btnAdd.setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Brand entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).brands().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with size=" + list.size() + " " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            brandList = list;
            adapter.replaceAll(list);
            tvNoBrands.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
            // error dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Brand entries: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Brand brand = adapter.getItem(position);
            if (brand != null) {
                Log.d(TAG, "Brand selected: " + brand.brandName);
                // TODO show details or options for delete and edit
            }
        }
    }


    private void onSearch(String query) {
        List<Brand> filteredList = filter(brandList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Brand> filter(List<Brand> brandList, String query) {
        String str = query.toLowerCase();
        List<Brand> list = new ArrayList<>();
        for (Brand brand : brandList) {
            if (brand.brandName.toLowerCase().contains(str)) {
                list.add(brand);
            }
        }
        return list;
    }

    private void showAddDialog() {
        if (addDialog == null) createDialog();
        addDialog.show();
    }

    private void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_brand, null);

        EditText etName = dialogView.findViewById(R.id.et_brand_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        dialogBuilder.setView(dialogView);
        addDialog = dialogBuilder.create();

        btnCancel.setOnClickListener(v -> addDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String str = etName.getText().toString();
            if (!str.isBlank()) addBrand(str);
            etName.getText().clear();
            addDialog.dismiss();
        });
    }

    private void addBrand(String brandName) {
        Brand brand = new Brand();
        brand.brandName = brandName;

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Brand entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).brands().insert(brand);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Done. Returned with ID=" + id + " " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            brand.brandId = id.intValue();
            adapter.addItem(brand);
            if (tvNoBrands.getVisibility() == View.VISIBLE) tvNoBrands.setVisibility(View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving Brand entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
