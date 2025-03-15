package io.zak.inventory.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.Comparator;
import java.util.List;

import io.zak.inventory.firebase.UserEntry;
import io.zak.inventory.R;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, position, license;
        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tv_name);
            position = view.findViewById(R.id.tv_position);
            license = view.findViewById(R.id.tv_license);
        }
    }

    private final Comparator<UserEntry> comparator = Comparator.comparing(userEntry -> userEntry.fullName);

    private final SortedList<UserEntry> sortedList = new SortedList<>(UserEntry.class, new SortedList.Callback<UserEntry>() {
        @Override
        public int compare(UserEntry o1, UserEntry o2) {
            return comparator.compare(o1, o2);
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(UserEntry oldItem, UserEntry newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(UserEntry item1, UserEntry item2) {
            return item1.uid.equals(item2.uid);
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

    public UserListAdapter() {

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserEntry entry = sortedList.get(position);
        if (entry != null) {
            holder.name.setText(entry.fullName);
            holder.position.setText(entry.position);
            if (entry.license != null && !entry.license.isBlank()) {
                holder.license.setText(entry.license);
            }
        }
    }

    @Override
    public int getItemCount() {
        return sortedList.size();
    }

    public UserEntry getItem(int position) {
        return sortedList.get(position);
    }

    public void replaceAll(List<UserEntry> list) {
        sortedList.beginBatchedUpdates();
        for (int i = sortedList.size() - 1; i >=0; i--) {
            UserEntry entry = sortedList.get(i);
            if (!list.contains(entry)) sortedList.remove(entry);
        }
        sortedList.addAll(list);
        sortedList.endBatchedUpdates();
    }
}
