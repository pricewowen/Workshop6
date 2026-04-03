package com.example.workshop6.logging;

import android.content.Context;

import com.example.workshop6.auth.SessionManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public final class ActivityLogger {
    private static final String FILE_NAME = "activity_log.txt";
    private static final String DEFAULT_USER = "SYSTEM";

    private ActivityLogger() {}

    public static void log(Context context, SessionManager sessionManager, String action, String target) {
        String userName = DEFAULT_USER;
        if (sessionManager != null) {
            String uuid = sessionManager.getUserUuid();
            if (uuid != null && !uuid.isEmpty()) {
                userName = "USER#" + uuid;
            } else if (sessionManager.getUserId() > 0) {
                userName = "USER#" + sessionManager.getUserId();
            }
        }
        writeLine(context, userName, action, target, false);
    }

    public static void log(Context context, String userName, String action, String target) {
        String resolved = (userName == null || userName.trim().isEmpty()) ? DEFAULT_USER : sanitize(userName.trim());
        writeLine(context, resolved, action, target, false);
    }

    public static void logFailure(Context context, SessionManager sessionManager, String action, String target) {
        String userName = DEFAULT_USER;
        if (sessionManager != null) {
            String uuid = sessionManager.getUserUuid();
            if (uuid != null && !uuid.isEmpty()) {
                userName = "USER#" + uuid;
            } else if (sessionManager.getUserId() > 0) {
                userName = "USER#" + sessionManager.getUserId();
            }
        }
        writeLine(context, userName, action, target, true);
    }

    private static void writeLine(Context context, String userName, String action, String target, boolean failed) {
        if (context == null || action == null || target == null) return;

        String safeAction = action.trim().toUpperCase(Locale.ROOT);
        if (failed) {
            safeAction = safeAction + "_FAILED";
        }
        String safeTarget = sanitize(target.trim());
        String now = timestamp();
        String line = now
                + " | USER=" + userName.toUpperCase(Locale.ROOT)
                + " | ACTION=" + safeAction
                + " | TARGET=" + safeTarget;

        try (PrintWriter out = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(
                                context.getApplicationContext().openFileOutput(FILE_NAME, Context.MODE_APPEND)
                        )
                )
        )) {
            out.append(line);
            out.println();
        } catch (IOException ignored) {
        }
    }

    private static String timestamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(new Date());
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }

        String sanitized = raw.trim()
                .replaceAll("(?i)[A-Z0-9._%+-]+@([A-Z0-9.-]+\\.[A-Z]{2,})", "***@$1")
                .replaceAll("(?i)(username|email):\\s*[^\\s|,]+", "$1:[redacted]")
                .replaceAll("(?i)(address):\\s*[^|]+", "$1:[redacted]");

        if (sanitized.length() > 140) {
            sanitized = sanitized.substring(0, 140);
        }

        return sanitized;
    }
}
