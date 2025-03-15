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
import io.zak.inventory.data.entities.Vehicle;

public class VehicleListAdapter extends RecyclerView.Adapter<VehicleListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, type, plateNo, status;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            type = view.findViewById(R.id.tv_type);
            plateNo = view.findViewById(R.id.tv_plate_no);
            status = view.findViewById(R.id.tv_status);
            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> onItemClickListener.onItemClick(getAdapterPosition()));
        }

    }

    private final Comparator<Vehicle> comparator;

    private final SortedList<Vehicle> sortedList = new SortedList<>(Vehicle.class, new SortedList.Callback<Vehicle>() {
        @Override
        public int compare(Vehicle o1, Vehicle o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Vehicle oldItem, Vehicle newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Vehicle item1, Vehicle item2) {
            return item1.vehicleId == item2.vehicleId;
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

    public VehicleListAdapter(Comparator<Vehicle> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vehicle vehicle = sortedList.get(position);
        if (vehicle != null) {
            holder.name.setText(vehicle.vehicleName);
            holder.type.setText(vehicle.vehicleType);
            if (!vehicle.plateNo.isBlank()) holder.plateNo.setText(vehicle.plateNo);
            holder.status.setText(vehicle.vehicleStatus);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public void clear() {
        sortedList.clear();
    }

    public void addItem(Vehicle vehicle) {
        sortedList.add(vehicle);
    }

    public Vehicle getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<Vehicle> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Vehicle vehicle = sortedList.get(i);
            if (!list.contains(vehicle)) sortedList.remove(vehicle);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
