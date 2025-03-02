package io.zak.inventory;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.relations.DeliveryItemDetails;

public class ViewDeliveryOrderItemActivity extends AppCompatActivity {

    private static final String TAG = "ViewDeliveryOrderItem";

    private ImageView qrCode;
    private TextView tvName, tvPrice, tvQuantity, tvTotalAmount;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private DeliveryItemDetails deliveryItemDetails;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_delivery_order_item);

        qrCode = findViewById(R.id.iv_qrcode);
        tvName = findViewById(R.id.tv_product_name);
        tvPrice = findViewById(R.id.tv_price);
        tvQuantity = findViewById(R.id.tv_quantity);
        tvTotalAmount = findViewById(R.id.tv_total_amount);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> goBack());

        disposables = new CompositeDisposable();
        dialogBuilder = new AlertDialog.Builder(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = getIntent().getIntExtra("delivery_order_item_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Delivery Order Item ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        disposables.add(Single.fromCallable(() -> {
            return AppDatabaseImpl.getDatabase(getApplicationContext()).deliveryOrderItems().getDeliveryItemWithDetails(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(orderItemList -> {
            deliveryItemDetails = orderItemList.get(0);
            displayInfo(deliveryItemDetails);
        }, err -> {
            Log.e(TAG, "Database error: " + err);
            goBack();
        }));
    }

    private void displayInfo(DeliveryItemDetails details) {
        tvName.setText(details.product.productName);
        tvPrice.setText(String.format("Php %s", Utils.toStringMoneyFormat(details.product.price)));
        tvQuantity.setText(String.valueOf(details.deliveryOrderItem.quantity));
        tvTotalAmount.setText(String.format("Php %s", Utils.toStringMoneyFormat(details.deliveryOrderItem.subtotal)));

        // QR Code
        Bitmap bitmap = createQrCode(details);
        if (bitmap != null) {
            qrCode.setImageBitmap(bitmap);
        }
    }

    private Bitmap createQrCode(DeliveryItemDetails details) {
        String str = String.format(Locale.getDefault(),
                "id=%d;" +              // item id
                "order_id=%d;" +                // delivery order id
                "stock_id=%d;" +                // warehouse id
                "product_id=%d;" +              // product id
                "name=%s;" +                    // product name
                "price=%.2f;" +                 // product price
                "qty=%d," +                     // quantity
                "subtotal=%.2f",                // total
                details.deliveryOrderItem.deliveryOrderItemId,
                details.deliveryOrderItem.fkDeliveryOrderId,
                details.deliveryOrderItem.fkWarehouseStockId,
                details.deliveryOrderItem.fkProductId,
                details.product.productName,
                details.product.price,
                details.deliveryOrderItem.quantity,
                details.deliveryOrderItem.subtotal);

        return Utils.generateQrCode(str, 300, 300);
    }

    private void goBack() {
        getOnBackPressedDispatcher().onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
