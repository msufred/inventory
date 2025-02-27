package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.entities.Supplier;

public class ViewProductActivity extends AppCompatActivity {

    private static final String TAG = "ViewProduct";

    // Widgets
    private ImageButton btnClose, btnEdit, btnSelectProfile;
    private TextView tvName, tvSupplier, tvBrand, tvCategory, tvPrice, tvDescription, tvCritLevel;
    private ImageView profile;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Product mProduct;
    private Supplier mSupplier;
    private Brand mBrand;
    private Category mCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_product);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnClose = findViewById(R.id.btn_close);
        btnEdit = findViewById(R.id.btn_edit);
        btnSelectProfile = findViewById(R.id.btn_select_profile);
        tvName = findViewById(R.id.tv_name);
        tvSupplier = findViewById(R.id.tv_supplier);
        tvBrand = findViewById(R.id.tv_brand);
        tvCategory = findViewById(R.id.tv_category);
        tvPrice = findViewById(R.id.tv_price);
        tvDescription = findViewById(R.id.tv_description);
        profile = findViewById(R.id.profile);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnClose.setOnClickListener(v -> goBack());
        btnEdit.setOnClickListener(v -> {
            if (mProduct != null) {
                Intent intent = new Intent(this, EditProductActivity.class);
                intent.putExtra("product_id", mProduct.productId);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = getIntent().getIntExtra("product_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Product ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        AppDatabase database = AppDatabaseImpl.getDatabase(getApplicationContext());

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching product with id=" + id + " " + Thread.currentThread());
            return database.products().getProduct(id);
        }).flatMap(products -> {
            mProduct = products.get(0);
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching supplier with id=" + mProduct.fkSupplierId + " " + Thread.currentThread());
                return database.suppliers().getSupplier(mProduct.fkSupplierId);
            });
        }).flatMap(suppliers -> {
            if (!suppliers.isEmpty()) {
                mSupplier = suppliers.get(0);
            }
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching brand with id=" + mProduct.fkBrandId + " " + Thread.currentThread());
                return database.brands().getBrand(mProduct.fkBrandId);
            });
        }).flatMap(brands -> {
            if (!brands.isEmpty()) {
                mBrand = brands.get(0);
            }
            return Single.fromCallable(() -> {
                Log.d(TAG, "Fetching category with id=" + mProduct.fkCategoryId + " " + Thread.currentThread());
                return database.categories().getCategory(mProduct.fkCategoryId);
            });
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(categories -> {
            progressGroup.setVisibility(View.GONE);
            if (!categories.isEmpty()) {
                mCategory = categories.get(0);
            }
            displayInfo();
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Product entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo() {
        if (mProduct != null) {
            tvName.setText(mProduct.productName);

            // Format price with currency symbol
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
            tvPrice.setText(currencyFormat.format(mProduct.price));



            // Set description (handle null or empty)
            String description = mProduct.productDescription;
            tvDescription.setText(description != null && !description.isEmpty() ?
                    description : getString(R.string.no_description));

            // Set supplier, brand, and category names
            tvSupplier.setText(mSupplier != null ? mSupplier.supplierName :
                    getString(R.string.no_suppliers));

            tvBrand.setText(mBrand != null ? mBrand.brandName :
                    getString(R.string.no_brands));

            tvCategory.setText(mCategory != null ? mCategory.categoryName :
                    getString(R.string.no_categories));
        }
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying resources.");
        disposables.dispose();
    }
}