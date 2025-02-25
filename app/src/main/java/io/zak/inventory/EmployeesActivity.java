package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import io.zak.inventory.adapters.EmployeeListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Warehouse;

public class EmployeesActivity extends AppCompatActivity implements EmployeeListAdapter.OnItemClickListener {

    private static final String TAG = "Employees";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoEmployees;
    private Button btnBack, btnAdd;
    private RelativeLayout progressGroup;

    // for RecyclerView
    private EmployeeListAdapter adapter;
    private List<Employee> employeeList; // Employee list reference

    // for search filter
    private final Comparator<Employee> comparator = Comparator.comparing(employee -> employee.name);

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employees);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        tvNoEmployees = findViewById(R.id.tv_no_employees);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeListAdapter(comparator, this);
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

        btnBack.setOnClickListener(v -> goBack());

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEmployeeActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Employee entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).employees().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
            employeeList = list;
            adapter.replaceAll(list);
            tvNoEmployees.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Employee employee = adapter.getItem(position);
            if (employee != null) {
                Intent intent = new Intent(this, EditEmployeeActivity.class);
                intent.putExtra("employee_id", employee.id);
                startActivity(intent);
            }
        }
    }

    private void onSearch(String query) {
        final List<Employee> filteredList = filter(employeeList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Employee> filter(List<Employee> ref, String query) {
        String str = query.toLowerCase();
        final List<Employee> list = new ArrayList<>();
        for (Employee employee : ref) {
            if (employee.name.toLowerCase().contains(str)) {
                list.add(employee);
            }
        }
        return list;
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources...");
        disposables.dispose();
    }
}
