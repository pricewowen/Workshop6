package com.example.workshop6.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.data.model.User;
import com.example.workshop6.util.HashUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { User.class, BakeryLocation.class }, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    public abstract BakeryLocationDao bakeryLocationDao();

    private static volatile AppDatabase INSTANCE;

    // Single-threaded executor — all DB writes go through here to avoid main-thread violations
    public static final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

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
                    .addCallback(seedCallback)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // Seed a default Admin account on first creation
    private static final RoomDatabase.Callback seedCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                User admin = new User();
                admin.fullName = "Admin User";
                admin.email = "admin@bakery.com";
                admin.phone = "555-0100";
                admin.passwordHash = HashUtils.hash("admin123");
                admin.role = "ADMIN";
                INSTANCE.userDao().insert(admin);
            });
        }
    };
}
