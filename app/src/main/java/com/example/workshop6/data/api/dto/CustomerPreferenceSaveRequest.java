// Contributor(s): Owen
// Main: Owen - Save preference selections from profile UI.

package com.example.workshop6.data.api.dto;

import java.util.List;

/**
 * Gson body to save taste preference chips for the logged-in customer on Workshop 7.
 */
public class CustomerPreferenceSaveRequest {
    public List<PreferenceEntry> preferences;

    public CustomerPreferenceSaveRequest(List<PreferenceEntry> preferences) {
        this.preferences = preferences;
    }

    public static class PreferenceEntry {
        public int tagId;
        public PreferenceType preferenceType;

        public PreferenceEntry(int tagId, PreferenceType preferenceType) {
            this.tagId = tagId;
            this.preferenceType = preferenceType;
        }
    }
}
