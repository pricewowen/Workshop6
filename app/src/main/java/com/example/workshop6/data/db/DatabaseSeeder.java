package com.example.workshop6.data.db;

import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.ProductTag;
import com.example.workshop6.data.model.RewardTier;
import com.example.workshop6.data.model.User;
import com.example.workshop6.util.HashUtils;

/**
 * Seeds the database with starter data.
 */
public class DatabaseSeeder {
    public static final int DEFAULT_REWARD_TIER_ID = 1;
    public static final String DEFAULT_TIER_NAME = "Default";

    public static void seed(AppDatabase db) {
        seedRewardTiers(db);
        seedDefaultAddress(db);
        seedAdminUser(db);
        seedAdminEmployee(db);
        seedCategories(db);
        seedProducts(db);
        seedProductTags(db);
    }

    private static void seedProductTags(AppDatabase db) {
        if (!db.productTagDao().getAllProductTags().isEmpty()) {
            return;
        }

        db.productTagDao().insert(new ProductTag(1, 1));
        db.productTagDao().insert(new ProductTag(2, 1));
        db.productTagDao().insert(new ProductTag(2, 12));
        db.productTagDao().insert(new ProductTag(3, 1));
        db.productTagDao().insert(new ProductTag(4, 3));
        db.productTagDao().insert(new ProductTag(4, 9));
        db.productTagDao().insert(new ProductTag(5, 3));
        db.productTagDao().insert(new ProductTag(5, 9));
        db.productTagDao().insert(new ProductTag(6, 3));
        db.productTagDao().insert(new ProductTag(6, 9));
        db.productTagDao().insert(new ProductTag(7, 3));
        db.productTagDao().insert(new ProductTag(7, 9));
        db.productTagDao().insert(new ProductTag(8, 4));
        db.productTagDao().insert(new ProductTag(8, 10));
        db.productTagDao().insert(new ProductTag(9, 4));
        db.productTagDao().insert(new ProductTag(9, 10));
        db.productTagDao().insert(new ProductTag(10, 2));
        db.productTagDao().insert(new ProductTag(10, 10));
        db.productTagDao().insert(new ProductTag(11, 2));
        db.productTagDao().insert(new ProductTag(11, 10));
        db.productTagDao().insert(new ProductTag(12, 2));
        db.productTagDao().insert(new ProductTag(12, 10));
        db.productTagDao().insert(new ProductTag(13, 2));
        db.productTagDao().insert(new ProductTag(13, 10));
        db.productTagDao().insert(new ProductTag(14, 2));
    }

    private static void seedRewardTiers(AppDatabase db) {
        if (!db.rewardTierDao().getAll().isEmpty()) return;
        db.rewardTierDao().insert(new RewardTier(DEFAULT_REWARD_TIER_ID, DEFAULT_TIER_NAME));
    }

    /** One default address for customers who don't provide one (required by FK). */
    private static void seedDefaultAddress(AppDatabase db) {
        if (db.addressDao().getById(1) != null) return;
        Address a = new Address();
        a.addressLine1 = "";
        a.addressLine2 = null;
        a.addressCity = null;
        a.addressProvince = "";
        a.addressPostalCode = "";
        db.addressDao().insert(a);
    }

    private static void seedAdminUser(AppDatabase db) {
        User existing = db.userDao().getUserByEmail("admin@bakery.com");
        if (existing != null) return;

        User admin = new User();
        admin.userUsername = "admin";
        admin.userEmail = "admin@bakery.com";
        admin.userPasswordHash = HashUtils.hash("admin123");
        admin.userRole = "ADMIN";
        admin.userCreatedAt = System.currentTimeMillis();
        db.userDao().insert(admin);
    }

    /** Creates an Employee record for the seeded admin with full test address and phone. */
    private static void seedAdminEmployee(AppDatabase db) {
        User admin = db.userDao().getUserByEmail("admin@bakery.com");
        if (admin == null) return;
        if (db.employeeDao().getByUserId(admin.userId) != null) return;

        // Insert a dedicated test address for the admin (all NOT NULL fields set)
        Address testAddress = new Address();
        testAddress.addressLine1 = "123 Bakery Street";
        testAddress.addressLine2 = "Suite 100";
        testAddress.addressCity = "Toronto";
        testAddress.addressProvince = "Ontario";
        testAddress.addressPostalCode = "M5V 1A1";
        long addressId = db.addressDao().insert(testAddress);

        Employee emp = new Employee();
        emp.userId = admin.userId;
        emp.addressId = (int) addressId;
        emp.employeeFirstName = "Admin";
        emp.employeeMiddleInitial = null;
        emp.employeeLastName = "User";
        emp.employeeRole = "ADMIN";
        emp.employeePhone = "(555) 555-5555";
        emp.employeeBusinessPhone = null;
        emp.employeeEmail = "admin@bakery.com";
        db.employeeDao().insert(emp);
    }

    private static void seedCategories(AppDatabase db) {
        if (!db.categoryDao().getAllCategories().isEmpty()) return;
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
        if (!db.productDao().getAllProducts().isEmpty()) return;
        db.productDao().insert(new Product(1, "Sourdough Loaf", "Naturally leavened sourdough bread", 6.49));
        db.productDao().insert(new Product(2, "Multigrain Sandwich Bread", "Whole grain sandwich loaf", 5.99));
        db.productDao().insert(new Product(3, "Baguette", "Classic French-style baguette", 3.49));
        db.productDao().insert(new Product(4, "Cinnamon Roll", "Soft roll with cinnamon filling and glaze", 4.25));
        db.productDao().insert(new Product(5, "Butter Croissant", "Flaky butter croissant", 3.95));
        db.productDao().insert(new Product(6, "Blueberry Muffin", "Muffin with blueberries", 3.25));
        db.productDao().insert(new Product(7, "Banana Bread Slice", "Moist banana bread slice", 2.95));
        db.productDao().insert(new Product(8, "Chocolate Chip Cookie", "Cookie with chocolate chips", 2.25));
        db.productDao().insert(new Product(9, "Oatmeal Raisin Cookie", "Chewy oatmeal raisin cookie", 2.25));
        db.productDao().insert(new Product(10, "Vanilla Cupcake", "Cupcake with vanilla frosting", 2.95));
        db.productDao().insert(new Product(11, "Chocolate Cupcake", "Cupcake with chocolate frosting", 2.95));
        db.productDao().insert(new Product(12, "Carrot Cake Slice", "Slice of carrot cake with cream cheese icing", 4.49));
        db.productDao().insert(new Product(13, "Lemon Tart", "Tart with lemon custard filling", 4.25));
        db.productDao().insert(new Product(14, "Brownie", "Fudgy chocolate brownie", 3.50));
        db.productDao().insert(new Product(15, "Gluten-Free Banana Muffin", "Gluten-free banana muffin", 3.75));
        db.productDao().insert(new Product(16, "Vegan Chocolate Chip Cookie", "Vegan cookie with chocolate chips", 2.50));
    }
}
