// Contributor(s): Owen
// Main: Owen - AI preference chips save and validation.

package com.example.workshop6.ui.me;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.CustomerPreferenceDto;
import com.example.workshop6.data.api.dto.CustomerPreferenceSaveRequest;
import com.example.workshop6.data.api.dto.PreferenceType;
import com.example.workshop6.data.api.dto.TagDto;
import com.example.workshop6.util.NavTransitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lets a signed-in customer map each catalog tag to a like, dislike, avoid or allergic preference for Workshop 7 AI.
 */
public class CustomerPreferencesActivity extends AppCompatActivity {

    private ApiService api;
    private LinearLayout container;
    private View loadingOverlay;
    private final List<Row> rows = new ArrayList<>();
    private final Map<Integer, Integer> initialSelectionByTag = new HashMap<>();

    private static final class Row {
        final int tagId;
        final Spinner spinner;

        Row(int tagId, Spinner spinner) {
            this.tagId = tagId;
            this.spinner = spinner;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_preferences);

        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || sessionManager.isGuestMode()) {
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        ApiClient.getInstance().setToken(sessionManager.getToken());
        api = ApiClient.getInstance().getService();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(CustomerPreferencesActivity.this);
        });
        container = findViewById(R.id.container_tags);
        loadingOverlay = findViewById(R.id.preferences_loading);

        findViewById(R.id.btn_save_preferences).setOnClickListener(v -> save());

        loadTagsAndPreferences();
    }

    private void loadTagsAndPreferences() {
        loadingOverlay.setVisibility(View.VISIBLE);
        api.getTags().enqueue(new Callback<List<TagDto>>() {
            @Override
            public void onResponse(Call<List<TagDto>> call, Response<List<TagDto>> response) {
                if (response.code() == 401 || response.code() == 403) {
                    redirectLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(CustomerPreferencesActivity.this,
                            getString(R.string.login_error_server, response.code()),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                List<TagDto> tags = response.body();
                api.getMyPreferences().enqueue(new Callback<List<CustomerPreferenceDto>>() {
                    @Override
                    public void onResponse(Call<List<CustomerPreferenceDto>> call2,
                                           Response<List<CustomerPreferenceDto>> response2) {
                        loadingOverlay.setVisibility(View.GONE);
                        if (response2.code() == 401 || response2.code() == 403) {
                            redirectLogin();
                            return;
                        }
                        List<CustomerPreferenceDto> prefs = response2.isSuccessful() && response2.body() != null
                                ? response2.body()
                                : List.of();
                        Map<Integer, PreferenceType> byTag = new HashMap<>();
                        for (CustomerPreferenceDto p : prefs) {
                            if (p.tagId != null && p.preferenceType != null) {
                                byTag.put(p.tagId, p.preferenceType);
                            }
                        }
                        buildRows(tags, byTag);
                    }

                    @Override
                    public void onFailure(Call<List<CustomerPreferenceDto>> call2, Throwable t) {
                        loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(CustomerPreferencesActivity.this,
                                R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<TagDto>> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(CustomerPreferencesActivity.this,
                        R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildRows(List<TagDto> tags, Map<Integer, PreferenceType> existing) {
        container.removeAllViews();
        rows.clear();
        initialSelectionByTag.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (tags == null || tags.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.preferences_no_tags);
            empty.setTextColor(ContextCompat.getColor(this, R.color.bakery_text_secondary));
            container.addView(empty);
            return;
        }
        for (TagDto tag : tags) {
            if (tag == null || tag.id == null) {
                continue;
            }
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.item_preference_tag_row, container, false);
            TextView name = row.findViewById(R.id.tv_tag_name);
            AppCompatSpinner spinner = row.findViewById(R.id.spinner_preference);
            name.setText(tag.name != null ? tag.name : "");
            android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                    this, R.array.preference_type_spinner, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            PreferenceType pref = existing.get(tag.id);
            int initialIndex = indexForPreference(pref);
            spinner.setSelection(initialIndex);
            container.addView(row);
            rows.add(new Row(tag.id, spinner));
            initialSelectionByTag.put(tag.id, initialIndex);
        }
    }

    private static int indexForPreference(@Nullable PreferenceType pref) {
        if (pref == null) {
            return 0;
        }
        switch (pref) {
            case like:
                return 1;
            case dislike:
                return 2;
            case avoid:
                return 3;
            case allergic:
                return 4;
            default:
                return 0;
        }
    }

    @Nullable
    private static PreferenceType preferenceForIndex(int index) {
        switch (index) {
            case 1:
                return PreferenceType.like;
            case 2:
                return PreferenceType.dislike;
            case 3:
                return PreferenceType.avoid;
            case 4:
                return PreferenceType.allergic;
            default:
                return null;
        }
    }

    private void save() {
        if (!hasPreferenceChanges()) {
            Toast.makeText(this, R.string.nothing_to_save_profile, Toast.LENGTH_SHORT).show();
            return;
        }
        List<CustomerPreferenceSaveRequest.PreferenceEntry> entries = new ArrayList<>();
        for (Row row : rows) {
            PreferenceType t = preferenceForIndex(row.spinner.getSelectedItemPosition());
            if (t != null) {
                entries.add(new CustomerPreferenceSaveRequest.PreferenceEntry(row.tagId, t));
            }
        }
        CustomerPreferenceSaveRequest body = new CustomerPreferenceSaveRequest(entries);
        loadingOverlay.setVisibility(View.VISIBLE);
        api.saveMyPreferences(body).enqueue(new Callback<List<CustomerPreferenceDto>>() {
            @Override
            public void onResponse(Call<List<CustomerPreferenceDto>> call, Response<List<CustomerPreferenceDto>> response) {
                loadingOverlay.setVisibility(View.GONE);
                if (response.code() == 401 || response.code() == 403) {
                    redirectLogin();
                    return;
                }
                if (response.code() == 404) {
                    Toast.makeText(CustomerPreferencesActivity.this,
                            R.string.preferences_need_customer_profile, Toast.LENGTH_LONG).show();
                    return;
                }
                if (!response.isSuccessful()) {
                    Toast.makeText(CustomerPreferencesActivity.this,
                            getString(R.string.login_error_server, response.code()),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(CustomerPreferencesActivity.this, R.string.preferences_saved, Toast.LENGTH_SHORT).show();
                MeFragment.invalidateAiRecommendationsCache();
                finish();
                NavTransitions.applyBackwardPending(CustomerPreferencesActivity.this);
            }

            @Override
            public void onFailure(Call<List<CustomerPreferenceDto>> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(CustomerPreferencesActivity.this,
                        R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasPreferenceChanges() {
        for (Row row : rows) {
            Integer initial = initialSelectionByTag.get(row.tagId);
            int current = row.spinner.getSelectedItemPosition();
            if (initial == null || initial != current) {
                return true;
            }
        }
        return false;
    }

    private void redirectLogin() {
        SessionManager sm = new SessionManager(this);
        sm.logout();
        android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
