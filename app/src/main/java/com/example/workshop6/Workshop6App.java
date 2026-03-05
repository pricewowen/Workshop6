package com.example.workshop6;

import android.app.Application;

import com.example.workshop6.data.db.AppDatabase;

/**
 * Application class used to eagerly initialize and seed the database on app start.
 */
public class Workshop6App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Build the Room database and ensure initial seed completes.
        AppDatabase.getInstance(this);
        AppDatabase.awaitSeed();
    }
}

