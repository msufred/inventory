package io.zak.inventory.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.zak.inventory.R;
import io.zak.inventory.data.entities.Brand;

public class BrandListAdapter extends RecyclerView.Adapter<BrandListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView name;
        public final LinearLayout layout;
        public final CheckBox checkBox;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            layout = view.findViewById(R.id.layout);
            checkBox = view.findViewById(R.id.checkbox);
        }
    }

    private final Comparator<Brand> comparator;
    private boolean selectionMode = false;
    private final Set<Integer> selectedItems = new HashSet<>();

    private final SortedList<Brand> sortedList = new SortedList<>(Brand.class, new SortedList.Callback<>() {
        @Override
        public int compare(Brand o1, Brand o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Brand oldItem, Brand newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Brand item1, Brand item2) {
            return item1.brandId == item2.brandId;
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
    private final OnItemLongClickListener onItemLongClickListener;

    public BrandListAdapter(Comparator<Brand> comparator, OnItemClickListener onItemClickListener, OnItemLongClickListener onItemLongClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Brand brand = sortedList.get(position);
        if (brand != null) {
            holder.name.setText(brand.brandName);

            holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);

            holder.checkBox.setChecked(selectedItems.contains(position));

            final int pos = position;

            holder.layout.setOnClickListener(v -> {
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(pos);
                }
            });

            holder.layout.setOnLongClickListener(v -> {
                if (pos != RecyclerView.NO_POSITION) {
                    return onItemLongClickListener.onItemLongClick(pos);
                }
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public Brand getItem(int position) {
        return sortedList.get(position);
    }

    public void addItem(Brand brand) {
        sortedList.add(brand);
    }

    public void updateItem(Brand updatedBrand) {
        // Find the position of the brand with the same ID
        for (int i = 0; i < sortedList.size(); i++) {
            Brand brand = sortedList.get(i);
            if (brand.brandId == updatedBrand.brandId) {
                // Remove the old item and add the updated one
                // SortedList will handle the sorting and notification
                sortedList.beginBatchedUpdates();
                sortedList.remove(brand);
                sortedList.add(updatedBrand);
                sortedList.endBatchedUpdates();
                return;
            }
        }
    }

    public void replaceAll(List<Brand> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Brand brand = sortedList.get(i);
            if (!list.contains(brand)) sortedList.remove(brand);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

    public void clear() {
        sortedList.clear();
    }

    // Selection mode methods
    public void enterSelectionMode() {
        selectionMode = true;
        notifyDataSetChanged();
    }

    public void exitSelectionMode() {
        selectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public boolean isInSelectionMode() {
        return selectionMode;
    }

    public void toggleSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }
        notifyItemChanged(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Brand> getSelectedBrands() {
        List<Brand> items = new ArrayList<>();
        for (Integer position : selectedItems) {
            if (position < sortedList.size()) {
                items.add(sortedList.get(position));
            }
        }
        return items;
    }
}

