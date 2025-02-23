package io.zak.inventory.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.zak.inventory.R;
import io.zak.inventory.data.relations.WarehouseStockDetails;

public class WarehouseStockListAdapter extends RecyclerView.Adapter<WarehouseStockListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, quantity, dateAcquired;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            quantity = view.findViewById(R.id.tv_quantity);
            dateAcquired = view.findViewById(R.id.tv_date);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> {
                onItemClickListener.onItemClick(getAdapterPosition());
            });
        }

    }

    private final Comparator<WarehouseStockDetails> comparator;

    private final SortedList<WarehouseStockDetails> sortedList = new SortedList<>(WarehouseStockDetails.class, new SortedList.Callback<WarehouseStockDetails>() {
        @Override
        public int compare(WarehouseStockDetails o1, WarehouseStockDetails o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(WarehouseStockDetails oldItem, WarehouseStockDetails newItem) {
            return oldItem.warehouseStock.equals(newItem.warehouseStock);
        }

        @Override
        public boolean areItemsTheSame(WarehouseStockDetails item1, WarehouseStockDetails item2) {
            return item1.warehouseStock.id == item2.warehouseStock.id;
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

    private final OnItemClickListener onItemClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public WarehouseStockListAdapter(Comparator<WarehouseStockDetails> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WarehouseStockDetails stockDetails = sortedList.get(position);
        if (stockDetails != null) {
            holder.name.setText(stockDetails.productName);
            holder.quantity.setText(String.valueOf(stockDetails.warehouseStock.quantity));
            Date date = new Date(stockDetails.warehouseStock.dateAcquired);
            holder.dateAcquired.setText(dateFormat.format(date));
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public WarehouseStockDetails getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<WarehouseStockDetails> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            WarehouseStockDetails stockDetails = sortedList.get(i);
            if (!list.contains(stockDetails)) sortedList.remove(stockDetails);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
