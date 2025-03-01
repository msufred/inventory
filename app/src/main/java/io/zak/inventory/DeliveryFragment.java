package io.zak.inventory;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.DeliveryListAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.relations.DeliveryDetails;

public class DeliveryFragment extends Fragment implements DeliveryListAdapter.OnItemClickListener {

    private static final String TAG = "Deliveries";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoDeliveries;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private DeliveryListAdapter adapter;
    private List<DeliveryDetails> deliveryList;

    private final Comparator<DeliveryDetails> comparator =
            Comparator.comparing(deliveryDetails -> deliveryDetails.deliveryOrder.deliveryDate);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delivery, container, false);
        getWidgets(view);
        setListeners();
        return view;
    }

    private void getWidgets(View view) {
        searchView = view.findViewById(R.id.search_view);
        recyclerView = view.findViewById(R.id.recycler_view);
        tvNoDeliveries = view.findViewById(R.id.tv_no_deliveries);
        btnAdd = view.findViewById(R.id.btn_add);
        progressGroup = view.findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new DeliveryListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);

        // Set up ItemTouchHelper for swipe-to-delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);

        dialogBuilder = new AlertDialog.Builder(requireActivity());
    }

    private void setListeners() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onSearch(newText);
                return false;
            }
        });

        btnAdd.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddDeliveryOrderActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (disposables == null) disposables = new CompositeDisposable();
        refresh();
    }

    public void refresh() {
        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getActivity());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching DeliveryOrder entries: " + Thread.currentThread());
            return database.deliveryOrders().getDeliveryOrdersWithDetails();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + list.size() + " " + Thread.currentThread());
            deliveryList = list;
            adapter.replaceAll(deliveryList);
            tvNoDeliveries.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching DeliveryOrder items: " + err)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            DeliveryDetails deliveryDetails = adapter.getItem(position);
            if (deliveryDetails != null) {
                viewDeliveryItems(deliveryDetails.deliveryOrder.deliveryOrderId);
            }
        }
    }

    private void onSearch(String query) {
        List<DeliveryDetails> filteredList = filter(deliveryList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<DeliveryDetails> filter(List<DeliveryDetails> deliveryDetailsList, String query) {
        String str = query.toLowerCase();
        List<DeliveryDetails> list = new ArrayList<>();
        for (DeliveryDetails deliveryDetails : deliveryDetailsList) {
            if (deliveryDetails.vehicle.vehicleName.toLowerCase().contains(str)) {
                list.add(deliveryDetails);
            }
        }
        return list;
    }

    private void viewDeliveryItems(int id) {
        Intent intent = new Intent(getActivity(), ViewDeliveryOrderActivity.class);
        intent.putExtra("delivery_id", id);
        startActivity(intent);
    }

    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private final Paint paint;
        private final Drawable deleteIcon;
        private final int iconMargin;
        private final int iconSize;
        private final float swipeThreshold = 0.3f;

        public SwipeToDeleteCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            paint = new Paint();
            deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash);
            if (deleteIcon != null) {
                DrawableCompat.setTint(deleteIcon, Color.WHITE);
            }
            iconMargin = (int) requireContext().getResources().getDimension(R.dimen.fab_margin);
            iconSize = (int) requireContext().getResources().getDimension(R.dimen.icon_size);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            DeliveryDetails item = adapter.getItem(position);
            if (item != null) {
                deleteDeliveryOrder(item);
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            float height = (float) itemView.getBottom() - (float) itemView.getTop();

            // Draw red background
            paint.setColor(Color.RED);
            RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
            c.drawRect(background, paint);

            // Draw delete icon
            if (deleteIcon != null) {
                int deleteIconTop = itemView.getTop() + (int) ((height - iconSize) / 2);
                int deleteIconMargin = (int) ((height - iconSize) / 2);
                int deleteIconLeft = itemView.getRight() - deleteIconMargin - iconSize;
                int deleteIconRight = itemView.getRight() - deleteIconMargin;
                int deleteIconBottom = deleteIconTop + iconSize;

                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
                deleteIcon.draw(c);
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return swipeThreshold;
        }
    }

    private void deleteDeliveryOrder(DeliveryDetails item) {
        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getActivity());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting DeliveryOrder entry");
            return database.deliveryOrders().delete(item.deliveryOrder);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(deletedRows -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Deleted rows: " + deletedRows);
            if (deletedRows > 0) {
                deliveryList.remove(item);
                adapter.replaceAll(deliveryList);
                tvNoDeliveries.setVisibility(deliveryList.isEmpty() ? View.VISIBLE : View.INVISIBLE);
                Toast.makeText(getContext(), "Delivery deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete delivery", Toast.LENGTH_SHORT).show();
                // Restore the item in the RecyclerView
                adapter.notifyDataSetChanged();
            }
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            Toast.makeText(getContext(), "Error deleting delivery: " + err.getMessage(), Toast.LENGTH_LONG).show();
            // Restore the item in the RecyclerView
            adapter.notifyDataSetChanged();
        }));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposables != null) {
            disposables.dispose();
        }
    }
}

