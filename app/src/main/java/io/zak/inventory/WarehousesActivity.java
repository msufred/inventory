package io.zak.inventory;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class WarehousesActivity extends AppCompatActivity {

    private static final String DEBUG_NAME = "Warehouses";

    private EditText etSearch;
    private RecyclerView recyclerView;
    private Button btnBack, btnAdd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warehouses);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etSearch = findViewById(R.id.et_search);
        recyclerView = findViewById(R.id.recycler_view);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);

        // set layout manager and adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setListeners() {
        etSearch.setOnKeyListener((v, keyCode, event) -> {
            // TODO search items
            Log.d(DEBUG_NAME, etSearch.getText().toString());
            return false;
        });
        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
            finish();
        });
    }
}
