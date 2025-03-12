package io.zak.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.zak.inventory.firebase.UserEntry;

public class ProfileFragment extends Fragment {

    private static final String TAG = "Profile";

    // Widgets
    private TextView tvUsername, tvPosition, tvContact, tvEmail, tvAddress;
    private ImageButton btnEdit;
    private Button btnLogout;
    private RelativeLayout progressGroup;

    private AlertDialog.Builder dialogBuilder;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private UserEntry mUserEntry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        getWidgets(view);
        setListeners();
        mAuth = FirebaseAuth.getInstance();
        return view;
    }

    private void getWidgets(View view) {
        tvUsername = view.findViewById(R.id.tv_username);
        tvPosition = view.findViewById(R.id.tv_position);
        tvContact = view.findViewById(R.id.tv_contact);
        tvAddress = view.findViewById(R.id.tv_address);
        tvEmail = view.findViewById(R.id.tv_email);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnLogout = view.findViewById(R.id.btn_logout);
        progressGroup = view.findViewById(R.id.progress_group);
        dialogBuilder = new AlertDialog.Builder(requireActivity());
    }

    private void setListeners() {
        btnEdit.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), EditProfileActivity.class));
        });
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDatabase == null) mDatabase = FirebaseDatabase.getInstance().getReference(); // root
        progressGroup.setVisibility(View.VISIBLE);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mDatabase.child("users")
                    .child(user.getUid())
                    .get()
                    .addOnCompleteListener(requireActivity(), task -> {
                        progressGroup.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            mUserEntry = task.getResult().getValue(UserEntry.class);
                            displayInfo(mUserEntry);
                        }
                    });
        }
    }

    private void displayInfo(UserEntry user) {
        Log.d(TAG, String.valueOf(user == null));
        if (user != null) {
            tvUsername.setText(user.fullName);
            if (user.position != null) tvPosition.setText(user.position);
            if (user.address != null) tvAddress.setText(user.address);
            if (user.email != null) tvEmail.setText(user.email);
            if (user.contactNo != null) tvContact.setText(user.contactNo);
        }
    }

    private void logout() {
        dialogBuilder.setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Confirm", (dialog, which) -> {
                    dialog.dismiss();
                    mAuth.signOut(); // sign out
                    Log.d(TAG, "User signed out");
                    startActivity(new Intent(getActivity(), MainActivity.class));
                });
        dialogBuilder.create().show();
    }
}
