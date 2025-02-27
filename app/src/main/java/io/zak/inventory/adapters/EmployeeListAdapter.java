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
import io.zak.inventory.data.entities.Employee;

public class EmployeeListAdapter extends RecyclerView.Adapter<EmployeeListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, position, status;

        public ViewHolder(View view, OnItemClickListener onClickListener) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            position = view.findViewById(R.id.tv_position);
            status = view.findViewById(R.id.tv_status);

            LinearLayout layout = view.findViewById(R.id.layout);
            layout.setOnClickListener(v -> {
                int position = getAdapterPosition();
                onClickListener.onItemClick(position);
            });
        }
    }

    private final Comparator<Employee> comparator;

    private final SortedList<Employee> sortedList = new SortedList<>(Employee.class, new SortedList.Callback<Employee>() {
        @Override
        public int compare(Employee o1, Employee o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(Employee oldItem, Employee newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Employee item1, Employee item2) {
            return item1.employeeId == item2.employeeId;
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

    public EmployeeListAdapter(Comparator<Employee> comparator, OnItemClickListener onItemClickListener) {
        this.comparator = comparator;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee employee = sortedList.get(position);
        if (employee != null) {
            holder.name.setText(employee.employeeName);
            holder.position.setText(employee.position);
            holder.status.setText(employee.employeeStatus);
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public Employee getItem(int position) {
        return sortedList.get(position);
    }

    public void addItem(Employee employee) {
        sortedList.add(employee);
    }

    public void addAll(List<Employee> list) {
        sortedList.addAll(list);
    }

    public void removeItem(Employee employee) {
        sortedList.remove(employee);
    }

    public void replaceAll(List<Employee> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >= 0; i--) {
            Employee employee = sortedList.get(i);
            if (!list.contains(employee)) sortedList.remove(employee);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }

    public void clear() {
        sortedList.clear();
    }
}
