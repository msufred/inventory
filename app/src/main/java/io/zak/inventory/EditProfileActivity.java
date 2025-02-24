package io.zak.inventory;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.zak.inventory.data.AppDatabaseImpl;
import io.zak.inventory.data.entities.User;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfile";

    // Widgets
    private EditText etName, etPosition, etContact, etAddress, etUsername, etPassword, etPasswordConfirm;
    private ImageButton btnBack, btnDelete;
    private Button btnCancel, btnSave;

    private Drawable errorDrawable;

    private CompositeDisposable disposables;
    private AlertDialog.Builder dialogBuilder;

    private User mUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        getWidgets();
        setListeners();
    }

    private void getWidgets() {
        etName = findViewById(R.id.et_name);
        etPosition = findViewById(R.id.et_position);
        etContact = findViewById(R.id.et_contact);
        etAddress = findViewById(R.id.et_address);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        dialogBuilder = new AlertDialog.Builder(this);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> goBack());
        btnCancel.setOnClickListener(v -> goBack());
        btnSave.setOnClickListener(v -> {
            if (validated()) saveAndClose();
        });
        btnDelete.setOnClickListener(v -> {
            if (mUser != null) {
                dialogBuilder.setTitle("Confirm Delete")
                        .setMessage("This will also delete all the data related to this user. " +
                                "Are you sure you want to delete this user entry?")
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            dialog.dismiss();
                            deleteUser();
                        });
                dialogBuilder.create().show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (disposables == null) disposables = new CompositeDisposable();

        // check user id
        int id = Utils.getLoginId(this);
        if (id == -1) {
            dialogBuilder.setTitle("Invalid Action")
                    .setMessage("No User ID found.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Fetching User entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).users().getUser(id);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(users -> {
            Log.d(TAG, "Returned with list size=" + users.size() + " " + Thread.currentThread());
            mUser = users.get(0);
            displayInfo(mUser);
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while fetching User entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void displayInfo(User user) {
        if (user != null) {
            etName.setText(user.fullName);
            etPosition.setText(user.position);
            etContact.setText(user.contactNo);
            etAddress.setText(user.address);
            etUsername.setText(user.username);
            etPassword.setText(user.password);
            etPasswordConfirm.setText(user.password);
        }
    }

    private boolean validated() {
        // clear drawables
        etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        etPassword.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        etPasswordConfirm.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();
        String passwordConfirm = etPasswordConfirm.getText().toString();

        if (username.isBlank()) etUsername.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        if (password.isBlank()) etPassword.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);
        if (passwordConfirm.isBlank()) etPasswordConfirm.setCompoundDrawablesWithIntrinsicBounds(null, null, errorDrawable, null);

        return !username.isBlank() && !password.isBlank() && !passwordConfirm.isBlank() &&
                (passwordConfirm.contentEquals(password));

    }

    private void saveAndClose() {
        mUser.fullName = Utils.normalize(etName.getText().toString());
        mUser.position = Utils.normalize(etPosition.getText().toString());
        mUser.address = Utils.normalize(etAddress.getText().toString());
        mUser.contactNo = Utils.normalize(etContact.getText().toString());
        mUser.username = Utils.normalize(etUsername.getText().toString());
        mUser.password = Utils.normalize(etPassword.getText().toString());

        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Updating User entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).users().update(mUser);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            Log.d(TAG, "Returned with row count=" + rowCount + " " + Thread.currentThread());

            if (rowCount > 0) {
                Toast.makeText(this, "Updated User Details", Toast.LENGTH_SHORT).show();
            }

            goBack();
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while updating User entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
    }

    private void deleteUser() {
        disposables.add(Single.fromCallable(() -> {
            Log.d(TAG, "Deleting User entry: " + Thread.currentThread());
            return AppDatabaseImpl.getDatabase(getApplicationContext()).users().delete(mUser);
        }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(rowCount -> {
            Log.d(TAG, "Returned with row count=" + rowCount + " " + Thread.currentThread());
            if (rowCount > 0) {
                Toast.makeText(this, "Deleted User", Toast.LENGTH_SHORT).show();
                Utils.logout(this); // logout
                // return to Login
                startActivity(new Intent(this, MainActivity.class));
            }
        }, err -> {
            Log.e(TAG, "Database Error: " + err);
            dialogBuilder.setTitle("Database Error")
                    .setMessage("Error while deleting User entry: " + err)
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        goBack();
                    });
            dialogBuilder.create().show();
        }));
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
