package com.example.workshop6.data.api.dto;

import java.util.List;

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
