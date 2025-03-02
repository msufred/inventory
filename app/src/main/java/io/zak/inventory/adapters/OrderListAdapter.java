package io.zak.inventory.adapters;

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
import io.zak.inventory.Utils;
import io.zak.inventory.data.relations.OrderDetails;

public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView orNo, vehicle, amount, date, status;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            orNo = view.findViewById(R.id.tv_orno);
            vehicle = view.findViewById(R.id.tv_vehicle);
            amount = view.findViewById(R.id.tv_amount);
            date = view.findViewById(R.id.tv_date);
            status = view.findViewById(R.id.tv_status);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> onItemClickListener.onItemClick(getAdapterPosition()));
        }

    }

    private final Comparator<OrderDetails> comparator;

    private final SortedList<OrderDetails> sortedList = new SortedList<>(OrderDetails.class, new SortedList.Callback<>() {
        @Override
        public int compare(OrderDetails o1, OrderDetails o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(OrderDetails oldItem, OrderDetails newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(OrderDetails item1, OrderDetails item2) {
            return item1.order.orderId == item2.order.orderId;
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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public OrderListAdapter(Comparator<OrderDetails> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderDetails details = sortedList.get(position);
        if (details != null) {
            holder.orNo.setText(details.order.orNo);
            holder.vehicle.setText(String.format("%s (%s)", details.vehicle.vehicleName, details.vehicle.plateNo));
            holder.amount.setText(Utils.toStringMoneyFormat(details.order.totalAmount));
            holder.date.setText(Utils.humanizeDate(new Date(details.order.dateOrdered)));
            holder.status.setText(details.order.orderStatus);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public OrderDetails getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<OrderDetails> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            OrderDetails details = sortedList.get(i);
            if (!list.contains(details)) sortedList.remove(details);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

    public void clear() {
        sortedList.clear();
    }
}
