package com.example.workshop6.util;

import android.util.Patterns;
import androidx.annotation.Nullable;
import java.util.regex.Pattern;

public class Validation {
    private static final int MIN_PASSWORD_LENGTH = 7;
    // Keep max aligned with practical credential policy used by the app.
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final Pattern PASSWORD_UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASSWORD_LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern PASSWORD_SYMBOL_PATTERN = Pattern.compile(".*[^A-Za-z0-9].*");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[a-zA-Z ]+$");
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^[0-9]{10}$");

    private static final int MAX_ADDRESS_LENGTH = 120;
    private static final Pattern CITY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z \\-']*$");
    private static final Pattern PROVINCE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z \\-']*$");
    public static final int ORDER_COMMENT_MAX_LENGTH = 250;
    public static final int CHAT_MESSAGE_MAX_LENGTH = 500;
    public static final int SEARCH_QUERY_MAX_LENGTH = 80;

    // Canada postal code: A1A 1A1 (space optional). Also accept US ZIP (12345 or 12345-6789).
    private static final Pattern POSTAL_CODE_CA = Pattern.compile("(?i)^[ABCEGHJ-NPRSTVXY]\\d[ABCEGHJ-NPRSTV-Z][ -]?\\d[ABCEGHJ-NPRSTV-Z]\\d$");
    private static final Pattern POSTAL_CODE_US = Pattern.compile("^\\d{5}(-\\d{4})?$");

    /** Username: 3–50 chars, letters, numbers, underscore, hyphen only. */
    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

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
     * Checks whether the password length is within allowed bounds.
     *
     * @param password the password to be validated
     * @return true if the password is long enough
     */
    public static boolean isPasswordValid(@Nullable CharSequence password) {
        return !isEmpty(password) && password.length() >= MIN_PASSWORD_LENGTH && password.length() <= MAX_PASSWORD_LENGTH;
    }

    public static boolean hasUppercase(@Nullable CharSequence password) {
        return !isEmpty(password) && PASSWORD_UPPERCASE_PATTERN.matcher(password).matches();
    }

    public static boolean hasLowercase(@Nullable CharSequence password) {
        return !isEmpty(password) && PASSWORD_LOWERCASE_PATTERN.matcher(password).matches();
    }

    public static boolean hasDigit(@Nullable CharSequence password) {
        return !isEmpty(password) && PASSWORD_DIGIT_PATTERN.matcher(password).matches();
    }

    public static boolean hasSymbol(@Nullable CharSequence password) {
        return !isEmpty(password) && PASSWORD_SYMBOL_PATTERN.matcher(password).matches();
    }

    public static boolean isPasswordStrong(@Nullable CharSequence password) {
        return isPasswordValid(password)
                && hasUppercase(password)
                && hasLowercase(password)
                && hasDigit(password)
                && hasSymbol(password);
    }

    public static boolean isPasswordTooShort(@Nullable CharSequence password) {
        return !isEmpty(password) && password.length() < MIN_PASSWORD_LENGTH;
    }

    public static boolean isPasswordTooLong(@Nullable CharSequence password) {
        return !isEmpty(password) && password.length() > MAX_PASSWORD_LENGTH;
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
     * Formats 10 digits as (XXX) XXX-XXXX for consistent DB storage.
     * @param digits digits-only phone string (e.g. from replaceAll("\\D", ""))
     * @return formatted string, or the original if not 10 digits
     */
    @Nullable
    public static String formatPhoneForStorage(@Nullable CharSequence digits) {
        if (digits == null) return null;
        String d = digits.toString().replaceAll("\\D", "");
        if (d.length() != 10) return d.isEmpty() ? null : d;
        return "(" + d.substring(0, 3) + ") " + d.substring(3, 6) + "-" + d.substring(6);
    }

    /**
     * Username: non-empty, 3–50 chars, only letters, digits, underscore, hyphen.
     */
    public static boolean isUsernameValid(@Nullable CharSequence username) {
        if (username == null) return false;
        String s = username.toString().trim();
        return s.length() >= USERNAME_MIN_LENGTH
                && s.length() <= USERNAME_MAX_LENGTH
                && USERNAME_PATTERN.matcher(s).matches();
    }

    public static boolean isUsernameTooShort(@Nullable CharSequence username) {
        if (username == null) return false;
        String s = username.toString().trim();
        return !s.isEmpty() && s.length() < USERNAME_MIN_LENGTH;
    }

    public static boolean isUsernameTooLong(@Nullable CharSequence username) {
        if (username == null) return false;
        String s = username.toString().trim();
        return s.length() > USERNAME_MAX_LENGTH;
    }

    public static boolean isUsernameFormatValid(@Nullable CharSequence username) {
        if (username == null) return false;
        String s = username.toString().trim();
        return !s.isEmpty() && USERNAME_PATTERN.matcher(s).matches();
    }

    public static boolean isAddressLineValid(@Nullable CharSequence addressLine) {
        if (isEmpty(addressLine)) return false;
        String s = addressLine.toString().trim();
        return s.length() > 0 && s.length() <= MAX_ADDRESS_LENGTH;
    }

    public static boolean isCityValid(@Nullable CharSequence city) {
        if (isEmpty(city)) return false;
        String s = city.toString().trim();
        return s.length() > 0 && s.length() <= 60 && CITY_PATTERN.matcher(s).matches();
    }

    public static boolean isProvinceValid(@Nullable CharSequence province) {
        if (isEmpty(province)) return false;
        String s = province.toString().trim();
        return s.length() >= 2 && s.length() <= 30 && PROVINCE_PATTERN.matcher(s).matches();
    }

    public static boolean isPostalCodeValid(@Nullable CharSequence postalCode) {
        if (isEmpty(postalCode)) return false;
        String s = postalCode.toString().trim();
        return POSTAL_CODE_CA.matcher(s).matches() || POSTAL_CODE_US.matcher(s).matches();
    }

    @Nullable
    public static String limitLength(@Nullable CharSequence value, int maxLength) {
        if (value == null) return null;
        String s = value.toString();
        if (s.length() <= maxLength) {
            return s;
        }
        return s.substring(0, maxLength);
    }
}