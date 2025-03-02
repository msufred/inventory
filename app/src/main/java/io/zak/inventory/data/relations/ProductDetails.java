package io.zak.inventory.data.relations;

import androidx.room.Embedded;

import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;
import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.entities.Supplier;

public class ProductDetails {

    @Embedded
    public Product product;

    @Embedded
    public Brand brand;

    @Embedded
    public Category category;

    @Embedded
    public Supplier supplier;
}
