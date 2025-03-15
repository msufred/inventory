package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.DeliveryListAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.DeliveryOrder;

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
    private List<DeliveryOrder> deliveryList;   // reference list of deliveries

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
        adapter = new DeliveryListAdapter(this);
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
            if (!hasPendingDelivery()) {
                startActivity(new Intent(getActivity(), AddDeliveryOrderActivity.class));
            } else {
                dialogBuilder.setTitle("Invalid Action")
                        .setMessage("Please complete \"Processing\" deliveries first and try again.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                dialogBuilder.create().show();
            }
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
            Log.d(TAG, "Fetching DeliveryOrder entries: " + Thread.currentThread());
            return database.deliveryOrders().getAll();
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

    public boolean hasPendingDelivery() {
        for (DeliveryOrder deliveryOrder : deliveryList) {
            if (deliveryOrder.deliveryOrderStatus.equalsIgnoreCase("processing")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            DeliveryOrder deliveryOrder = adapter.getItem(position);
            if (deliveryOrder != null) {
                viewDeliveryItems(deliveryOrder.deliveryOrderId);
            }
        }
    }

    private void onSearch(String query) {
        List<DeliveryOrder> filteredList = filter(deliveryList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<DeliveryOrder> filter(List<DeliveryOrder> deliveryOrderList, String query) {
        String str = query.toLowerCase();
        List<DeliveryOrder> list = new ArrayList<>();
        for (DeliveryOrder deliveryOrder : deliveryOrderList) {
            if (deliveryOrder.vehicleName.contains(str)) {
                list.add(deliveryOrder);
            }
        }
        return list;
    }

    private void viewDeliveryItems(int id) {
        Intent intent = new Intent(getActivity(), ViewDeliveryOrderActivity.class);
        intent.putExtra("delivery_id", id);
        startActivity(intent);
    }

}
