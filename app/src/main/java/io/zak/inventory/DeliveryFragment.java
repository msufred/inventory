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

    // used for list adapter's sorted list (see DeliveryListAdapter class)
    // we will sort Delivery items by date
    private final Comparator<DeliveryDetails> comparator =
            Comparator.comparing(deliveryDetails -> deliveryDetails.deliveryOrder.deliveryDate);

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

        btnAdd.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddDeliveryOrderActivity.class)));
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
            Log.d(TAG, "Fetching DeliveryOrder entries: " + Thread.currentThread());
            return database.deliveryOrders().getDeliveryOrdersWithDetails();
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

    private void viewDeliveryItems(int id) {
        Intent intent = new Intent(getActivity(), ViewDeliveryOrderActivity.class);
        intent.putExtra("delivery_id", id);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
