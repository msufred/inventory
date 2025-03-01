package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.ConsumerListAdapter;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Consumer;

public class ConsumersActivity extends AppCompatActivity implements ConsumerListAdapter.OnItemClickListener {

    private static final String TAG = "Consumers";

    // Widgets
    private SearchView searchView;
    private RecyclerView recyclerView;
    private TextView tvNoConsumers;
    private ImageView btnBack;
    private Button btnAdd;
    private RelativeLayout progressGroup;

    // RecyclerView adapter
    private ConsumerListAdapter adapter;

    // list reference for search filter
    private List<Consumer> consumerList;

    // Comparator used for search filter; passed to SupplierListAdapter
    private final Comparator<Consumer> comparator = Comparator.comparing(consumer ->consumer.consumerName);

    private CompositeDisposable disposables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumers);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recycler_view);
        tvNoConsumers = findViewById(R.id.tv_no_consumers);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);
        progressGroup = findViewById(R.id.progress_group);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConsumerListAdapter(comparator, this);
        recyclerView.setAdapter(adapter);
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

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            finish();
        });

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddConsumerActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Consumer entries: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).consumers().getAll();
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            Log.d(TAG, "Fetched " + list.size() + " items: " + Thread.currentThread());
            progressGroup.setVisibility(View.GONE);
            consumerList = list;
            adapter.replaceAll(list);
            tvNoConsumers.setVisibility(list.isEmpty() ? View.VISIBLE : View.INVISIBLE);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            progressGroup.setVisibility(View.GONE);
        }));
    }

    @Override
    public void onItemClick(int position) {
        if (adapter != null) {
            Consumer consumer = adapter.getItem(position);
            if (consumer != null) {
                Log.d(TAG, "Consumer selected: " + consumer.consumerName);
                Intent intent = new Intent(this, ViewConsumerActivity.class);
                intent.putExtra("consumer_id", consumer.consumerId);
                startActivity(intent);
            }
        }
    }

    private void onSearch(String query) {
        List<Consumer> filteredList = filter(consumerList, query);
        adapter.replaceAll(filteredList);
        recyclerView.scrollToPosition(0);
    }

    private List<Consumer> filter(List<Consumer> consumers, String query) {
        String str = query.toLowerCase();
        List<Consumer> list = new ArrayList<>();
        for (Consumer consumer : consumers) {
            if (consumer.consumerName.toLowerCase().contains(str)) {
                list.add(consumer);
            }
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
