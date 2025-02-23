package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

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

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private Button btnBack, btnAdd;
    private RelativeLayout progressGroup;

    // RecyclerView adapter
    private CategoryListAdapter adapter;

    // list reference for search filter
    private List<Category> categoryList;

    // comparator for search filter
    private final Comparator<Category> comparator = Comparator.comparing(category -> category.category);

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
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        dialogBuilder = new AlertDialog.Builder(this);
        createDialog();
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

        btnAdd.setOnClickListener(v -> addDialog.show());
    }

    private void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);

        EditText etName = dialogView.findViewById(R.id.et_brand_name);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        dialogBuilder.setView(dialogView);
        addDialog = dialogBuilder.create();

        btnCancel.setOnClickListener(v -> addDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String str = etName.getText().toString();
            if (!str.isBlank()) addCategory(str);
            etName.getText().clear();
            addDialog.dismiss();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Category entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with size=" + list.size() + " " + Thread.currentThread());
            categoryList = list;
            adapter.replaceAll(list);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // error dialog
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
                Log.d(TAG, "Selected category=" + category.category);
                // TODO
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
            if (category.category.toLowerCase().contains(str)) {
                list.add(category);
            }
        }
        return list;
    }

    private void addCategory(String str) {
        Category category = new Category();
        category.category = Utils.normalize(str);

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Category entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).categories().insert(category);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with ID=" + id + " " + Thread.currentThread());
            category.id = id.intValue();
            adapter.addItem(category);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving Category entry: " + err)
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
