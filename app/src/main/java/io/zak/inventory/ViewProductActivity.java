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
import io.zak.inventory.data.relations.ProductDetails;

public class ViewProductActivity extends AppCompatActivity {

    private static final String TAG = "ViewProduct";

    // Widgets
    private ImageButton btnClose, btnEdit, btnSelectProfile;
    private ImageView qrCodeView;
    private TextView tvName, tvSupplier, tvBrand, tvCategory, tvPrice, tvDescription, tvCritLevel;
    private ImageView profile;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private ProductDetails productDetails;

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
        qrCodeView = findViewById(R.id.iv_qrcode);
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
            if (productDetails != null) {
                Intent intent = new Intent(this, EditProductActivity.class);
                intent.putExtra("product_id", productDetails.product.productId);
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
            Log.d(TAG, "Fetching product with details.");
            return database.products().getProductWithDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(list -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + list.size());
            productDetails = list.get(0);
            displayInfo(productDetails);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database error: " + err);
            dialogBuilder.setTitle("Database Error").setMessage("Error while retrieving Product entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(ProductDetails productDetails) {
        if (productDetails != null) {
            tvName.setText(productDetails.product.productName);
            tvBrand.setText(productDetails.brand.brandName);
            tvCategory.setText(productDetails.category.categoryName);
            tvPrice.setText(Utils.toStringMoneyFormat(productDetails.product.price));
            tvSupplier.setText(productDetails.supplier.supplierName);
            if (productDetails.product.productDescription != null && !productDetails.product.productDescription.isBlank()) {
                tvDescription.setText(productDetails.product.productDescription);
            }

            Product product = productDetails.product;

            // QR Code
            String str = String.format(Locale.getDefault(),
                    "id=%d;" +
                    "name=%s;" +
                    "brand=%d;" +
                    "category=%d;" +
                    "supplier=%d;" +
                    "crit=%d;" +
                    "price=%.2f;" +
                    "desc=%s",
                    product.productId,
                    product.productName,
                    product.fkBrandId,
                    product.fkCategoryId,
                    product.fkSupplierId,
                    product.criticalLevel,
                    product.price,
                    product.productDescription);

            qrCodeView.setImageBitmap(Utils.generateQrCode(str, 200, 200));
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