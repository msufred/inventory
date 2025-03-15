package io.zak.inventory.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.Comparator;
import java.util.List;

import io.zak.inventory.R;
import io.zak.inventory.data.entities.Supplier;

public class SupplierListAdapter extends RecyclerView.Adapter<SupplierListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, contact, address;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            contact = view.findViewById(R.id.tv_contact);
            address = view.findViewById(R.id.tv_address);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> {
                onItemClickListener.onItemClick(getAdapterPosition());
            });
        }

    }

    private final Comparator<Supplier> comparator;

    private final SortedList<Supplier> sortedList = new SortedList<>(Supplier.class, new SortedList.Callback<Supplier>() {
        @Override
        public int compare(Supplier o1, Supplier o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Supplier oldItem, Supplier newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Supplier item1, Supplier item2) {
            return item1.supplierId == item2.supplierId;
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

    public SupplierListAdapter(Comparator<Supplier> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supplier, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Supplier supplier = sortedList.get(position);
        if (supplier != null) {
            holder.name.setText(supplier.supplierName);
            if (!supplier.supplierContactNo.isBlank()) holder.contact.setText(supplier.supplierContactNo);
            if (!supplier.supplierAddress.isBlank()) holder.address.setText(supplier.supplierAddress);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void clear() {
        sortedList.clear();
    }

    public void addItem(Supplier supplier) {
        sortedList.add(supplier);
    }

    public Supplier getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<Supplier> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Supplier supplier = sortedList.get(i);
            if (!list.contains(supplier)) sortedList.remove(supplier);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
