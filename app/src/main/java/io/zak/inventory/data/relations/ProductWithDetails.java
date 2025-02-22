package io.zak.inventory.data.relations;

import androidx.room.Embedded;

import io.zak.inventory.data.entities.Product;

public class ProductWithDetails {

    @Embedded public Product product;
    public String brandName;
    public String categoryName;
}
