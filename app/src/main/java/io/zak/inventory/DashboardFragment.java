package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class DashboardFragment extends Fragment {

    private static final String DEBUG_NAME = "Dashboard";

    private CardView cardWarehouses, cardEmployees, cardVehicles, cardProducts, cardBrands, cardCategories, cardSuppliers, cardConsumers;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // get widgets
        cardWarehouses = view.findViewById(R.id.card_warehouses);
        cardEmployees = view.findViewById(R.id.card_employees);
        cardVehicles = view.findViewById(R.id.card_vehicles);
        cardProducts = view.findViewById(R.id.card_products);
        cardBrands = view.findViewById(R.id.card_brands);
        cardCategories = view.findViewById(R.id.card_categories);
        cardSuppliers = view.findViewById(R.id.card_suppliers);
        cardConsumers = view.findViewById(R.id.card_consumers);

        // setup listeners
        cardWarehouses.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), WarehousesActivity.class));
        });

        return view;
    }
}
