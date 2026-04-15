package com.example.workshop6.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-local version counter used to invalidate in-memory UI caches after mutating API calls.
 */
public final class DataRefreshBus {

    private static final AtomicLong VERSION = new AtomicLong(0L);

    private DataRefreshBus() {
    }

    public static long currentVersion() {
        return VERSION.get();
    }

    public static void bumpVersion() {
        VERSION.incrementAndGet();
    }
}
