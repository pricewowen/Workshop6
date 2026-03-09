// η℩.cαηtor ↈ and his AI, ⌈𝗆𝖾𝗍𝖺𝖼𝗈𝖽𝖺⌋ ⊛

package com.example.workshop6.logging;

import android.content.Context;

import com.example.workshop6.models.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class LogData {
    private static final String FILE_NAME = "Log.txt";

    /**
     * Saves a log entry to Log.txt in the app's internal storage.
     */
    public static void saveLog(Context context, Log log) {
        if (context == null || log == null) return;

        if (log.getUser() != null && log.getAction() != null && log.getTarget() != null) {
            try (PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    context.getApplicationContext().openFileOutput(FILE_NAME, Context.MODE_APPEND)
                            )
                    )
            )) {
                String line = log.getCurrentDate()
                        + " | USER=" + log.getUser()
                        + " | ACTION=" + log.getAction()
                        + " | TARGET=" + log.getTarget();

                out.append(line);
                out.println();
            } catch (IOException e) {
                System.err.println("ERROR: Could not write to log file");
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs an error in the same Workshop 5 style, with _FAILED appended to the action.
     */
    public static void logError(Context context, Log log) {
        if (context == null || log == null) return;

        if (log.getUser() != null && log.getAction() != null && log.getTarget() != null) {
            try (PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    context.getApplicationContext().openFileOutput(FILE_NAME, Context.MODE_APPEND)
                            )
                    )
            )) {
                String line = log.getCurrentDate()
                        + " | USER=" + log.getUser()
                        + " | ACTION=" + log.getAction() + "_FAILED"
                        + " | TARGET=" + log.getTarget();

                out.append(line);
                out.println();
            } catch (IOException e) {
                System.err.println("ERROR: Could not write to log file");
                System.err.println(log.getCurrentDate()
                        + " | USER=" + log.getUser()
                        + " | ACTION=" + log.getAction() + "_FAILED"
                        + " | TARGET=" + log.getTarget());
                e.printStackTrace();
            }
        }
    }

    /**
     * Log exception messages to the log file.
     */
    public static void handleException(Context context, String action, Exception e) {
        if (e == null) return;
        logError(context, new Log(action, e.getMessage()));
    }

    /**
     * Save a normal action to the log file.
     */
    public static void logAction(Context context, String action, String target) {
        saveLog(context, new Log(action, target));
    }
}