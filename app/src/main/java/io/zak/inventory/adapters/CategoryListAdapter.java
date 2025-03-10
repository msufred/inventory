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
import io.zak.inventory.data.entities.Category;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.ViewHolder> {

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

    private final Comparator<Category> comparator;
    private boolean selectionMode = false;
    private final Set<Integer> selectedItems = new HashSet<>();

    private final SortedList<Category> sortedList = new SortedList<>(Category.class, new SortedList.Callback<>() {
        @Override
        public int compare(Category o1, Category o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Category oldItem, Category newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Category item1, Category item2) {
            return item1.categoryId == item2.categoryId;
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

    public CategoryListAdapter(Comparator<Category> comparator, OnItemClickListener onItemClickListener, OnItemLongClickListener onItemLongClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = sortedList.get(position);
        if (category != null) {
            holder.name.setText(category.categoryName);

            // Show/hide checkbox based on selection mode
            holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);

            // Set checkbox state based on selection
            holder.checkBox.setChecked(selectedItems.contains(position));

            // Set click listeners here instead of in the ViewHolder constructor
            final int pos = position; // Capture position for use in listeners

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

    public Category getItem(int position) {
        return sortedList.get(position);
    }

    public void addItem(Category category) {
        sortedList.add(category);
    }

    public void updateItem(Category updatedCategory) {
        // Find the position of the category with the same ID
        for (int i = 0; i < sortedList.size(); i++) {
            Category category = sortedList.get(i);
            if (category.categoryId == updatedCategory.categoryId) {
                // Remove the old item and add the updated one
                // SortedList will handle the sorting and notification
                sortedList.beginBatchedUpdates();
                sortedList.remove(category);
                sortedList.add(updatedCategory);
                sortedList.endBatchedUpdates();
                return;
            }
        }
    }

    public void replaceAll(List<Category> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Category category = sortedList.get(i);
            if (!list.contains(category)) sortedList.remove(category);
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

    public List<Category> getSelectedCategories() {
        List<Category> items = new ArrayList<>();
        for (Integer position : selectedItems) {
            if (position < sortedList.size()) {
                items.add(sortedList.get(position));
            }
        }
        return items;
    }
}

