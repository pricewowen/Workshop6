package com.example.workshop6.data.db;

import com.example.workshop6.R;
import android.util.Log;

import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.Batch;
import com.example.workshop6.data.model.BakeryHours;
import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.ProductTag;
import com.example.workshop6.data.model.Reward;
import com.example.workshop6.data.model.RewardTier;
import com.example.workshop6.data.model.User;
import com.example.workshop6.util.HashUtils;

/**
 * Seeds the database with starter data.
 */
public class DatabaseSeeder {
    public static final int DEFAULT_REWARD_TIER_ID = 1;
    public static final String DEFAULT_TIER_NAME = "Default";
    public static final int SILVER_REWARD_TIER_ID = 2;
    public static final String SILVER_TIER_NAME = "Silver";
    public static final int GOLD_REWARD_TIER_ID = 3;
    public static final String GOLD_TIER_NAME = "Gold";

    public static void seed(AppDatabase db) {
        seedRewardTiers(db);
        seedAdminUser(db);
        seedAdminEmployee(db);
        seedBakeryLocations(db);
        seedTestCustomer(db);
        seedCategories(db);
        seedProducts(db);
        seedProductTags(db);
        seedCustomers(db);
        seedBakeryLocations(db);
        seedBatches(db);
        seedRewards(db);
    }


    private static void seedCustomers(AppDatabase db) {
        // Create users for customers
        String[][] customerUsers = {
                {"customer1","customer1@bakery.com"},
                {"customer2","customer2@bakery.com"},
                {"customer3","customer3@bakery.com"},
                {"customer4","customer4@bakery.com"},
                {"customer5","customer5@bakery.com"},
                {"customer6","customer6@bakery.com"},
                {"customer7","customer7@bakery.com"},
                {"customer8","customer8@bakery.com"},
                {"customer9","customer9@bakery.com"},
                {"customer10","customer10@bakery.com"},
                {"customer11","customer11@bakery.com"},
                {"customer12","customer12@bakery.com"},
                {"customer13","customer13@bakery.com"},
        };

        for (String[] u : customerUsers) {
            if (db.userDao().getUserByEmail(u[1]) == null) {
                User user = new User();
                user.userUsername = u[0];
                user.userEmail = u[1];
                user.userPasswordHash = HashUtils.hash("customer123");
                user.userRole = "CUSTOMER";
                user.userCreatedAt = System.currentTimeMillis();
                db.userDao().insert(user);
            }
        }

        // Create customer records
        for (int i = 1; i <= 13; i++) {
            User u = db.userDao().getUserByEmail("customer" + i + "@bakery.com");
            if (u == null) continue;
            if (db.customerDao().getByUserId(u.userId) != null) continue;

            // Create a real, non-empty address for each seeded customer.
            Address addr = new Address();
            addr.addressLine1 = i + " Customer St";
            addr.addressLine2 = null;
            addr.addressCity = "Toronto";
            addr.addressProvince = "Ontario";
            addr.addressPostalCode = "M5V 0" + i;
            int addressId = (int) db.addressDao().insert(addr);

            Customer c = new Customer();
            c.userId = u.userId;
            c.addressId = addressId;
            c.customerFirstName = "Customer" + i;
            c.customerMiddleInitial = null;
            c.customerLastName = "Test";
            c.customerRole = "Customer";
            c.customerPhone = "(555) 100-000" + i;
            c.customerBusinessPhone = null;
            c.customerRewardBalance = 0;
            c.customerTierAssignedDate = System.currentTimeMillis();
            c.customerRewardBalance = 100 * i; // example balance
            c.rewardTierId = 1; // default tier
            c.customerEmail = u.userEmail;
            db.customerDao().insert(c);
        }
    }

    private static void seedBakeryLocations(AppDatabase db) {
        if (!db.bakeryLocationDao().getAllLocationsSync().isEmpty()) {
            return;
        }

        BakeryLocation loc1 = new BakeryLocation();
        loc1.name = "North Harbour Bakery - Downtown";
        Address loc1Address = new Address();
        loc1Address.addressLine1 = "123 Main St";
        loc1Address.addressLine2 = null;
        loc1Address.addressCity = "Calgary";
        loc1Address.addressProvince = "Alberta";
        loc1Address.addressPostalCode = "T2P 1A1";
        loc1.addressId = (int) db.addressDao().insert(loc1Address);
        loc1.phone = "(403) 555-2101";
        loc1.email = "downtown@northharbourbakery.ca";
        loc1.status = "Open";
        loc1.latitude = 51.0447;
        loc1.longitude = -114.0719;
        int loc1Id = (int) db.bakeryLocationDao().insert(loc1);
        seedDefaultBakeryHours(db, loc1Id);

        BakeryLocation loc2 = new BakeryLocation();
        loc2.name = "North Harbour Bakery - Edmonton Central";
        Address loc2Address = new Address();
        loc2Address.addressLine1 = "456 Jasper Ave";
        loc2Address.addressLine2 = null;
        loc2Address.addressCity = "Edmonton";
        loc2Address.addressProvince = "Alberta";
        loc2Address.addressPostalCode = "T5J 1S9";
        loc2.addressId = (int) db.addressDao().insert(loc2Address);
        loc2.phone = "(780) 555-4302";
        loc2.email = "edmonton@northharbourbakery.ca";
        loc2.status = "Open";
        loc2.latitude = 53.5461;
        loc2.longitude = -113.4938;
        int loc2Id = (int) db.bakeryLocationDao().insert(loc2);
        seedDefaultBakeryHours(db, loc2Id);

        BakeryLocation loc3 = new BakeryLocation();
        loc3.name = "North Harbour Bakery - Toronto Financial";
        Address loc3Address = new Address();
        loc3Address.addressLine1 = "789 Bay St";
        loc3Address.addressLine2 = null;
        loc3Address.addressCity = "Toronto";
        loc3Address.addressProvince = "Ontario";
        loc3Address.addressPostalCode = "M5J 2T3";
        loc3.addressId = (int) db.addressDao().insert(loc3Address);
        loc3.phone = "(416) 555-9012";
        loc3.email = "toronto@northharbourbakery.ca";
        loc3.status = "Open";
        loc3.latitude = 43.6532;
        loc3.longitude = -79.3832;
        int loc3Id = (int) db.bakeryLocationDao().insert(loc3);
        seedDefaultBakeryHours(db, loc3Id);
    }

    private static void seedDefaultBakeryHours(AppDatabase db, int bakeryId) {
        for (int day = 1; day <= 6; day++) {
            db.bakeryHoursDao().insert(new BakeryHours(bakeryId, day, "07:00", "19:00", false));
        }
        db.bakeryHoursDao().insert(new BakeryHours(bakeryId, 7, null, null, true));
    }

    private static void seedBatches(AppDatabase db) {
        android.util.Log.d("DatabaseSeeder", "seedBatches called, current count: " + db.batchDao().getAllBatches().size());
        if (!db.batchDao().getAllBatches().isEmpty()) {
            android.util.Log.d("DatabaseSeeder", "Batches already seeded, skipping");
            return;
        }

        long now = System.currentTimeMillis();
        long day = 24 * 60 * 60 * 1000L;

        db.batchDao().insert(new Batch(1,  1, 1,  1, now - 3*day, now + day, 60));
        db.batchDao().insert(new Batch(2,  1, 3,  2, now - 3*day, now + 2*day, 90));
        db.batchDao().insert(new Batch(3,  1, 5,  3, now - 2*day, now + 4*day, 120));
        db.batchDao().insert(new Batch(4,  1, 8,  4, now - 4*day, now + 7*day, 200));
        db.batchDao().insert(new Batch(5,  1, 13, 3, now - 2*day, now + 2*day, 12));
        db.batchDao().insert(new Batch(7,  2, 2,  5, now - 4*day, now + 4*day, 55));

        db.batchDao().insert(new Batch(6, 2, 1, 4, now - 2*day, now + 3*day, 50));

        db.batchDao().insert(new Batch(8,  2, 6,  6, now - 2*day, now + 3*day, 140));
        db.batchDao().insert(new Batch(9,  2, 10, 7, now - 2*day, now + 6*day, 110));
        db.batchDao().insert(new Batch(10, 2, 14, 8, now - 2*day, now + 2*day, 40));
        db.batchDao().insert(new Batch(12, 3, 4,  6, now - 3*day, now + 3*day, 70));

        db.batchDao().insert(new Batch(11, 2, 4, 6, now - 3*day, now + 2*day, 80));

        db.batchDao().insert(new Batch(13, 3, 7,  7, now - 4*day, now + day, 120));
        db.batchDao().insert(new Batch(14, 3, 12, 8, now - 2*day, now + 5*day, 30));
        db.batchDao().insert(new Batch(15, 3, 15, 9, now - 2*day, now + 2*day, 75));
        db.batchDao().insert(new Batch(16, 3, 16, 5, now - day, now + 3*day, 65));

        android.util.Log.d("DatabaseSeeder", "Batches seeded, new count: " + db.batchDao().getAllBatches().size());
    }

    private static void seedRewards(AppDatabase db) {
        if (!db.rewardDao().getAllRewards().isEmpty()) return;

        long now = System.currentTimeMillis();

        // Reward(customerId, productId, rewardId, points, timestamp)
        db.rewardDao().insert(new Reward(1, 1, 1, 1000, now));
        db.rewardDao().insert(new Reward(2, 2, 2, 2000, now));
        db.rewardDao().insert(new Reward(3, 3, 3, 1500, now));
        db.rewardDao().insert(new Reward(4, 4, 4, 1200, now));
        db.rewardDao().insert(new Reward(5, 5, 5, 1800, now));
        db.rewardDao().insert(new Reward(6, 6, 6, 1600, now));
        db.rewardDao().insert(new Reward(7, 7, 7, 1300, now));
        db.rewardDao().insert(new Reward(8, 8, 8, 1400, now));
        db.rewardDao().insert(new Reward(9, 9, 9, 1100, now));
        db.rewardDao().insert(new Reward(10, 10, 10, 1700, now));
        db.rewardDao().insert(new Reward(11, 11, 11, 1250, now));
        db.rewardDao().insert(new Reward(12, 12, 12, 1350, now));
        db.rewardDao().insert(new Reward(13, 13, 13, 1450, now));
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
        db.rewardTierDao().insert(new RewardTier(
                DEFAULT_REWARD_TIER_ID,
                DEFAULT_TIER_NAME,
                0,
                9999,
                "Starting tier for all customers"
        ));
        db.rewardTierDao().insert(new RewardTier(
                SILVER_REWARD_TIER_ID,
                SILVER_TIER_NAME,
                10000,
                24999,
                "Mid-tier status with stronger rewards progress"
        ));
        db.rewardTierDao().insert(new RewardTier(
                GOLD_REWARD_TIER_ID,
                GOLD_TIER_NAME,
                25000,
                null,
                "Top tier for highly engaged customers"
        ));
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

        // adding test employees
        for (int i = 2; i <= 10; i++) {
            // Create a dummy user for this employee
            User u = new User();
            u.userUsername = "employee" + i;
            u.userEmail = "employee" + i + "@bakery.com";
            u.userPasswordHash = HashUtils.hash("employee123");
            u.userRole = "EMPLOYEE";
            u.userCreatedAt = System.currentTimeMillis();
            long userId = db.userDao().insert(u); // store userId

            // Create an address for the employee
            Address a = new Address();
            a.addressLine1 = i + " Test Street";
            a.addressLine2 = null;
            a.addressCity = "Toronto";
            a.addressProvince = "Ontario";
            a.addressPostalCode = "M5V 1" + i;
            long addrId = db.addressDao().insert(a);

            // Insert employee
            Employee e = new Employee();
            e.userId = (int) userId;
            e.addressId = (int) addrId;
            e.employeeFirstName = "Employee" + i;
            e.employeeMiddleInitial = null;
            e.employeeLastName = "Test";
            e.employeeRole = "STAFF";
            e.employeePhone = "(555) 000-000" + i;
            e.employeeBusinessPhone = null;
            e.employeeEmail = "employee" + i + "@bakery.com";
            db.employeeDao().insert(e);
        }
    }
    private static void seedTestCustomer(AppDatabase db) {
        // Check if test user already exists
        User existingUser = db.userDao().getUserByEmail("customer@bakery.com");
        if (existingUser != null) return;

        // Create a test address
        Address address = new Address();
        address.addressLine1 = "123 Bakery Avenue";
        address.addressLine2 = null;
        address.addressCity = "Calgary";
        address.addressProvince = "Alberta";
        address.addressPostalCode = "2B2 B2B";
        long addressId = db.addressDao().insert(address);

        // Create user
        User user = new User();
        user.userUsername = "customer";
        user.userEmail = "customer@bakery.com";
        user.userPasswordHash = HashUtils.hash("customer123");
        user.userRole = "CUSTOMER";
        user.userCreatedAt = System.currentTimeMillis();
        long userId = db.userDao().insert(user);

        // Create customer profile
        Customer customer = new Customer();
        customer.userId = (int) userId;
        customer.addressId = (int) addressId;
        customer.rewardTierId = DEFAULT_REWARD_TIER_ID;
        customer.customerFirstName = "Test";
        customer.customerMiddleInitial = null;
        customer.customerLastName = "Customer";
        customer.customerRole = "Customer";
        customer.customerPhone = "(416) 555-1234";
        customer.customerBusinessPhone = null;
        customer.customerRewardBalance = 1000; // Give them some points
        customer.customerTierAssignedDate = System.currentTimeMillis();
        customer.customerEmail = "customer@bakery.com";
        customer.profilePhotoPath = null;
        customer.photoApprovalPending = false;

        db.customerDao().insert(customer);

        Log.d("DatabaseSeeder", "Test customer created with address ID: " + addressId);
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
        db.productDao().insert(new Product(1, "Sourdough Loaf", "Naturally leavened sourdough bread", 6.49, R.drawable.product_sourdough_loaf));
        db.productDao().insert(new Product(2, "Multigrain Sandwich Bread", "Whole grain sandwich loaf", 5.99, R.drawable.product_multigrain_bread));
        db.productDao().insert(new Product(3, "Baguette", "Classic French-style baguette", 3.49, R.drawable.product_baguette));
        db.productDao().insert(new Product(4, "Cinnamon Roll", "Soft roll with cinnamon filling and glaze", 4.25, R.drawable.product_cinnamon_roll));
        db.productDao().insert(new Product(5, "Butter Croissant", "Flaky butter croissant", 3.95, R.drawable.product_butter_croissant));
        db.productDao().insert(new Product(6, "Blueberry Muffin", "Muffin with blueberries", 3.25, R.drawable.product_blueberry_muffin));
        db.productDao().insert(new Product(7, "Banana Bread Slice", "Moist banana bread slice", 2.95, R.drawable.product_banana_bread));
        db.productDao().insert(new Product(8, "Chocolate Chip Cookie", "Cookie with chocolate chips", 2.25, R.drawable.product_chocolate_chip_cookie));
        db.productDao().insert(new Product(9, "Oatmeal Raisin Cookie", "Chewy oatmeal raisin cookie", 2.25, R.drawable.product_oatmeal_raisin_cookie));
        db.productDao().insert(new Product(10, "Vanilla Cupcake", "Cupcake with vanilla frosting", 2.95, R.drawable.product_vanilla_cupcake));
        db.productDao().insert(new Product(11, "Chocolate Cupcake", "Cupcake with chocolate frosting", 2.95, R.drawable.product_chocolate_cupcake));
        db.productDao().insert(new Product(12, "Carrot Cake Slice", "Slice of carrot cake with cream cheese icing", 4.49, R.drawable.product_carrot_cake));
        db.productDao().insert(new Product(13, "Lemon Tart", "Tart with lemon custard filling", 4.25, R.drawable.product_lemon_tart));
        db.productDao().insert(new Product(14, "Brownie", "Fudgy chocolate brownie", 3.50, R.drawable.product_brownie));
        db.productDao().insert(new Product(15, "Gluten-Free Banana Muffin", "Gluten-free banana muffin", 3.75, R.drawable.product_gluten_free_banana_muffin));
        db.productDao().insert(new Product(16, "Vegan Chocolate Chip Cookie", "Vegan cookie with chocolate chips", 2.50, R.drawable.product_vegan_chocolate_chip_cookie));
    }
}
