package io.zak.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.zak.inventory.R;
import io.zak.inventory.WarehousesActivity;
import io.zak.inventory.data.entities.Warehouse;

public class WarehouseListAdapter extends RecyclerView.Adapter<WarehouseListAdapter.WarehouseViewHolder> {


    private final Context context;
    private ArrayList<Warehouse> arrayList;

    public WarehouseListAdapter(Context context, List<Warehouse> list) {
        this.context = context;
        arrayList = new ArrayList<>(list);
    }

    public void addItem(Warehouse warehouse) {
        arrayList.add(warehouse);
        notifyItemInserted(arrayList.size() - 1);
    }

    @NonNull
    @Override
    public WarehouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_profile, parent, false);
        return new WarehouseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WarehouseViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class WarehouseViewHolder extends RecyclerView.ViewHolder {

        WarehouseViewHolder(View view) {
            super(view);
        }
    }
}
