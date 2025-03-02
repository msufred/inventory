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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.Consumer;

public class ViewConsumerActivity extends AppCompatActivity {

    private static final String TAG = "ViewConsumer";

    // Widgets
    private ImageButton btnClose, btnEdit, btnSelectProfile;
    private TextView tvName, tvContactNo, tvEmail, tvAddress;
    private ImageView profile;
    private RelativeLayout progressGroup;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private Consumer mConsumer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_consumer);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        btnClose = findViewById(R.id.btn_close);
        btnEdit = findViewById(R.id.btn_edit);
        btnSelectProfile = findViewById(R.id.btn_select_profile);
        tvName = findViewById(R.id.tv_name);
        tvContactNo = findViewById(R.id.tv_contact_no);
        tvEmail = findViewById(R.id.tv_email);
        tvAddress = findViewById(R.id.tv_address);
        profile = findViewById(R.id.profile);
        progressGroup = findViewById(R.id.progress_group);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnClose.setOnClickListener(v -> goBack());
        btnEdit.setOnClickListener(v -> {
            if (mConsumer != null) {
                Intent intent = new Intent(this, EditConsumerActivity.class);
                intent.putExtra("consumer_id", mConsumer.consumerId);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        int id = getIntent().getIntExtra("consumer_id", -1);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("Invalid Consumer ID")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
            return;
        }

        progressGroup.setVisibility(View.VISIBLE);
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching consumer with id=" + id + " " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).consumers().getConsumer(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(consumers -> {
            progressGroup.setVisibility(View.GONE);
            Log.d(TAG, "Returned with list size=" + consumers.size() + " " + Thread.currentThread());
            mConsumer = consumers.get(0);
            displayInfo(mConsumer);
        }, err -> {
            progressGroup.setVisibility(View.GONE);
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching Employee entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(Consumer consumer) {
        if (consumer != null) {
            tvName.setText(consumer.consumerName);
            tvContactNo.setText(consumer.consumerContactNo);
            if(consumer.consumerEmail == null){
                tvEmail.setText(R.string.no_email);
            } else{
                tvEmail.setText(consumer.consumerEmail);
            }
            tvAddress.setText(consumer.consumerAddress);

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
