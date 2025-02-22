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
import io.zak.inventory.data.entities.Brand;

public class BrandListAdapter extends RecyclerView.Adapter<BrandListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> {
                onItemClickListener.onItemClick(getAdapterPosition());
            });
        }

    }

    private final Comparator<Brand> comparator;

    private final SortedList<Brand> sortedList = new SortedList<>(Brand.class, new SortedList.Callback<Brand>() {
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

    private final OnItemClickListener onItemClickListener;

    public BrandListAdapter(Comparator<Brand> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Brand brand = sortedList.get(position);
        if (brand != null) {
            holder.name.setText(brand.name);
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
}
