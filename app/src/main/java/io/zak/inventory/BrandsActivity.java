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

public class BrandsActivity extends AppCompatActivity implements BrandListAdapter.OnItemClickListener, BrandListAdapter.OnItemLongClickListener {

    private static final String TAG = "Brands";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoBrands;
    private Button btnAdd;
    private ImageButton btnBack;
    private RelativeLayout progressGroup;
    private Button btnDelete;
    private TextView titleTextView;

    // for RecyclerView
    private BrandListAdapter adapter;

    // list reference for search filter
    private List<Brand> brandList;

    // comparator for search filter
    private final Comparator<Brand> comparator = Comparator.comparing(brand -> brand.brandName);

    private CompositeDisposable disposables;
    private AlertDialog addDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brands);
        getWidgets();
        setListeners();
        loadData();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoBrands = findViewById(R.id.tv_no_brands);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);
        btnDelete = findViewById(R.id.btn_delete);
        progressGroup = findViewById(R.id.progress_group);
        titleTextView = findViewById(R.id.title);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BrandListAdapter(comparator, this, this);
        recyclerView.setAdapter(adapter);

        // Initially hide delete button
        btnDelete.setVisibility(View.GONE);
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
            if (adapter.isInSelectionMode()) {
                exitSelectionMode();
            } else {
                finish();
            }
        });

        btnAdd.setOnClickListener(v -> showAddDialog());

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadData() {
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).brands().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            brandList = list;
            adapter.replaceAll(list);
            tvNoBrands.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);

            // error dialog
            new AlertDialog.Builder(this)
                    .setTitle("Database Error")
                    .setMessage("Error while fetching Brand entries: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }));
    }

    private void showDeleteConfirmationDialog() {
        int count = adapter.getSelectedItemCount();
        // Create a new AlertDialog.Builder each time
        new AlertDialog.Builder(this)
                .setTitle("Delete Selected Items")
                .setMessage("Are you sure you want to delete " + count + " selected item(s)?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteSelectedItems();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteSelectedItems() {
        List<Brand> selectedBrands = adapter.getSelectedBrands();

        disposables.add(Single.fromCallable(() -> {
            int deletedCount = 0;
            for (Brand brand : selectedBrands) {
                deletedCount += AppDatabaseImpl.getDatabase(getApplicationContext()).brands().delete(brand);
            }
            return deletedCount;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(count -> {
            Log.d(TAG, "Deleted " + count + " brands");

            // Remove deleted items from the list
            brandList.removeAll(selectedBrands);
            adapter.replaceAll(brandList);

            // Exit selection mode
            exitSelectionMode();

            // Show empty view if needed
            tvNoBrands.setVisibility(brandList.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        }, err -> {
            Log.e(TAG, "Database Error during deletion: " + err);

            // dialog
            new AlertDialog.Builder(this)
                    .setTitle("Invalid Action")
                    .setMessage("Selected Brand/s are associated with some products. Please delete or edit them first.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            if (adapter.isInSelectionMode()) {
                adapter.toggleSelection(position);
                updateSelectionUI();
            } else {
                Brand brand = adapter.getItem(position);
                if (brand != null) {
                    showAddDialog(brand); // Pass the selected brand for editing
                }
            }
        }
    }

    @Override
    public boolean onItemLongClick(int position) {
        if (!adapter.isInSelectionMode()) {
            adapter.enterSelectionMode();
            adapter.toggleSelection(position);
            updateSelectionUI();
            return true;
        }
        return false;
    }

    private void updateSelectionUI() {
        if (adapter.isInSelectionMode()) {
            btnAdd.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
            int count = adapter.getSelectedItemCount();
            titleTextView.setText(count + " selected");
        } else {
            btnAdd.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.GONE);
            titleTextView.setText(R.string.brands);
        }
    }

    private void exitSelectionMode() {
        adapter.exitSelectionMode();
        updateSelectionUI();
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
        showAddDialog(null); // Pass null for adding a new brand
    }

    private void showAddDialog(Brand brandToEdit) {
        // Create a new LayoutInflater instance each time
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_brand, null);

        TextView tvTitle = dialogView.findViewById(R.id.title);
        EditText etName = dialogView.findViewById(R.id.et_brand_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        tvTitle.setText(brandToEdit == null ? R.string.add_brand : R.string.edit_brand);

        if (brandToEdit != null) {
            etName.setText(brandToEdit.brandName);
        }

        // Create a new AlertDialog.Builder each time
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        addDialog = builder.create();

        btnCancel.setOnClickListener(v -> addDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String str = etName.getText().toString();
            if (!str.isBlank()) {
                if (brandToEdit == null) {
                    // Add new brand
                    addBrand(str);
                } else {
                    // Update existing brand
                    updateBrand(brandToEdit.brandId, str);
                }
            }
            etName.getText().clear();
            addDialog.dismiss();
        });

        addDialog.show();
    }

    private void addBrand(String brandName) {
        Brand brand = new Brand();
        brand.brandName = brandName;

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Brand entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).brands().insert(brand);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            Log.d(TAG, "Done. Returned with ID=" + id + " " + Thread.currentThread());
            brand.brandId = id.intValue();
            adapter.addItem(brand);
            if (tvNoBrands.getVisibility() == View.VISIBLE) tvNoBrands.setVisibility(View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);

            // dialog
            new AlertDialog.Builder(this)
                    .setTitle("Database Error")
                    .setMessage("Error while saving Brand entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }));
    }

    private void updateBrand(int brandId, String brandName) {
        Brand brand = new Brand();
        brand.brandId = brandId;
        brand.brandName = brandName;

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating Brand entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).brands().update(brand);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowsAffected -> {
            Log.d(TAG, "Done. Updated rows=" + rowsAffected + " " + Thread.currentThread());
            adapter.updateItem(brand);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);

            // dialog
            new AlertDialog.Builder(this)
                    .setTitle("Database Error")
                    .setMessage("Error while updating Brand entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        if (disposables != null) {
            disposables.dispose();
        }
    }
}

