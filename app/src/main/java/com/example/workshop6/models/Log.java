// η℩.cαηtor ↈ and his AI, ⌈𝗆𝖾𝗍𝖺𝖼𝗈𝖽𝖺⌋ ⊛

package com.example.workshop6.models;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private String user;
    private String action;
    private String target;
    private String currentDate;

    private static final String DEFAULT_USER = "SYSTEM";
    private static String loggedInUser = null;

    /**
     * Sets the username of the logged-in user.
     */
    public static void setLoggedInUser(String username) {
        loggedInUser = username;
    }

    /**
     * Clears the logged-in user.
     */
    public static void clearLoggedInUser() {
        loggedInUser = null;
    }

    public Log(String action, String description) {
        String user = DEFAULT_USER;

        if (loggedInUser != null && !loggedInUser.trim().isEmpty()) {
            user = loggedInUser;
        }

        Instant now = Instant.now();
        ZonedDateTime localTime = now.atZone(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        this.user = user.toUpperCase();
        this.action = action.toUpperCase();
        this.target = description.toUpperCase();
        this.currentDate = localTime.format(formatter);
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public String toString() {
        return currentDate + " | USER=" + user + " | ACTION=" + action + " | TARGET=" + target;
    }
}