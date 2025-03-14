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
import io.zak.inventory.Utils;
import io.zak.inventory.data.entities.OrderItem;
import io.zak.inventory.data.relations.OrderItemDetails;

public class OrderItemListAdapter extends RecyclerView.Adapter<OrderItemListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName, tvPrice, tvQuantity, tvAmount;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            tvName = view.findViewById(R.id.tv_name);
            tvPrice = view.findViewById(R.id.tv_price);
            tvQuantity = view.findViewById(R.id.tv_quantity);
            tvAmount = view.findViewById(R.id.tv_total_amount);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> onItemClickListener.onItemClick(getAdapterPosition()));
        }

    }

    private final Comparator<OrderItemDetails> comparator;

    private final SortedList<OrderItemDetails> sortedList = new SortedList<>(OrderItemDetails.class, new SortedList.Callback<>() {
        @Override
        public int compare(OrderItemDetails o1, OrderItemDetails o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(OrderItemDetails oldItem, OrderItemDetails newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(OrderItemDetails item1, OrderItemDetails item2) {
            return item1.orderItem.orderItemId == item2.orderItem.orderItemId;
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

    public OrderItemListAdapter(Comparator<OrderItemDetails> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_item, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItemDetails details = sortedList.get(position);
        if (details != null) {
            holder.tvName.setText(details.product.productName);
            holder.tvPrice.setText(Utils.toStringMoneyFormat(details.orderItem.sellingPrice));
            holder.tvQuantity.setText(String.valueOf(details.orderItem.quantity));
            holder.tvAmount.setText(Utils.toStringMoneyFormat(details.orderItem.subtotal));
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public OrderItemDetails getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<OrderItemDetails> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            OrderItemDetails details = sortedList.get(i);
            if (!list.contains(details)) sortedList.remove(details);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
