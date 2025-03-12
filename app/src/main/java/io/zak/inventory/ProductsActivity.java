package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import io.zak.inventory.adapters.ProductListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.relations.ProductDetails;
import io.zak.inventory.firebase.ProductEntry;

public class ProductsActivity extends AppCompatActivity implements ProductListAdapter.OnItemClickListener {

    private static final String TAG = "Products";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoProducts;
    private ImageButton btnBack, btnSync;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    // RecyclerView adapter
    private ProductListAdapter adapter;

    // list reference for search filter
    private List<Product> productList;

    // comparator for search filter
    private final Comparator<Product> comparator = Comparator.comparing(product -> product.productName);

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoProducts = findViewById(R.id.tv_no_products);
        btnBack = findViewById(R.id.btn_back);
        btnSync = findViewById(R.id.btn_sync);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductListAdapter(comparator, this);
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

        btnSync.setOnClickListener(v -> syncData());

        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Product entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).products().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with size=" + list.size() + " " + Thread.currentThread());
            productList = list;
            adapter.replaceAll(list);
            tvNoProducts.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Product enties: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> dialog.dismiss()));
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Product product = adapter.getItem(position);
            if (product != null) {
                Log.d(TAG, "Product selected: " + product.productName);
                // TODO
                Intent intent = new Intent(this, ViewProductActivity.class);
                intent.putExtra("product_id", product.productId);
                startActivity(intent);
            }
        }
    }

    private void onSearch(String query) {
        List<Product> filteredList = filter(productList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Product> filter(List<Product> products, String query) {
        String str = query.toLowerCase();
        List<Product> list = new ArrayList<>();
        for (Product product : products) {
            if (product.productName.toLowerCase().contains(str)) {
                list.add(product);
            }
        }
        return list;
    }

    private void syncData() {
        progressGroup.setVisibility(View.VISIBLE);
        // first, delete all data from online database
        mDatabase.child("products").setValue(null);
        // add non-deleted entries to online database
        for (Product product : productList) {
            ProductEntry entry = new ProductEntry();
            entry.id = product.productId;
            entry.name = product.productName;
            entry.brandId = product.fkBrandId;
            entry.categoryId = product.fkCategoryId;
            entry.supplierId = product.fkSupplierId;
            entry.criticalLevel = product.criticalLevel;
            entry.price = product.price;
            entry.description = product.productDescription;
            mDatabase.child("products")
                    .child(String.valueOf(entry.id))
                    .setValue(entry);
        }
        Toast.makeText(this, "Products Data Synced", Toast.LENGTH_SHORT).show();
        progressGroup.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
