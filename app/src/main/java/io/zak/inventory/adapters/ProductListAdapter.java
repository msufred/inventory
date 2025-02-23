package io.zak.inventory.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.zak.inventory.R;
import io.zak.inventory.data.entities.Product;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, price, description;
        private final ImageView imageView;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            price = view.findViewById(R.id.tv_price);
            description = view.findViewById(R.id.tv_description);
            imageView = view.findViewById(R.id.image_view);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> {
                onItemClickListener.onItemClick(getAdapterPosition());
            });
        }

    }

    private final Comparator<Product> comparator;

    private final SortedList<Product> sortedList = new SortedList<>(Product.class, new SortedList.Callback<>() {
        @Override
        public int compare(Product o1, Product o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Product oldItem, Product newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Product item1, Product item2) {
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

    public ProductListAdapter(Comparator<Product> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = sortedList.get(position);
        if (product != null) {
            holder.name.setText(product.name);
            holder.price.setText(String.format(Locale.getDefault(), "Php %.2f", product.price));
            if (!product.description.isBlank()) holder.description.setText(product.description);
            // TODO image view
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void clear() {
        sortedList.clear();
    }

    public Product getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<Product> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Product product = sortedList.get(i);
            if (!list.contains(product)) sortedList.remove(product);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
