package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.DeliveryListAdapter;
import io.zak.inventory.adapters.EmployeeSpinnerAdapter;
import io.zak.inventory.adapters.VehicleSpinnerAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Vehicle;
import io.zak.inventory.data.relations.DeliveryDetails;

public class DeliveryFragment extends Fragment implements DeliveryListAdapter.OnItemClickListener {

    private static final String TAG = "Deliveries";

    // Widgets
    private SearchView searchView;              // use to search delivery by vehicle name
    private RecyclerView recyclerView;          // contains delivery list
    private TextView tvNoDeliveries;            // visible if delivery list is empty
    private Button btnAdd;
    private RelativeLayout progressGroup;       // progress indicator group

    private CompositeDisposable disposables;    // holds Disposable objects (reactive programming)
    private AlertDialog.Builder dialogBuilder;  // creates AlertDialog (error, add, etc)

    private DeliveryListAdapter adapter;        // recyclerView list adapter
    private List<DeliveryDetails> deliveryList; // reference list of deliveries

    // for Add Delivery dialog
    private List<Vehicle> vehicleList;          // list of all vehicles
    private List<Employee> employeeList;        // list of all employees
    private AlertDialog addDialog;              // used to add delivery item
    private Vehicle mVehicle;                   // current selected vehicle (add dialog)
    private Employee mEmployee;                 // current selected employee (add dialog)

    // used for list adapter's sorted list (see DeliveryListAdapter class)
    private final Comparator<DeliveryDetails> comparator = Comparator.comparing(deliveryDetails -> deliveryDetails.vehicle.vehicleName);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery, container, false);
        getWidgets(view);
        setListeners();
        return view;
    }

    private void getWidgets(View view) {
        searchView = view.findViewById(R.id.search_view);
        recyclerView = view.findViewById(R.id.recycler_view);
        tvNoDeliveries = view.findViewById(R.id.tv_no_deliveries);
        btnAdd = view.findViewById(R.id.btn_add);
        progressGroup = view.findViewById(R.id.progress_group);

        // setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new DeliveryListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        // initialize dialog builder
        dialogBuilder = new AlertDialog.Builder(requireActivity());
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

        btnAdd.setOnClickListener(v -> {
            if (addDialog == null) createDialog();
            addDialog.show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (disposables == null) disposables = new CompositeDisposable();
        refresh();
    }

    public void refresh() {
        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getActivity());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Vehicle entries: " + Thread.currentThread());
            return database.vehicles().getAll();
        }).flatMap(vehicles -> {
            Log.d(TAG, "Returned with list size=" + vehicles.size());
            vehicleList = vehicles;
            return Single.fromCallable(() -> {
               Log.d(TAG, "Fetching Employee entries: " + Thread.currentThread());
               return database.employees().getAll();
            });
        }).flatMap(employees -> {
            Log.d(TAG, "Returned with list size=" + employees.size());
            employeeList = employees;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching DeliveryOrder entries: " + Thread.currentThread());
                return database.deliveryOrders().getDeliveryOrdersWithDetails();
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + list.size() + " " + Thread.currentThread());
            deliveryList = list;
            adapter.replaceAll(deliveryList);
            tvNoDeliveries.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching DeliveryOrder items: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            DeliveryDetails deliveryDetails = adapter.getItem(position);
            if (deliveryDetails != null) {
                viewDeliveryItems(deliveryDetails.deliveryOrder.deliveryOrderId);
            }
        }
    }

    private void onSearch(String query) {
        List<DeliveryDetails> filteredList = filter(deliveryList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<DeliveryDetails> filter(List<DeliveryDetails> deliveryDetailsList, String query) {
        String str = query.toLowerCase();
        List<DeliveryDetails> list = new ArrayList<>();
        for (DeliveryDetails deliveryDetails : deliveryDetailsList) {
            if (deliveryDetails.vehicle.vehicleName.contains(str)) {
                list.add(deliveryDetails);
            }
        }
        return list;
    }

    private void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.dialog_add_delivery, null);

        Spinner vehicleSpinner = dialogView.findViewById(R.id.vehicle_spinner);
        vehicleSpinner.setAdapter(new VehicleSpinnerAdapter(getActivity(), vehicleList));
        vehicleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (vehicleList != null) mVehicle = vehicleList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        TextView emptyVehicle = dialogView.findViewById(R.id.empty_vehicle_spinner);
        emptyVehicle.setVisibility(vehicleList.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        Spinner employeeSpinner = dialogView.findViewById(R.id.employee_spinner);
        employeeSpinner.setAdapter(new EmployeeSpinnerAdapter(getActivity(), employeeList));
        employeeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (employeeList != null) mEmployee = employeeList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        TextView emptyEmployee = dialogView.findViewById(R.id.empty_employee_spinner);
        emptyEmployee.setVisibility(employeeList.isEmpty() ? View.VISIBLE : View.INVISIBLE);

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> addDialog.dismiss());

        Button btnSave = dialogView.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            if (mEmployee != null && mVehicle != null) {
                addDelivery(mVehicle, mEmployee);
            }
            addDialog.dismiss();
        });

        dialogBuilder.setView(dialogView);
        addDialog = dialogBuilder.create();
    }

    private void addDelivery(Vehicle vehicle, Employee employee) {
        Date now = new Date();
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.fkVehicleId = vehicle.vehicleId;
        deliveryOrder.fkEmployeeId = employee.employeeId;
        deliveryOrder.deliveryDate = now.getTime();
        deliveryOrder.totalAmount = 0.0;
        deliveryOrder.deliveryOrderStatus = "Processing";

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Adding new DeliveryOrder entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getActivity()).deliveryOrders().insert(deliveryOrder);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with id=" + id + " " + Thread.currentThread());

            viewDeliveryItems(id.intValue());
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while adding DeliveryOrder entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    private void viewDeliveryItems(int id) {
        Intent intent = new Intent(getActivity(), ViewDeliveryOrderActivity.class);
        intent.putExtra("delivery_id", id);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        addDialog = null;
    }
}
