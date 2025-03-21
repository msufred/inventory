package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Database;

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
import io.zak.inventory.adapters.EmployeeListAdapter;
import io.zak.inventory.adapters.UserListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.firebase.UserEntry;

public class EmployeesActivity extends AppCompatActivity implements EmployeeListAdapter.OnItemClickListener {

    private static final String TAG = "Employees";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoEmployees;
    private ImageButton btnBack, btnSync;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    // for RecyclerView
    // private EmployeeListAdapter adapter;
    // private List<Employee> employeeList; // Employee list reference

    // for search filter
    private final Comparator<Employee> comparator = Comparator.comparing(employee -> employee.employeeName);

    private CompositeDisposable disposables;
    private DatabaseReference mDatabase;
    private DatabaseReference mUsersRef;
    private final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            replaceEmployees(snapshot);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "cancelled", error.toException());
        }
    };

    private UserListAdapter userListAdapter;
    private List<UserEntry> userEntryList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employees);
        getWidgets();
        setListeners();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        tvNoEmployees = findViewById(R.id.tv_no_employees);
        btnBack = findViewById(R.id.btn_back);
        btnSync = findViewById(R.id.btn_sync);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // adapter = new EmployeeListAdapter(comparator, this);
        // recyclerView.setAdapter(adapter);
        userListAdapter = new UserListAdapter();
        recyclerView.setAdapter(userListAdapter);
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

        btnSync.setOnClickListener(v -> {
            if (mUsersRef != null) {
                mUsersRef.removeEventListener(valueEventListener);
                mUsersRef.addValueEventListener(valueEventListener); // re-listen and fetch online
            }
        });

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEmployeeActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

//        progressGroup.setVisibility(View.VISIBLE);
//        disposables.add(Single.fromCallable(() -> {
//            Log.d(TAG, "Fetching Employee entries: " + Thread.currentThread());
//            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().getAll();
//        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
//            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
//            progressGroup.setVisibility(View.GONE);
//            employeeList = list;
//            adapter.replaceAll(list);
//            tvNoEmployees.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
//        }, err -> {
//            Log.e(TAG, "Database Error: " + err);
//            progressGroup.setVisibility(View.GONE);
//        }));

        // fetch users online
        if (mUsersRef == null) {
            mUsersRef = mDatabase.child("users");
            mUsersRef.addValueEventListener(valueEventListener);
        }
    }

    private void replaceEmployees(DataSnapshot dataSnapshot) {
        progressGroup.setVisibility(View.VISIBLE);
        List<UserEntry> list = new ArrayList<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            UserEntry entry = snapshot.getValue(UserEntry.class);
            if (entry != null) {
                entry.uid = snapshot.getKey();
                Log.d(TAG, snapshot.getKey());
                list.add(entry);
            }
        }
        userListAdapter.replaceAll(list);
        userEntryList = list;
        tvNoEmployees.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        mUsersRef.removeEventListener(valueEventListener);
        progressGroup.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(int position) {
//        if (adapter != null) {
//            Employee employee = adapter.getItem(position);
//            Log.d(TAG, "Employee selected (ID: " + employee.employeeId + ")");
//
//            Intent intent = new Intent(this, ViewEmployeeActivity.class);
//            intent.putExtra("employee_id", employee.employeeId);
//            startActivity(intent);
//        }
    }

    private void onSearch(String query) {
//        final List<Employee> filteredList = filter(employeeList, query);
//        adapter.replaceAll(filteredList);
//        recyclerView.scrollToPosition(0);
        final List<UserEntry> filteredList = filterUserEntry(userEntryList, query);
        userListAdapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Employee> filter(List<Employee> ref, String query) {
        String str = query.toLowerCase();
        final List<Employee> list = new ArrayList<>();
        for (Employee employee : ref) {
            if (employee.employeeName.toLowerCase().contains(str)) {
                list.add(employee);
            }
        }
        return list;
    }

    private List<UserEntry> filterUserEntry(List<UserEntry> ref, String query) {
        List<UserEntry> list = new ArrayList<>();
        for (UserEntry entry : ref) {
            if (entry.fullName.toLowerCase().contentEquals(query.toLowerCase())) {
                list.add(entry);
            }
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources...");
        disposables.dispose();
    }
}
