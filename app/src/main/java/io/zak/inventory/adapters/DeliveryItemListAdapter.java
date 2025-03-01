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
import io.zak.inventory.data.relations.DeliveryItemDetails;

public class DeliveryItemListAdapter extends RecyclerView.Adapter<DeliveryItemListAdapter.ViewHolder> {

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

    private final Comparator<DeliveryItemDetails> comparator;

    private final SortedList<DeliveryItemDetails> sortedList = new SortedList<>(DeliveryItemDetails.class, new SortedList.Callback<>() {
        @Override
        public int compare(DeliveryItemDetails o1, DeliveryItemDetails o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(DeliveryItemDetails oldItem, DeliveryItemDetails newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(DeliveryItemDetails item1, DeliveryItemDetails item2) {
            return item1.deliveryOrderItem.deliveryOrderItemId == item2.deliveryOrderItem.deliveryOrderItemId;
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

    public DeliveryItemListAdapter(Comparator<DeliveryItemDetails> comparator, OnItemClickListener onItemClickListener) {
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
        DeliveryItemDetails details = sortedList.get(position);
        if (details != null) {
            holder.tvName.setText(details.product.productName);
            holder.tvPrice.setText(Utils.toStringMoneyFormat(details.product.price));
            holder.tvQuantity.setText(String.valueOf(details.deliveryOrderItem.quantity));
            holder.tvAmount.setText(Utils.toStringMoneyFormat(details.deliveryOrderItem.subtotal));
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public DeliveryItemDetails getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<DeliveryItemDetails> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            DeliveryItemDetails details = sortedList.get(i);
            if (!list.contains(details)) sortedList.remove(details);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

}
