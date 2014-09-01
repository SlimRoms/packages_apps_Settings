/*
 * Copyright (C) 2014 Slimroms
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

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SlimSeekBarPreference;
import android.provider.Settings;
import android.os.UserHandle;

import com.android.internal.util.slim.DeviceUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.slim.quicksettings.QuickSettingsUtil;
import com.android.settings.R;

public class QsSettings extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener  {

    public static final String TAG = "QsSettings";

    private static final String PRE_QUICK_PULLDOWN =
            "quick_pulldown";
    private static final String PRE_SMART_PULLDOWN =
            "smart_pulldown";
    private static final String PRE_COLLAPSE_PANEL =
            "collapse_panel";
    private static final String PREF_TILES_STYLE =
            "quicksettings_tiles_style";
    private static final String PREF_TILE_PICKER =
            "tile_picker";

    ListPreference mQuickPulldown;
    ListPreference mSmartPulldown;
    CheckBoxPreference mCollapsePanel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.qs_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mQuickPulldown = (ListPreference) findPreference(PRE_QUICK_PULLDOWN);
        mSmartPulldown = (ListPreference) findPreference(PRE_SMART_PULLDOWN);
        if (!DeviceUtils.isPhone(getActivity())) {
            prefs.removePreference(mQuickPulldown);
            prefs.removePreference(mSmartPulldown);
        } else {
            // Quick Pulldown
            mQuickPulldown.setOnPreferenceChangeListener(this);
            int statusQuickPulldown = Settings.System.getInt(getContentResolver(),
                    Settings.System.QS_QUICK_PULLDOWN, 0);
            mQuickPulldown.setValue(String.valueOf(statusQuickPulldown));
            updateQuickPulldownSummary(statusQuickPulldown);

            // Smart Pulldown
            mSmartPulldown.setOnPreferenceChangeListener(this);
            int smartPulldown = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.QS_SMART_PULLDOWN, 0, UserHandle.USER_CURRENT);
            mSmartPulldown.setValue(String.valueOf(smartPulldown));
            updateSmartPulldownSummary(smartPulldown);
        }

        mCollapsePanel = (CheckBoxPreference) findPreference(PRE_COLLAPSE_PANEL);
        mCollapsePanel.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.QS_COLLAPSE_PANEL, 0, UserHandle.USER_CURRENT) == 1);
        mCollapsePanel.setOnPreferenceChangeListener(this);

        updateQuickSettingsOptions();
    }

    private void updateQuickSettingsOptions() {
        Preference tilesStyle = (Preference) findPreference(PREF_TILES_STYLE);
        Preference tilesPicker = (Preference) findPreference(PREF_TILE_PICKER);
        String qsConfig = Settings.System.getStringForUser(getContentResolver(),
                Settings.System.QUICK_SETTINGS_TILES, UserHandle.USER_CURRENT);
        boolean hideSettingsPanel = qsConfig != null && qsConfig.isEmpty();
        mQuickPulldown.setEnabled(!hideSettingsPanel);
        mSmartPulldown.setEnabled(!hideSettingsPanel);
        tilesStyle.setEnabled(!hideSettingsPanel);
        if (hideSettingsPanel) {
            tilesPicker.setSummary(getResources().getString(R.string.disable_qs));
        } else {
            tilesPicker.setSummary(getResources().getString(R.string.tile_picker_summary));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        QuickSettingsUtil.updateAvailableTiles(getActivity());
        updateQuickSettingsOptions();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQuickPulldown) {
            int statusQuickPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_QUICK_PULLDOWN,
                    statusQuickPulldown);
            updateQuickPulldownSummary(statusQuickPulldown);
            return true;
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
            return true;
        } else if (preference == mCollapsePanel) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.QS_COLLAPSE_PANEL,
                    (Boolean) newValue ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    private void updateQuickPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.quick_pulldown_off));
        } else {
            String direction = res.getString(value == 2
                    ? R.string.quick_pulldown_left
                    : R.string.quick_pulldown_right);
            mQuickPulldown.setSummary(res.getString(R.string.summary_quick_pulldown, direction));
        }
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }
}
