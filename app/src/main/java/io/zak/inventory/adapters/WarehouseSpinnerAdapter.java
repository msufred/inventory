package io.zak.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.zak.inventory.data.entities.Warehouse;

public class WarehouseSpinnerAdapter extends ArrayAdapter<Warehouse> {

    private final Context context;
    private final List<Warehouse> warehouseList;

    public WarehouseSpinnerAdapter(Context context, List<Warehouse> warehouseList) {
        super(context, android.R.layout.simple_spinner_item, warehouseList);
        this.context = context;
        this.warehouseList = warehouseList;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createCustomView(position, convertView, parent);
    }

    private View createCustomView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }

        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText(warehouseList.get(position).warehouseName);
        return view;
    }
}
