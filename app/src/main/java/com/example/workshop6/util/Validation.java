package com.example.workshop6.util;

import android.util.Patterns;
import androidx.annotation.Nullable;
import java.util.regex.Pattern;

public class Validation {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 250;
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[a-zA-Z ]+$");
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern SQL_KEYWORD_PATTERN = Pattern.compile("(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|FROM|WHERE|--|;)");

    private Validation() {
        // private constructor to prevent instantiation
    }

    /**
     * Evaluates if the value is empty or not.
     * Returns true if the value is null or its length is 0.
     *
     * @param str the value to be evaluated
     * @return true if the value is empty
     */
    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    /**
     * Checks if the given email has a valid format.
     *
     * @param email the email to be validated
     * @return true if the email is valid
     */
    public static boolean isEmailValid(@Nullable CharSequence email) {
        return !isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Checks if the given password meets the minimum and maximum length requirement.
     *
     * @param password the password to be validated
     * @return true if the password is long enough
     */
    public static boolean isPasswordValid(@Nullable CharSequence password) {
        return !isEmpty(password) && password.length() >= MIN_PASSWORD_LENGTH && password.length() <= MAX_PASSWORD_LENGTH;
    }

    /**
     * Checks if the given full name contains only letters and spaces.
     *
     * @param fullName the full name to be validated
     * @return true if the full name is valid
     */
    public static boolean isFullNameValid(@Nullable CharSequence fullName) {
        return !isEmpty(fullName) && FULL_NAME_PATTERN.matcher(fullName).matches();
    }

    /**
     * Checks if the given phone number contains only digits.
     *
     * @param phoneNumber the phone number to be validated
     * @return true if the phone number is valid
     */
    public static boolean isPhoneNumberValid(@Nullable CharSequence phoneNumber) {
        return !isEmpty(phoneNumber) && PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches();
    }

    /**
     * A basic check to see if the password contains common SQL keywords.
     * Note: This is not a substitute for using parameterized queries to prevent SQL injection,
     * which is the correct way to handle it. This is a simple client-side check.
     *
     * @param password the password to be checked
     * @return true if the password does not appear to contain SQL.
     */
    public static boolean isPasswordSafeFromSimpleSql(@Nullable CharSequence password) {
        if (isEmpty(password)) {
            return true;
        }
        return !SQL_KEYWORD_PATTERN.matcher(password).find();
    }
}
