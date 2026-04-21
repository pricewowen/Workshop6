// Contributor(s): Owen
// Main: Owen - Canadian phone formatting on register and profile fields.

package com.example.workshop6.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Formats a phone number as (###) ###-#### while the user types.
 * Keeps only digits in the stored content.
 */
public class PhoneFormatTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isFormatting;

    public PhoneFormatTextWatcher(EditText editText) {
        this.editText = editText;
        this.isFormatting = false;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if (isFormatting) return;
        isFormatting = true;

        String digits = s.toString().replaceAll("\\D", "");
        if (digits.length() > 10) {
            digits = digits.substring(0, 10);
        }

        String formatted;
        int len = digits.length();

        if (len == 0) {
            formatted = "";
        } else if (len <= 3) {
            formatted = "(" + digits;
        } else if (len <= 6) {
            formatted = "(" + digits.substring(0, 3) + ") " + digits.substring(3);
        } else {
            formatted = "(" + digits.substring(0, 3) + ") " + digits.substring(3, 6) + "-" + digits.substring(6);
        }

        editText.setText(formatted);
        editText.setSelection(formatted.length());

        isFormatting = false;
    }
}