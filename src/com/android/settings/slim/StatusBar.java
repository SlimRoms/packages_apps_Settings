/*
 * Copyright (C) 2013 SlimRoms Project
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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.util.slim.DeviceUtils;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceScreen;
import android.provider.Settings.SettingNotFoundException;
import com.android.settings.Utils;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarSettings";

    private static final String KEY_STATUS_BAR_TICKER = "status_bar_ticker";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";
    private static final String KEY_STATUS_BAR_NETWORK_ARROWS= "status_bar_show_network_activity";
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_BATTERY_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_BATTERY_STYLE_TEXT = "6";

    private SwitchPreference mStatusBarBrightnessControl;
    private PreferenceScreen mClockStyle;
    private ListPreference mStatusBarBattery;
    private ListPreference mStatusBarBatteryShowPercent;

    private int mbatteryStyle;
    private int mbatteryShowPercent;
    private SwitchPreference mNetworkArrows;
    private SwitchPreference mTicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar_settings);

        ContentResolver resolver = getActivity().getContentResolver();

        PreferenceScreen prefSet = getPreferenceScreen();

        // Start observing for changes on auto brightness
        StatusBarBrightnessChangedObserver statusBarBrightnessChangedObserver =
                new StatusBarBrightnessChangedObserver(new Handler());
        statusBarBrightnessChangedObserver.startObserving();

        mTicker = (SwitchPreference) prefSet.findPreference(KEY_STATUS_BAR_TICKER);
        mTicker.setChecked(Settings.System.getInt(
                getContentResolver(), Settings.System.TICKER_ENABLED, 0) == 1);
        mTicker.setOnPreferenceChangeListener(this);

        mStatusBarBrightnessControl =
            (SwitchPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));
        mStatusBarBrightnessControl.setOnPreferenceChangeListener(this);

        mClockStyle = (PreferenceScreen) prefSet.findPreference(KEY_STATUS_BAR_CLOCK);
        updateClockStyleDescription();


        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mStatusBarBatteryShowPercent =
                (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);

        mbatteryStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);
        mStatusBarBattery.setValue(String.valueOf(mbatteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mbatteryShowPercent = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0);
        mStatusBarBatteryShowPercent.setValue(String.valueOf(mbatteryShowPercent));
        mStatusBarBatteryShowPercent.setSummary(mStatusBarBatteryShowPercent.getEntry());
        mStatusBarBatteryShowPercent.setOnPreferenceChangeListener(this);
        enableStatusBarBatteryDependents(String.valueOf(mbatteryStyle));

        mNetworkArrows = (SwitchPreference) findPreference(KEY_STATUS_BAR_NETWORK_ARROWS);
        mNetworkArrows.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 0) == 1);
        mNetworkArrows.setOnPreferenceChangeListener(this);
        int networkArrows = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 0);
        updateNetworkArrowsSummary(networkArrows);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            mbatteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.STATUS_BAR_BATTERY_STYLE, mbatteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            enableStatusBarBatteryDependents((String) newValue);
            return true;
        } else if (preference == mStatusBarBatteryShowPercent) {
            mbatteryShowPercent = Integer.valueOf((String) newValue);
            int index = mStatusBarBatteryShowPercent.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, mbatteryShowPercent);
            mStatusBarBatteryShowPercent.setSummary(
                    mStatusBarBatteryShowPercent.getEntries()[index]);
            return true;
        } else if (preference == mTicker) {
            Settings.System.putInt(resolver, Settings.System.TICKER_ENABLED,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mStatusBarBrightnessControl) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mNetworkArrows) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY,
                    ((Boolean) newValue) ? 1 : 0);
            int networkArrows = Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 0);
            updateNetworkArrowsSummary(networkArrows);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateClockStyleDescription();
        updateStatusBarBrightnessControl();
        enableStatusBarBatteryDependents(String.valueOf(mbatteryStyle));
    }

    private void updateStatusBarBrightnessControl() {
        try {
            if (mStatusBarBrightnessControl != null) {
                int mode = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    mStatusBarBrightnessControl.setEnabled(false);
                    mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
                } else {
                    mStatusBarBrightnessControl.setEnabled(true);
                    mStatusBarBrightnessControl.setSummary(
                        R.string.status_bar_toggle_brightness_summary);
                }
            }
        } catch (SettingNotFoundException e) {
        }
    }

    private void updateNetworkArrowsSummary(int value) {
        String summary = value != 0
                ? getResources().getString(R.string.enabled)
                : getResources().getString(R.string.disabled);
        mNetworkArrows.setSummary(summary);
    }

    private void updateClockStyleDescription() {
        if (mClockStyle == null) {
            return;
        }
        if (Settings.System.getInt(getContentResolver(),
               Settings.System.STATUS_BAR_CLOCK, 1) == 1) {
            mClockStyle.setSummary(getString(R.string.enabled));
        } else {
            mClockStyle.setSummary(getString(R.string.disabled));
         }
    }

    private class StatusBarBrightnessChangedObserver extends ContentObserver {
        public StatusBarBrightnessChangedObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateStatusBarBrightnessControl();
        }

        public void startObserving() {
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                    false, this);
        }
    }

    private void enableStatusBarBatteryDependents(String value) {
        boolean enabled = !(value.equals(STATUS_BAR_BATTERY_STYLE_TEXT)
                || value.equals(STATUS_BAR_BATTERY_STYLE_HIDDEN));
        mStatusBarBatteryShowPercent.setEnabled(enabled);
    }

}
