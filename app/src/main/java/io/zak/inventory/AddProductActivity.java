package io.zak.inventory;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.adapters.BrandSpinnerAdapter;
import io.zak.inventory.adapters.CategorySpinnerAdapter;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.data.entities.Product;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProduct";

    // Widgets
    private EditText etName, etPrice, etDescription;
    private Spinner brandSpinner, categorySpinner;
    private ImageButton btnBack;
    private Button btnCancel, btnSave;
    private RelativeLayout progressGroup;

    private Drawable errorDrawable;

    // list references to Brands and Categories
    private List<Brand> brandList;
    private List<Category> categoryList;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Brand mSelectedBrand;
    private Category mSelectedCategory;

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etPrice = findViewById(R.id.et_price);
        etDescription = findViewById(R.id.et_description);
        brandSpinner = findViewById(R.id.brand_spinner);
        categorySpinner = findViewById(R.id.category_spinner);
        btnBack = findViewById(R.id.btn_back);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);

        errorDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_x_circle);
    }

    private void setListeners() {
        brandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (brandList != null && !brandList.isEmpty()) {
                    mSelectedBrand = brandList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (categoryList != null && !categoryList.isEmpty()) {
                    mSelectedCategory = categoryList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // empty
            }
        });

        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching Brand entries: " + Thread.currentThread());
            return  database.brands().getAll();
        }).flatMap(brands -> {
            brandList = brands;
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching Category entries: " + Thread.currentThread());
                return  database.categories().getAll();
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(categories -> {
            Log.d(TAG, "Returned with category size: " +categories.size() + " " + Thread.currentThread());
            categoryList = categories;
            setupSpinners();
            progressGroup.setVisibility(View.GONE);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Brand and Category entries: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> dialog.dismiss()));
            dialogBuilder.create().show();
        }));
    }

    private void setupSpinners() {
        if (brandList == null || categoryList == null) {
            dialogBuilder.setTitle("Error")
                    .setMessage("Unable to display Brands and Categories")
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
            return;
        }

        BrandSpinnerAdapter brandSpinnerAdapter = new BrandSpinnerAdapter(this, brandList);
        brandSpinner.setAdapter(brandSpinnerAdapter);
        CategorySpinnerAdapter categorySpinnerAdapter = new CategorySpinnerAdapter(this, categoryList);
        categorySpinner.setAdapter(categorySpinnerAdapter);
    }

    private boolean validated() {
        // clear drawables
        etName.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        etPrice.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        String name = etName.getText().toString();
        String price = etPrice.getText().toString();

        if (name.isBlank()) {
            etName.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        }

        if (price.isBlank()) {
            etPrice.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        }

        return !name.isBlank() && !price.isBlank();
    }

    private void saveAndClose() {
        if (mSelectedBrand == null && mSelectedCategory == null) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("No selected Brand and/or Category")
                    .setPositiveButton("OK", ((dialog, which) -> dialog.dismiss()));
            dialogBuilder.create().show();
            return;
        }

        Product product = new Product();
        product.name = Utils.normalize(etName.getText().toString());
        product.brandId = mSelectedBrand.id;
        product.categoryId = mSelectedCategory.id;
        product.price = Double.parseDouble(etPrice.getText().toString().trim());
        product.description = Utils.normalize(etDescription.getText().toString());

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Saving Product entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).products().insert(product);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(id -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with ID:" + id + " " + Thread.currentThread());
            goBack();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);

            // dialog
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while saving Product entry: " + err)
                    .setPositiveButton("OK", ((dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    }));
            dialogBuilder.create().show();
        }));
    }

    private void goBack() {
        getOnBackPressedDispatcher();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}
