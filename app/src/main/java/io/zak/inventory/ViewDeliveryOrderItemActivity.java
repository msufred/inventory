package io.zak.inventory;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabase;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.relations.DeliveryItemDetails;

public class ViewDeliveryOrderItemActivity extends AppCompatActivity {

    private static final String TAG = "ViewDeliveryOrderItem";

    private ImageView qrCode;
    private TextView tvName, tvPrice, tvQuantity, tvTotalAmount;
    private ImageButton btnBack;

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
        btnBack = findViewById(R.id.btn_back);

        // set listeners
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

        Bitmap bitmap = null;
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix matrix = qrCodeWriter.encode(str, BarcodeFormat.QR_CODE, 300, 300);

            int w = matrix.getWidth();
            int h = matrix.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pixels[y * w + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        } catch (WriterException e) {
            Log.e(TAG, "Failed to write QR Code: " + e);
        }
        return bitmap;
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
