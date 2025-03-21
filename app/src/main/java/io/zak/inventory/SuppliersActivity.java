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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.SupplierListAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Supplier;
import io.zak.inventory.firebase.SupplierEntry;

public class SuppliersActivity extends AppCompatActivity implements SupplierListAdapter.OnItemClickListener {

    private static final String TAG = "Suppliers";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoSuppliers;
    private ImageButton btnBack, btnSync;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    // RecyclerView adapter
    private SupplierListAdapter adapter;

    // list reference for search filter
    private List<Supplier> supplierList;

    // Comparator used for search filter; passed to SupplierListAdapter
    private final Comparator<Supplier> comparator = Comparator.comparing(supplier -> supplier.supplierName);

    private CompositeDisposable disposables;

    private DatabaseReference mDatabase;
    private DatabaseReference mSuppliersRef;

    private final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            fetchSuppliers(snapshot);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled", error.toException());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suppliers);
        getWidgets();
        setListeners();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mSuppliersRef = mDatabase.child("suppliers");
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoSuppliers = findViewById(R.id.tv_no_suppliers);
        btnBack = findViewById(R.id.btn_back);
        btnSync = findViewById(R.id.btn_sync);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

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

        btnSync.setOnClickListener(v -> syncData());

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddSupplierActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Supplier entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).suppliers().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            supplierList = list;
            adapter.replaceAll(list);
            tvNoSuppliers.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);

            // fetch suppliers online and sync
            mSuppliersRef.addValueEventListener(valueEventListener);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    private void fetchSuppliers(DataSnapshot dataSnapshot) {
        Log.d(TAG, "fetch suppliers online");
        progressGroup.setVisibility(View.VISIBLE);
        List<SupplierEntry> supplierEntries = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            SupplierEntry entry = snapshot.getValue(SupplierEntry.class);
            if (entry != null) supplierEntries.add(entry);
        }
        mSuppliersRef.removeEventListener(valueEventListener);

        // save to local database
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "saving to local database");
            int count = 0;
            AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
            for (SupplierEntry entry : supplierEntries) {
                Supplier supplier = new Supplier();
                supplier.supplierId = entry.id;
                supplier.supplierName = entry.name;
                supplier.supplierAddress = entry.address;
                supplier.supplierContactNo = entry.contactNo;
                supplier.supplierEmail = entry.email;

                boolean hasDuplicate = false;
                for (Supplier s : supplierList) {
                    if (s.supplierId == supplier.supplierId) {
                        hasDuplicate = true;
                        break;
                    }
                }

                if (!hasDuplicate) {
                    database.suppliers().insert(supplier);
                    adapter.addItem(supplier);
                    supplierList.add(supplier);
                    count++;
                }
            }
            return count;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(count -> {
            Log.d(TAG, "added " + count + " items");
            //tvNoSuppliers.setVisibility(supplierList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
            progressGroup.setVisibility(View.GONE);
        }, err -> {
            Log.e(TAG, "database error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Supplier supplier = adapter.getItem(position);
            if (supplier != null) {
                Log.d(TAG, "Supplier selected: " + supplier.supplierName);
                Intent intent = new Intent(this, ViewSupplierActivity.class);
                intent.putExtra("supplier_id", supplier.supplierId);
                startActivity(intent);
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
            if (supplier.supplierName.toLowerCase().contains(str)) {
                list.add(supplier);
            }
        }
        return list;
    }

    /**
     * Sync data to online database (Firebase). Prioritize local storage.
     * TODO Fetch all entries from Firebase, check if item is deleted from Room database.
     * Delete entry from Firebase if necessary.
     */
    private void syncData() {
        progressGroup.setVisibility(View.VISIBLE);
        for (Supplier supplier : supplierList) {
            SupplierEntry entry = new SupplierEntry(
                    supplier.supplierId,
                    supplier.supplierName,
                    supplier.supplierAddress,
                    supplier.supplierEmail,
                    supplier.supplierContactNo
            );
            mDatabase.child("suppliers")
                    .child(String.valueOf(supplier.supplierId))
                    .setValue(entry);
        }
        Toast.makeText(this, "Suppliers Data Synced", Toast.LENGTH_SHORT).show();
        progressGroup.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
