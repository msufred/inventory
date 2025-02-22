package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class DashboardFragment extends Fragment {

    private static final String TAG = "Dashboard";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // get widgets
        CardView cardWarehouses = view.findViewById(R.id.card_warehouses);
        CardView cardEmployees = view.findViewById(R.id.card_employees);
        CardView cardVehicles = view.findViewById(R.id.card_vehicles);
        CardView cardProducts = view.findViewById(R.id.card_products);
        CardView cardBrands = view.findViewById(R.id.card_brands);
        CardView cardCategories = view.findViewById(R.id.card_categories);
        CardView cardSuppliers = view.findViewById(R.id.card_suppliers);
        CardView cardConsumers = view.findViewById(R.id.card_consumers);

        // setup listeners
        cardWarehouses.setOnClickListener(v -> startActivity(new Intent(getActivity(), WarehousesActivity.class)));
        cardEmployees.setOnClickListener(v -> startActivity(new Intent(getActivity(), EmployeesActivity.class)));
        cardVehicles.setOnClickListener(v -> startActivity(new Intent(getActivity(), VehiclesActivity.class)));
        cardBrands.setOnClickListener(v -> startActivity(new Intent(getActivity(), BrandsActivity.class)));
        cardSuppliers.setOnClickListener(v -> startActivity(new Intent(getActivity(), SuppliersActivity.class)));

        return view;
    }
}
