package com.example.workshop6.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.User;
import com.example.workshop6.util.HashUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { User.class, BakeryLocation.class, Category.class, Product.class }, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract BakeryLocationDao bakeryLocationDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();

    private static volatile AppDatabase INSTANCE;

    // Single-threaded executor — all DB writes go through here to avoid main-thread violations
    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    // Create a callback that seeds using DAOs (no raw SQL)
                    RoomDatabase.Callback seedCallback = new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);

                            // Seed in background after Room finishes creating tables
                            databaseWriteExecutor.execute(() -> {
                                try {
                                    UserDao userDao = INSTANCE.userDao();

                                    // Only seed if admin doesn't already exist
                                    User existing = userDao.getUserByEmail("admin@bakery.com");
                                    if (existing == null) {
                                        User admin = new User();
                                        admin.fullName = "Admin User";
                                        admin.email = "admin@bakery.com";
                                        admin.phone = "555-0100";
                                        admin.passwordHash = HashUtils.hash("admin123");
                                        admin.role = "ADMIN";

                                        userDao.insert(admin);
                                    }

                                    DatabaseSeeder.seed(INSTANCE);
                                } catch (Exception ignored) {
                                    // If something goes wrong, login will simply fail rather than crash the app
                                }
                            });
                        }
                    };

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "workshop6_database"
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(seedCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}