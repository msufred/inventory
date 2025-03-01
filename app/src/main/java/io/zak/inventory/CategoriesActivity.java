package io.zak.inventory;

import android.annotation.SuppressLint;
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
import io.zak.inventory.adapters.CategoryListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Category;

public class CategoriesActivity extends AppCompatActivity implements CategoryListAdapter.OnItemClickListener {

    private static final String TAG = "Categories";

    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoCategories;
    private Button  btnAdd;
    private ImageButton btnBack;

    private RelativeLayout progressGroup;

    private CategoryListAdapter adapter;
    private List<Category> categoryList;
    private final Comparator<Category> comparator = Comparator.comparing(category -> category.categoryName);
    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog addDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoCategories = findViewById(R.id.tv_no_categories);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryListAdapter(comparator, this);
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

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            finish();
        });

        btnAdd.setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        showAddDialog(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Category entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Returned with size=" + list.size() + " " + Thread.currentThread());
            categoryList = list;
            adapter.replaceAll(list);
            tvNoCategories.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Category entries: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Category category = adapter.getItem(position);
            if (category != null) {
                Log.d(TAG, "Category selected: " + category.categoryName);
                showAddDialog(category);
            }
        }
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



    private void showAddDialog(Category categoryToEdit) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView tvTitle = dialogView.findViewById(R.id.et_title);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        tvTitle.setText(categoryToEdit == null ? R.string.add_category : R.string.edit_category);
        if (categoryToEdit != null) {
            etName.setText(categoryToEdit.categoryName);
        }

        dialogBuilder.setView(dialogView);
        addDialog = dialogBuilder.create();

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

        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().insert(category);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            category.categoryId = id.intValue();
            adapter.addItem(category);
            if (tvNoCategories.getVisibility() == View.VISIBLE) tvNoCategories.setVisibility(View.INVISIBLE);
        }, err -> {
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving Category entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    private void updateCategory(int categoryId, String categoryName) {
        Category category = new Category();
        category.categoryId = categoryId;
        category.categoryName = categoryName;

        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().update(category);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowsAffected -> {
            adapter.updateItem(category);
        }, err -> {
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while updating Category entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
