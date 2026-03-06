package com.example.workshop6.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.data.model.Order;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.ProductTag;
import com.example.workshop6.data.model.Reward;
import com.example.workshop6.data.model.RewardTier;
import com.example.workshop6.data.model.User;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Database(
        entities = {
            User.class,
            Address.class,
            Customer.class,
            Employee.class,
            RewardTier.class,
            BakeryLocation.class,
            Category.class,
            Product.class,
                ProductTag.class,
                Reward.class,
                Order.class
        },
        version = 10,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract AddressDao addressDao();
    public abstract CustomerDao customerDao();
    public abstract EmployeeDao employeeDao();
    public abstract RewardTierDao rewardTierDao();
    public abstract BakeryLocationDao bakeryLocationDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract ProductTagDao productTagDao();
    public abstract RewardDao rewardDao();
    public abstract OrderDao orderDao();

    private static volatile AppDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    // Used so login can wait until seeding is finished
    private static final CountDownLatch seedLatch = new CountDownLatch(1);
    private static volatile boolean seedStarted = false;

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "workshop6_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();

                    startSeedingIfNeeded();
                }
            }
        } else {
            startSeedingIfNeeded();
        }

        return INSTANCE;
    }

    private static void startSeedingIfNeeded() {
        if (seedStarted) return;
        seedStarted = true;

        databaseWriteExecutor.execute(() -> {
            try {
                DatabaseSeeder.seed(INSTANCE);
            } catch (Exception ignored) {
            } finally {
                seedLatch.countDown();
            }
        });
    }

    /**
     * Allows callers to wait briefly for seeding to finish (login should call this).
     * If seeding is already done, returns immediately.
     */
    public static void awaitSeed() {
        try {
            seedLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}