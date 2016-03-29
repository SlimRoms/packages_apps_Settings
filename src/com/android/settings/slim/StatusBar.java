/*
 * Copyright (C) 2016 SlimRoms
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
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import com.android.internal.logging.SlimMetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String STATUS_BAR_BATTERY_PERCENT = "status_bar_battery_percent";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarBatteryPercent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_settings);

        ContentResolver resolver = getActivity().getContentResolver();

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBatteryPercent = (ListPreference) findPreference(STATUS_BAR_BATTERY_PERCENT);

        int batteryStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);
        mStatusBarBattery.setValue(String.valueOf(batteryStyle));
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        int batteryPercent = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_PERCENT, 0);
        mStatusBarBatteryPercent.setValue(String.valueOf(batteryPercent));
        mStatusBarBatteryPercent.setOnPreferenceChangeListener(this);
        updateBatteryPercentPreference(batteryStyle);
    }

    @Override
    protected int getMetricsCategory() {
        return SlimMetricsLogger.STATUS_BAR_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            int batteryStyle = Integer.valueOf((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.STATUS_BAR_BATTERY_STYLE, batteryStyle);
            updateBatteryPercentPreference(batteryStyle);
            return true;
        } else if (preference == mStatusBarBatteryPercent) {
            int batteryPercent = Integer.valueOf((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_BATTERY_PERCENT, batteryPercent);
            return true;
        }
        return false;
    }

    private void updateBatteryPercentPreference(int value) {
        mStatusBarBatteryPercent.setEnabled(!(value == 4 || value == 6));
    }
}
