package io.zak.inventory.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import io.zak.inventory.Utils;
import io.zak.inventory.data.entities.Product;

public class ProductSpinnerAdapter extends ArrayAdapter<Product> {

    private final Context context;
    private final List<Product> productList;

    public ProductSpinnerAdapter(Context context, List<Product> productList) {
        super(context, android.R.layout.simple_spinner_item, productList);
        this.context = context;
        this.productList = productList;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createCustomView(position, convertView, parent);
    }

    private View createCustomView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }

        Product product = productList.get(position);
        String text = String.format("%s - Php %s", product.productName, Utils.toStringMoneyFormat(product.price));
        TextView textView = view.findViewById(android.R.id.text1);
        textView.setText(text);
        return view;
    }
}
