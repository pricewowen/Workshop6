package com.example.workshop6.data.db;

import com.example.workshop6.data.model.Category;

public class DatabaseSeeder {
    /**
     * calls all methods to seed the database
     * @param db database
     */
    public static void seed(AppDatabase db) {
        seedCategories(db);
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
}
