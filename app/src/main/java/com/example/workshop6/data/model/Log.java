package com.example.workshop6.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Log {
    private String user;
    private String action;
    private String target;
    private String currentDate;

    private static final String DEFAULT_USER = "SYSTEM";
    private static String loggedInUser = null;

    public static void setLoggedInUser(String username) {
        loggedInUser = username;
    }

    public static void clearLoggedInUser() {
        loggedInUser = null;
    }

    public Log(String action, String description) {
        String resolvedUser = DEFAULT_USER;

        if (loggedInUser != null && !loggedInUser.trim().isEmpty()) {
            resolvedUser = loggedInUser;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());

        this.user = resolvedUser.toUpperCase();
        this.action = action.toUpperCase();
        this.target = description.toUpperCase();
        this.currentDate = formatter.format(new Date());
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