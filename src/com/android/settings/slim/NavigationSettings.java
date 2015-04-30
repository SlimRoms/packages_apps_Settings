/*
 * Copyright (C) 2014 TeamEos
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

import com.android.internal.util.actions.ActionUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

public class NavigationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String KEY_BUTTON_INTERFACE = "category_buttons_interface";
    private static final String KEY_NAVBAR_FORCE = "navigation_bar_force";
    private static final String KEY_BUTTON_SETTINGS = "button_settings";
    private static final String KEY_NAVBAR_INTERFACE = "category_navbar_interface";
    private static final String KEY_NAVBAR_MODE = "navigation_mode";
    private static final String KEY_NAVMODE_SETTINGS = "navigation_mode_settings";
    private static final String KEY_NAVBAR_SIZE = "navigation_bar_size";
    private static final String KEY_CATEGORY_NAVIGATION_GENERAL = "category_navbar_general";

    private SwitchPreference mNavbarForce;
    private ListPreference mNavbarMode;
    private Preference mNavbarSize;
    private PreferenceScreen mSettingsTarget;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.navigation_settings);

        mNavbarForce = (SwitchPreference) findPreference(KEY_NAVBAR_FORCE);
        mNavbarMode = (ListPreference) findPreference(KEY_NAVBAR_MODE);
        mSettingsTarget = (PreferenceScreen) findPreference(KEY_NAVMODE_SETTINGS);
        mNavbarSize = (Preference) findPreference(KEY_NAVBAR_SIZE);

        if (ActionUtils.isCapKeyDevice(getActivity())) {
            boolean forceBarEnabled = isForceNavbarEnabled();
            mNavbarForce.setChecked(forceBarEnabled);
            mNavbarForce.setOnPreferenceChangeListener(this);
            updatePrefsForForcedBar(forceBarEnabled);
        } else {
            PreferenceCategory cat = (PreferenceCategory) findPreference(KEY_BUTTON_INTERFACE);
            getPreferenceScreen().removePreference(cat);
        }

        int modeVal = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NAVIGATION_BAR_MODE, 0, UserHandle.USER_CURRENT);

        mNavbarMode.setValue(String.valueOf(modeVal));
        mNavbarMode.setOnPreferenceChangeListener(this);
        updateSummaryFromValue(mNavbarMode, R.array.navigation_mode_entries,
                R.array.navigation_mode_values);

        updateSettingsTarget(modeVal);
    }

    private boolean isForceNavbarEnabled() {
        return Settings.System.getIntForUser(getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
    }

    private void updatePrefsForForcedBar(boolean enabled) {
        mNavbarMode.setEnabled(enabled);
        mSettingsTarget.setEnabled(enabled);
        mNavbarSize.setEnabled(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mNavbarMode)) {
            int val = Integer.parseInt(((String) newValue).toString());
            boolean ret = Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_MODE, val, UserHandle.USER_CURRENT);
            mNavbarMode.setValue(String.valueOf(val));
            updateSummaryFromValue(mNavbarMode, R.array.navigation_mode_entries,
                    R.array.navigation_mode_values);
            updateSettingsTarget(val);
            return ret;
        } else if (preference.equals(mNavbarForce)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            boolean ret = Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.DEV_FORCE_SHOW_NAVBAR, enabled ? 1 : 0, UserHandle.USER_CURRENT);
            updatePrefsForForcedBar(enabled);
            return ret;
        }
        return false;
    }

    private void updateSummaryFromValue(ListPreference pref, int entryRes, int valueRes) {
        String[] entries = getResources().getStringArray(entryRes);
        String[] vals = getResources().getStringArray(valueRes);
        String currentVal = pref.getValue();
        String newEntry = "";
        for (int i = 0; i < vals.length; i++) {
            if (vals[i].equals(currentVal)) {
                newEntry = entries[i];
                break;
            }
        }
        pref.setSummary(newEntry);
    }

    private void updateSettingsTarget(int val) {
        mSettingsTarget.setFragment(getResources().getStringArray(
                R.array.navigation_settings_fragments)[val]);
        mSettingsTarget.setTitle(getResources().getStringArray(
                R.array.navigation_settings_titles)[val]);
    }
}
