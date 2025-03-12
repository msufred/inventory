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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.CategoryListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.firebase.CategoryEntry;

public class CategoriesActivity extends AppCompatActivity implements CategoryListAdapter.OnItemClickListener, CategoryListAdapter.OnItemLongClickListener {

    private static final String TAG = "Categories";

    // Widgets
    private ImageButton btnBack, btnSync;
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoCategories;
    private Button btnAdd;
    private Button btnDelete;
    private TextView titleTextView;
    private RelativeLayout progressGroup;

    // for RecyclerView
    private CategoryListAdapter adapter;

    // list reference for search filter
    private List<Category> categoryList;

    // comparator for search filter
    private final Comparator<Category> comparator = Comparator.comparing(category -> category.categoryName);

    private CompositeDisposable disposables;
    private AlertDialog addDialog;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnBack = findViewById(R.id.btn_back);
        btnSync = findViewById(R.id.btn_sync);
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoCategories = findViewById(R.id.tv_no_categories);
        btnAdd = findViewById(R.id.btn_add);
        btnDelete = findViewById(R.id.btn_delete);
        progressGroup = findViewById(R.id.progress_group);
        titleTextView = findViewById(R.id.title);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryListAdapter(comparator, this, this);
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

        btnSync.setOnClickListener(v -> syncData());

        btnAdd.setOnClickListener(v -> showAddDialog());

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();
        loadData();
    }

    private void loadData() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Category entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with size=" + list.size() + " " + Thread.currentThread());
            exitSelectionMode();
            categoryList = list;
            adapter.replaceAll(list);
            tvNoCategories.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // Create a new AlertDialog.Builder each time
            new AlertDialog.Builder(this)
                    .setTitle("Database Error")
                    .setMessage("Error while fetching Category entries: " + err)
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
        List<Category> selectedCategories = adapter.getSelectedCategories();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            int deletedCount = 0;
            for (Category category : selectedCategories) {
                int rowCount = AppDatabaseImpl.getDatabase(getApplicationContext()).categories().delete(category);
                if (rowCount > 0) {
                    deletedCount += 1;
                    // delete from online database
                    mDatabase.child("categories").child(String.valueOf(category.categoryId)).removeValue();
                }
            }
            return deletedCount;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(count -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Deleted " + count + " categories");
            loadData(); // refresh
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error during deletion: " + err);

            // Create a new AlertDialog.Builder each time
            new AlertDialog.Builder(this)
                    .setTitle("Invalid Action")
                    .setMessage("Selected Category/ies are associated with some products. Please delete or edit them first.")
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
                Category category = adapter.getItem(position);
                if (category != null) {
                    Log.d(TAG, "Category selected: " + category.categoryName);
                    showAddDialog(category);
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
            titleTextView.setText(R.string.categories);
        }
    }

    private void exitSelectionMode() {
        adapter.exitSelectionMode();
        updateSelectionUI();
    }

    private void onSearch(String query) {
        List<Category> filteredList = filter(categoryList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Category> filter(List<Category> categoryList, String query) {
        String str = query.toLowerCase();
        List<Category> list = new ArrayList<>();
        for (Category category : categoryList) {
            if (category.categoryName.toLowerCase().contains(str)) {
                list.add(category);
            }
        }
        return list;
    }

    private void showAddDialog() {
        showAddDialog(null);
    }

    private void showAddDialog(Category categoryToEdit) {
        // Create a new LayoutInflater instance each time
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);

        TextView tvTitle = dialogView.findViewById(R.id.et_title);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        tvTitle.setText(categoryToEdit == null ? R.string.add_category : R.string.edit_category);
        if (categoryToEdit != null) {
            etName.setText(categoryToEdit.categoryName);
        }

        // Create a new AlertDialog.Builder each time
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        addDialog = builder.create();

        btnCancel.setOnClickListener(v -> addDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String str = etName.getText().toString();
            if (!str.isBlank()) {
                if (categoryToEdit == null) {
                    addCategory(str);
                } else {
                    updateCategory(categoryToEdit.categoryId, str);
                }
            }
            etName.getText().clear();
            addDialog.dismiss();
        });

        addDialog.show();
    }

    private void addCategory(String categoryName) {
        Category category = new Category();
        category.categoryName = categoryName;

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().insert(category);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            // add entry to online database
            CategoryEntry entry = new CategoryEntry();
            entry.id = id.intValue();
            entry.category = categoryName;
            mDatabase.child("categories")
                    .child(String.valueOf(entry.id))
                    .setValue(entry)
                    .addOnCompleteListener(this, task -> {
                        progressGroup.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Category entry added!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "failure add category", task.getException());
                        }
                        loadData();
                    });
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            // Create a new AlertDialog.Builder each time
            new AlertDialog.Builder(this)
                    .setTitle("Database Error")
                    .setMessage("Error while saving Category entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }));
    }

    private void updateCategory(int categoryId, String categoryName) {
        Category category = new Category();
        category.categoryId = categoryId;
        category.categoryName = categoryName;

        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().update(category);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowsAffected -> {
            // update entry in online database
            mDatabase.child("categories")
                    .child(String.valueOf(categoryId))
                    .child("category")
                    .setValue(categoryName)
                    .addOnCompleteListener(this, task -> {
                        progressGroup.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Category updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "failure update category", task.getException());
                        }
                        loadData();
                    });
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            // Create a new AlertDialog.Builder each time
            new AlertDialog.Builder(this)
                    .setTitle("Database Error")
                    .setMessage("Error while updating Category entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }));
    }
    
    private void syncData() {
        progressGroup.setVisibility(View.VISIBLE);
        for (Category category : categoryList) {
            CategoryEntry entry = new CategoryEntry(category.categoryId, category.categoryName);
            mDatabase.child("categories").child(String.valueOf(category.categoryId)).setValue(entry);
        }
        Toast.makeText(this, "Categories Synced!", Toast.LENGTH_SHORT).show();
        progressGroup.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposables != null) {
            disposables.dispose();
        }
    }
}

