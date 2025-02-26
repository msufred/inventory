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
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.zak.inventory.R;
import io.zak.inventory.Utils;
import io.zak.inventory.data.relations.DeliveryDetails;

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

    private final Comparator<DeliveryDetails> comparator;

    private final SortedList<DeliveryDetails> sortedList = new SortedList<>(DeliveryDetails.class, new SortedList.Callback<>() {
        @Override
        public int compare(DeliveryDetails o1, DeliveryDetails o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(DeliveryDetails oldItem, DeliveryDetails newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(DeliveryDetails item1, DeliveryDetails item2) {
            return item1.deliveryOrder.id == item2.deliveryOrder.id;
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

    public DeliveryListAdapter(Comparator<DeliveryDetails> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
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
        DeliveryDetails details = sortedList.get(position);
        if (details != null) {
            holder.name.setText(String.format("%s (%s)", details.vehicleName, details.plateNo));
            holder.amount.setText(Utils.toStringMoneyFormat(details.deliveryOrder.totalAmount));
            holder.employee.setText(details.deliveryOrder.employeeName);
            holder.date.setText(Utils.humanizeDate(new Date(details.deliveryOrder.dateOrdered)));
            holder.status.setText(details.deliveryOrder.status);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public DeliveryDetails getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<DeliveryDetails> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            DeliveryDetails details = sortedList.get(i);
            if (!list.contains(details)) sortedList.remove(details);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

    public void clear() {
        sortedList.clear();
    }
}
