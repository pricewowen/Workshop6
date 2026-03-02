package com.example.workshop6.data.db;

import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Product;

public class DatabaseSeeder {
    /**
     * calls all methods to seed the database
     * @param db database
     */
    public static void seed(AppDatabase db) {
        seedCategories(db);
        seedProducts(db);
    }

    /**
     * Populate the category table
     * @param db database
     */
    private static void seedCategories(AppDatabase db) {
        if (!db.categoryDao().getAllCategories().isEmpty()) {
            return;
        }

        db.categoryDao().insert(new Category(1, "Bread"));
        db.categoryDao().insert(new Category(2, "Cake"));
        db.categoryDao().insert(new Category(3, "Pastry"));
        db.categoryDao().insert(new Category(4, "Cookie"));
        db.categoryDao().insert(new Category(5, "Gluten-Free"));
        db.categoryDao().insert(new Category(6, "Dairy-Free"));
        db.categoryDao().insert(new Category(7, "Seasonal"));
        db.categoryDao().insert(new Category(8, "Vegan"));
        db.categoryDao().insert(new Category(9, "Breakfast"));
        db.categoryDao().insert(new Category(10, "Dessert"));
        db.categoryDao().insert(new Category(11, "Nut-Free"));
        db.categoryDao().insert(new Category(12, "Whole Grain"));
    }

    private static void seedProducts(AppDatabase db) {
        if (!db.productDao().getAllProducts().isEmpty()) {
            return;
        }

        db.productDao().insert(new Product(1, "Sourdough Loaf", "Naturally leavened sourdough bread", 6.49));
        db.productDao().insert(new Product(2, "Multigrain Sandwich Bread", "Whole grain sandwich loaf", 5.99));
        db.productDao().insert(new Product(3, "Baguette", "Classic French-style baguette", 3.49));
        db.productDao().insert(new Product(4, "Cinnamon Roll", "Soft roll with cinnamon filling and glaze", 4.25));
        db.productDao().insert(new Product(5, "Butter Croissant", "Flaky butter croissant", 3.95));
        db.productDao().insert(new Product(6, "Blueberry Muffin", "Muffin with blueberries", 3.25));
        db.productDao().insert(new Product(7, "Banana Bread Slice", "Moist banana bread slice", 2.95));
        db.productDao().insert(new Product(8, "Chocolate Chip Cookie", "Cookie with chocolate chips", 2.25));
        db.productDao().insert(new Product(9, "Oatmeal Raisin Cookie", "Oatmeal cookie with raisins", 2.25));
        db.productDao().insert(new Product(10, "Vanilla Cupcake", "Vanilla cupcake with buttercream", 3.50));
        db.productDao().insert(new Product(11, "Chocolate Cupcake", "Chocolate cupcake with buttercream", 3.50));
        db.productDao().insert(new Product(12, "Carrot Cake Slice", "Carrot cake slice with cream cheese icing", 6.95));
        db.productDao().insert(new Product(13, "Chocolate Layer Cake", "Chocolate cake with ganache", 29.99));
        db.productDao().insert(new Product(14, "Cheesecake Slice", "Classic cheesecake slice", 7.25));
        db.productDao().insert(new Product(15, "Apple Turnover", "Puff pastry turnover with apple filling", 4.10));
        db.productDao().insert(new Product(16, "Spinach Feta Danish", "Danish pastry with spinach and feta", 4.75));
        db.productDao().insert(new Product(17, "Lemon Tart", "Tart with lemon curd filling", 6.50));
        db.productDao().insert(new Product(18, "Brownie", "Fudgy chocolate brownie", 3.75));
        db.productDao().insert(new Product(19, "Vegan Chocolate Brownie", "Dairy-free brownie", 4.25));
        db.productDao().insert(new Product(20, "Glute-Free Banana Muffin", "Gluten-free banana muffin", 3.95));
        db.productDao().insert(new Product(21, "Seasonal Pumpkin Muffin", "Pumpkin spice muffin", 3.75));
        db.productDao().insert(new Product(22, "Strawberry Shortcake Cup", "Layered shortcake with strawberries", 6.95));
        db.productDao().insert(new Product(23, "Almond Biscotti", "Twice-baked almond biscotti", 2.75));
        db.productDao().insert(new Product(24, "Whole Wheat Scone", "Scone made with whole wheat flour", 3.25));
        db.productDao().insert(new Product(25, "Raspberry Danish", "Danish pastry with raspberry filling", 4.75));

    }
}
