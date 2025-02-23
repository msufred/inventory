package io.zak.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.zak.inventory.R;
import io.zak.inventory.WarehousesActivity;
import io.zak.inventory.data.entities.Warehouse;

public class WarehouseListAdapter extends RecyclerView.Adapter<WarehouseListAdapter.WarehouseViewHolder> {


    public static class WarehouseViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName, tvAddress, tvContact;
        private final OnItemClickListener listener;

        public WarehouseViewHolder(View view, OnItemClickListener listener) {
            super(view);
            LinearLayout layout = view.findViewById(R.id.layout);
            tvName = view.findViewById(R.id.tv_name);
            tvAddress = view.findViewById(R.id.tv_address);
            tvContact = view.findViewById(R.id.tv_contact);
            this.listener = listener;

            layout.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (this.listener != null) {
                    this.listener.onItemClick(pos);
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final Comparator<Warehouse> comparator;

    private final SortedList<Warehouse> sortedList = new SortedList<>(Warehouse.class, new SortedList.Callback<>() {
        @Override
        public int compare(Warehouse o1, Warehouse o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Warehouse oldItem, Warehouse newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Warehouse item1, Warehouse item2) {
            return item1.id == item2.id;
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    });

    private final OnItemClickListener listener;

    public WarehouseListAdapter(Comparator<Warehouse> comparator, OnItemClickListener listener) {
        this.comparator = comparator;
        this.listener = listener;
    }

    public void addItem(Warehouse warehouse) {
        sortedList.add(warehouse);
    }

    public void addAll(List<Warehouse> list) {
        sortedList.addAll(list);
    }

    public void removeItem(Warehouse warehouse) {
        sortedList.remove(warehouse);
    }

    public void replaceAll(List<Warehouse> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            final Warehouse warehouse = sortedList.get(i);
            if (!list.contains(warehouse)) sortedList.remove(warehouse);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

    public void clear() {
        sortedList.clear();
    }

    public Warehouse getItem(int position) {
        return sortedList.get(position);
    }

    @NonNull
    @Override
    public WarehouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_warehouse, parent, false);
        return new WarehouseViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull WarehouseViewHolder holder, int position) {
        Warehouse warehouse = sortedList.get(position);
        if (warehouse != null) {
            holder.tvName.setText(warehouse.name);
            if (!warehouse.address.isBlank()) holder.tvAddress.setText(warehouse.address);
            if (!warehouse.contactNo.isBlank()) holder.tvContact.setText(warehouse.contactNo);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

}
