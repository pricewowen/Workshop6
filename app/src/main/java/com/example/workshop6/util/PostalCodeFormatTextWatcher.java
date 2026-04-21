// Contributor(s): Owen
// Main: Owen - Canadian postal code formatting for address fields.

package com.example.workshop6.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Formats Canadian postal code as A1A 1A1: auto-inserts space after 3rd character,
 * allows only letters and digits, max 6 characters (display 7 with space).
 */
public class PostalCodeFormatTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isFormatting;

    public PostalCodeFormatTextWatcher(EditText editText) {
        this.editText = editText;
        this.isFormatting = false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (isFormatting) return;
        isFormatting = true;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 6; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
        }
        String raw = sb.toString();

        String formatted;
        if (raw.length() <= 3) {
            formatted = raw;
        } else {
            formatted = raw.substring(0, 3) + " " + raw.substring(3);
        }

        editText.setText(formatted);
        editText.setSelection(formatted.length());

        isFormatting = false;
    }
}
