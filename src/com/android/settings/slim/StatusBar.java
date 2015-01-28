/*
 * Copyright (C) 2013-2016 SlimRoms Project
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

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import org.slim.framework.internal.logging.SlimMetricsLogger;
import org.slim.provider.SlimSettings;


public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarSettings";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_BATTERY_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_BATTERY_STYLE_TEXT = "6";

    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarBatteryShowPercent;

    private int mBatteryStyle;
    private int mBatteryShowPercent;

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.STATUS_BAR_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_settings);

        ContentResolver resolver = getActivity().getContentResolver();

        PreferenceScreen prefSet = getPreferenceScreen();

        // Start observing for changes on auto brightness

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBatteryShowPercent =
                (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);

        mBatteryStyle = SlimSettings.Secure.getInt(resolver,
                SlimSettings.Secure.STATUS_BAR_BATTERY_STYLE, 0);
        mStatusBarBattery.setValue(String.valueOf(mBatteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mBatteryShowPercent = SlimSettings.Secure.getInt(resolver,
                SlimSettings.Secure.STATUS_BAR_BATTERY_PERCENT, 0);
        mStatusBarBatteryShowPercent.setValue(String.valueOf(mBatteryShowPercent));
        mStatusBarBatteryShowPercent.setSummary(mStatusBarBatteryShowPercent.getEntry());
        mStatusBarBatteryShowPercent.setOnPreferenceChangeListener(this);
        enableStatusBarBatteryDependents(String.valueOf(mBatteryStyle));

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            mBatteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            SlimSettings.Secure.putInt(
                    resolver, SlimSettings.Secure.STATUS_BAR_BATTERY_STYLE, mBatteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            enableStatusBarBatteryDependents((String) newValue);
            return true;
        } else if (preference == mStatusBarBatteryShowPercent) {
            mBatteryShowPercent = Integer.valueOf((String) newValue);
            int index = mStatusBarBatteryShowPercent.findIndexOfValue((String) newValue);
            SlimSettings.Secure.putInt(resolver,
                    SlimSettings.Secure.STATUS_BAR_BATTERY_PERCENT, mBatteryShowPercent);
            mStatusBarBatteryShowPercent.setSummary(
                    mStatusBarBatteryShowPercent.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        enableStatusBarBatteryDependents(String.valueOf(mBatteryStyle));
    }

    private void enableStatusBarBatteryDependents(String value) {
        boolean enabled = !(value.equals(STATUS_BAR_BATTERY_STYLE_TEXT)
                || value.equals(STATUS_BAR_BATTERY_STYLE_HIDDEN));
        mStatusBarBatteryShowPercent.setEnabled(enabled);
    }
}
