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

import com.google.firebase.Firebase;
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
import io.zak.inventory.adapters.VehicleListAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Vehicle;
import io.zak.inventory.firebase.VehicleEntry;

public class VehiclesActivity extends AppCompatActivity implements VehicleListAdapter.OnItemClickListener {

    private static final String TAG = "Vehicles";
    private static final int EDIT_VEHICLE_REQUEST = 1001;

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoVehicles;
    private ImageButton btnBack, btnUpload;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    // for RecyclerView
    private List<Vehicle> vehicleList;
    private VehicleListAdapter adapter;
    private final Comparator<Vehicle> comparator = Comparator.comparing(vehicle -> vehicle.vehicleName);

    private CompositeDisposable disposables;
    private DatabaseReference mDatabase;
    private DatabaseReference mVehiclesRef;

    private final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            fetchVehicles(snapshot);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled", error.toException());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicles);
        getWidgets();
        setListeners();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mVehiclesRef = mDatabase.child("vehicles");
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoVehicles = findViewById(R.id.tv_no_vehicles);
        btnBack = findViewById(R.id.btn_back);
        btnUpload = findViewById(R.id.btn_upload);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VehicleListAdapter(comparator, this);
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

        btnUpload.setOnClickListener(v -> uploadData());

        btnAdd.setOnClickListener(v -> startActivity(new Intent(this, AddVehicleActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();
        loadVehicles(); // TODO synchronize local data with online data
    }

    private void loadVehicles() {
        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Vehicle entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).vehicles().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            vehicleList = list;
            adapter.replaceAll(list);
            tvNoVehicles.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);

            // fetch vehicles online and sync
            mVehiclesRef.addValueEventListener(valueEventListener);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    private void fetchVehicles(DataSnapshot dataSnapshot) {
        Log.d(TAG, "fetch vehicles online");
        List<VehicleEntry> vehicleEntries = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            VehicleEntry entry = snapshot.getValue(VehicleEntry.class);
            if (entry != null) vehicleEntries.add(entry);
        }
        mVehiclesRef.removeEventListener(valueEventListener);

        // add to local database
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "saving to local database");
            int count = 0;
            AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
            for (VehicleEntry entry : vehicleEntries) {
                Vehicle vehicle = new Vehicle();
                vehicle.vehicleId = entry.id;
                vehicle.vehicleName = entry.name;
                vehicle.vehicleType = entry.type;
                vehicle.plateNo = entry.plateNo;
                vehicle.vehicleStatus = entry.status;

                boolean hasDuplicate = false;
                for (Vehicle v : vehicleList) {
                    if (v.vehicleId == vehicle.vehicleId) {
                        hasDuplicate = true;
                        break;
                    }
                }

                if (!hasDuplicate) {
                    database.vehicles().insert(vehicle);
                    adapter.addItem(vehicle);
                    vehicleList.add(vehicle);
                    count++;
                }
            }
            return count;
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(count -> {
            Log.d(TAG, "added " + count + " items");
            tvNoVehicles.setVisibility(vehicleList.isEmpty() ? View.VISIBLE : View.GONE);
            progressGroup.setVisibility(View.GONE);
        }, err -> {
            Log.e(TAG, "database error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    public void uploadData() {
        progressGroup.setVisibility(View.VISIBLE);
        Log.d(TAG, "uploading vehicle entries");
        for (Vehicle vehicle : vehicleList) {
            VehicleEntry entry = new VehicleEntry(
                    vehicle.vehicleId,
                    vehicle.vehicleName,
                    vehicle.vehicleType,
                    vehicle.plateNo,
                    vehicle.vehicleStatus
            );
            mDatabase.child("vehicles").child(String.valueOf(vehicle.vehicleId)).setValue(entry);
        }
        Toast.makeText(this, "Vehicle Entries Uploaded", Toast.LENGTH_SHORT).show();
        progressGroup.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Vehicle vehicle = adapter.getItem(position);
            if (vehicle != null) {
                Intent intent = new Intent(this, EditVehicleActivity.class);
                intent.putExtra("vehicleId", vehicle.vehicleId);
                startActivityForResult(intent, EDIT_VEHICLE_REQUEST);
                Log.d(TAG, "Vehicle selected: " + vehicle.vehicleName + " with ID: " + vehicle.vehicleId);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_VEHICLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                loadVehicles();

                if (data != null) {
                    int vehicleId = data.getIntExtra("vehicleId", -1);
                    Log.d(TAG, "Returned from editing vehicle with ID: " + vehicleId);
                }
            }
        }
    }

    private void onSearch(String query) {
        List<Vehicle> filteredList = filterList(vehicleList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Vehicle> filterList(List<Vehicle> ref, String query) {
        String str = query.toLowerCase();
        final List<Vehicle> list = new ArrayList<>();
        for (Vehicle vehicle : ref) {
            if (vehicle.vehicleName.toLowerCase().contains(str)) {
                list.add(vehicle);
            }
        }
        return list;
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