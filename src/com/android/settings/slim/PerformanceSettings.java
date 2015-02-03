/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.slim;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.DevelopmentSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Performance Settings
 */
public class PerformanceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "PerformanceSettings";

    private static final String CATEGORY_PROFILES = "perf_profile_prefs";

    private static final String PERF_PROFILE_PREF = "pref_perf_profile";

    private ListPreference mPerfProfilePref;

    private String[] mPerfProfileEntries;
    private String[] mPerfProfileValues;
    private String mPerfProfileDefaultEntry;

    private AlertDialog mAlertDialog;

    private PowerManager mPowerManager;

    private ContentObserver mPerformanceProfileObserver = null;

    private class PerformanceProfileObserver extends ContentObserver {
        public PerformanceProfileObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            setCurrentValue();
        }
    }

    private SharedPreferences mDevelopmentPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mDevelopmentPreferences = getActivity().getSharedPreferences(
                DevelopmentSettings.PREF_FILE, Context.MODE_PRIVATE);

        mPerfProfileDefaultEntry = getString(
                com.android.internal.R.string.config_perf_profile_default_entry);
        mPerfProfileEntries = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_entries);
        mPerfProfileValues = getResources().getStringArray(
                com.android.internal.R.array.perf_profile_values);

        addPreferencesFromResource(R.xml.performance_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        // 1. always show performance profiles, if available on this device
        // 2. only show system / graphics options if dev mode enabled
        // 3. never show individual processor control if profiles enabled

        PreferenceCategory category = (PreferenceCategory) prefSet.findPreference(CATEGORY_PROFILES);

        mPerfProfilePref = (ListPreference)prefSet.findPreference(PERF_PROFILE_PREF);
        if (mPerfProfilePref != null && !mPowerManager.hasPowerProfiles()) {
            prefSet.removePreference(category);
            mPerfProfilePref = null;
        } else {
            mPerfProfilePref.setEntries(mPerfProfileEntries);
            mPerfProfilePref.setEntryValues(mPerfProfileValues);
            setCurrentValue();
            mPerfProfilePref.setOnPreferenceChangeListener(this);
        }

        mPerformanceProfileObserver = new PerformanceProfileObserver(new Handler());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPerfProfilePref != null) {
            setCurrentValue();
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.PERFORMANCE_PROFILE), false, mPerformanceProfileObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPerfProfilePref != null) {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.unregisterContentObserver(mPerformanceProfileObserver);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mPerfProfilePref) {
                mPowerManager.setPowerProfile(String.valueOf(newValue));
                setCurrentPerfProfileSummary();
                return true;
            }
        }
        return false;
    }

    private void setCurrentPerfProfileSummary() {
        String value = mPowerManager.getPowerProfile();
        String summary = "";
        int count = mPerfProfileValues.length;
        for (int i = 0; i < count; i++) {
            try {
                if (mPerfProfileValues[i].equals(value)) {
                    summary = mPerfProfileEntries[i];
                }
            } catch (IndexOutOfBoundsException ex) {
                // Ignore
            }
        }
        mPerfProfilePref.setSummary(String.format("%s", summary));
    }

    private void setCurrentValue() {
        if (mPerfProfilePref == null) {
            return;
        }
        mPerfProfilePref.setValue(mPowerManager.getPowerProfile());
        setCurrentPerfProfileSummary();
    }
}
