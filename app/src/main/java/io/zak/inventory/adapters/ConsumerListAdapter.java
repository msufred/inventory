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
import io.zak.inventory.data.entities.Consumer;

public class ConsumerListAdapter extends RecyclerView.Adapter<ConsumerListAdapter.ViewHolder> {

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

    private final Comparator<Consumer> comparator;

    private final SortedList<Consumer> sortedList = new SortedList<>(Consumer.class, new SortedList.Callback<>() {
        @Override
        public int compare(Consumer o1, Consumer o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Consumer oldItem, Consumer newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Consumer item1, Consumer item2) {
            return item1.consumerId == item2.consumerId;
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

    public ConsumerListAdapter(Comparator<Consumer> comparator, OnItemClickListener onItemClickListener) {
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
        Consumer consumer = sortedList.get(position);
        if (consumer != null) {
            holder.name.setText(consumer.consumerName);
            if (!consumer.consumerContactNo.isBlank()) holder.contact.setText(consumer.consumerContactNo);
            if (!consumer.consumerAddress.isBlank()) holder.address.setText(consumer.consumerAddress);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void clear() {
        sortedList.clear();
    }

    public Consumer getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<Consumer> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Consumer consumer = sortedList.get(i);
            if (!list.contains(consumer)) sortedList.remove(consumer);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
