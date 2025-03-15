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
import io.zak.inventory.data.entities.DeliveryOrder;

public class DeliveryListAdapter extends RecyclerView.Adapter<DeliveryListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, amount, employee, date, status;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            amount = view.findViewById(R.id.tv_amount);
            employee = view.findViewById(R.id.tv_employee);
            date = view.findViewById(R.id.tv_date);
            status = view.findViewById(R.id.tv_status);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> onItemClickListener.onItemClick(getAdapterPosition()));
        }

    }

    private final Comparator<DeliveryOrder> comparator = Comparator.comparing(deliveryOrder -> deliveryOrder.trackingNo);

    private final SortedList<DeliveryOrder> sortedList = new SortedList<>(DeliveryOrder.class, new SortedList.Callback<>() {
        @Override
        public int compare(DeliveryOrder o1, DeliveryOrder o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(DeliveryOrder oldItem, DeliveryOrder newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(DeliveryOrder item1, DeliveryOrder item2) {
            return item1.deliveryOrderId == item2.deliveryOrderId;
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

    public DeliveryListAdapter(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeliveryOrder deliveryOrder = sortedList.get(position);
        if (deliveryOrder != null) {
            holder.name.setText(String.format("%s (%s)", deliveryOrder.vehicleName, deliveryOrder.vehiclePlateNo));
            holder.amount.setText(Utils.toStringMoneyFormat(deliveryOrder.totalAmount));
            holder.employee.setText(deliveryOrder.userName);
            holder.date.setText(Utils.humanizeDate(new Date(deliveryOrder.deliveryDate)));
            holder.status.setText(deliveryOrder.deliveryOrderStatus);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public DeliveryOrder getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<DeliveryOrder> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            DeliveryOrder order = sortedList.get(i);
            if (!list.contains(order)) sortedList.remove(order);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

    public void clear() {
        sortedList.clear();
    }
}
