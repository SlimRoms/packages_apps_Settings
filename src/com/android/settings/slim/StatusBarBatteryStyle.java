/*
 * Copyright (C) 2012 Slimroms Project
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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.util.Date;

public class StatusBarBatteryStyle extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBarBatteryStyle";

    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_CIRCLE_BATTERY_COLOR = "status_bar_circle_battery_color";
    private static final String STATUS_BAR_CIRCLE_BATTERY_TEXT_COLOR = "status_bar_circle_battery_text_color";
    private static final String STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED = "status_bar_circle_battery_animationspeed";

    private static final String PREF_CIRCLE_COLOR_RESET = "circle_battery_reset";
    private static final String PREF_STATUS_BAR_CIRCLE_BATTERY_COLOR = "circle_battery_color";
    private static final String PREF_STATUS_BAR_CIRCLE_BATTERY_TEXT_COLOR = "circle_battery_text_color";
    private static final String PREF_STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED = "circle_battery_animation_speed";

    private static final String PREF_BATT_BAR = "battery_bar_list";
    private static final String PREF_BATT_BAR_STYLE = "battery_bar_style";
    private static final String PREF_BATT_BAR_COLOR = "battery_bar_color";
    private static final String PREF_BATT_BAR_WIDTH = "battery_bar_thickness";
    private static final String PREF_BATT_ANIMATE = "battery_bar_animate";

    private ListPreference mStatusBarBattery;
    private ColorPickerPreference mCircleColor;
    private ColorPickerPreference mCircleTextColor;
    private ListPreference mCircleAnimSpeed;
    private Preference mCircleColorReset;

    private ListPreference mBatteryBar;
    private ListPreference mBatteryBarStyle;
    private ListPreference mBatteryBarThickness;
    private CheckBoxPreference mBatteryBarChargingAnimation;
    private ColorPickerPreference mBatteryBarColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int defaultColor;
        int intColor;
        String hexColor;

        addPreferencesFromResource(R.xml.status_bar_battery_style);

        PreferenceScreen prefSet = getPreferenceScreen();

        mStatusBarBattery = (ListPreference) prefSet.findPreference(STATUS_BAR_BATTERY);
        int statusBarBattery = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(statusBarBattery));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mCircleColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_CIRCLE_BATTERY_COLOR);
        mCircleColor.setOnPreferenceChangeListener(this);
        defaultColor = getResources().getColor(
                com.android.internal.R.color.holo_blue_dark);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_COLOR, defaultColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mCircleColor.setSummary(hexColor);

        mCircleTextColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_CIRCLE_BATTERY_TEXT_COLOR);
        mCircleTextColor.setOnPreferenceChangeListener(this);
        defaultColor = getResources().getColor(
                com.android.internal.R.color.holo_blue_dark);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_TEXT_COLOR, defaultColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mCircleTextColor.setSummary(hexColor);

        mCircleAnimSpeed = (ListPreference) findPreference(PREF_STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED);
        mCircleAnimSpeed.setOnPreferenceChangeListener(this);
        mCircleAnimSpeed.setValue((Settings.System
                .getInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, 3))
                + "");
        mCircleAnimSpeed.setSummary(mCircleAnimSpeed.getEntry());

        mBatteryBar = (ListPreference) findPreference(PREF_BATT_BAR);
        mBatteryBar.setOnPreferenceChangeListener(this);
        mBatteryBar.setValue((Settings.System
                .getInt(getActivity().getContentResolver(),
                        Settings.System.STATUSBAR_BATTERY_BAR, 0))
                + "");
        mBatteryBar.setSummary(mBatteryBar.getEntry());

        mBatteryBarStyle = (ListPreference) findPreference(PREF_BATT_BAR_STYLE);
        mBatteryBarStyle.setOnPreferenceChangeListener(this);
        mBatteryBarStyle.setValue((Settings.System.getInt(getActivity()
                .getContentResolver(),
                Settings.System.STATUSBAR_BATTERY_BAR_STYLE, 0))
                + "");
        mBatteryBarStyle.setSummary(mBatteryBarStyle.getEntry());

        mBatteryBarColor = (ColorPickerPreference) findPreference(PREF_BATT_BAR_COLOR);
        mBatteryBarColor.setOnPreferenceChangeListener(this);
        defaultColor = getResources().getColor(
                com.android.internal.R.color.holo_blue_light);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_BAR_COLOR, defaultColor);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mBatteryBarColor.setSummary(hexColor);

        mBatteryBarChargingAnimation = (CheckBoxPreference) findPreference(PREF_BATT_ANIMATE);
        mBatteryBarChargingAnimation.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE, 0) == 1);

        mBatteryBarThickness = (ListPreference) findPreference(PREF_BATT_BAR_WIDTH);
        mBatteryBarThickness.setOnPreferenceChangeListener(this);
        mBatteryBarThickness.setValue((Settings.System.getInt(getActivity()
                .getContentResolver(),
                Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, 1))
                + "");
        mBatteryBarThickness.setSummary(mBatteryBarThickness.getEntry());

        mCircleColorReset = (Preference) findPreference(PREF_CIRCLE_COLOR_RESET);
        if (Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_RESET, 0) == 1) {
            circleColorReset();
        }

        updateBatteryBarOptions();
        updateBatteryIconOptions();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;

        if (preference == mStatusBarBattery) {
            int statusBarBattery = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, statusBarBattery);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            updateBatteryIconOptions();
            return true;
        } else if (preference == mCircleColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_COLOR, intHex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_RESET, 0);
            return true;
        } else if (preference == mCircleTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_TEXT_COLOR, intHex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_RESET, 0);
            return true;
        } else if (preference == mBatteryBarColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_BAR_COLOR, intHex);
            return true;
        } else if (preference == mCircleAnimSpeed) {
            int val = Integer.parseInt((String) newValue);
            int index = mCircleAnimSpeed.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_ANIMATIONSPEED, val);
            mCircleAnimSpeed.setSummary(mCircleAnimSpeed.getEntries()[index]);
            return true;
        } else if (preference == mBatteryBar) {
            int val = Integer.parseInt((String) newValue);
            int index = mBatteryBar.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_BAR, val);
            mBatteryBar.setSummary(mBatteryBar.getEntries()[index]);
            updateBatteryBarOptions();
            return true;
        } else if (preference == mBatteryBarStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mBatteryBarStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_BAR_STYLE, val);
            mBatteryBarStyle.setSummary(mBatteryBarStyle.getEntries()[index]);
            return true;
        } else if (preference == mBatteryBarThickness) {
            int val = Integer.parseInt((String) newValue);
            int index = mBatteryBarThickness.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, val);
            mBatteryBarThickness.setSummary(mBatteryBarThickness.getEntries()[index]);
            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mBatteryBarChargingAnimation) {

            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mCircleColorReset) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CIRCLE_BATTERY_RESET, 1);
            circleColorReset();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void circleColorReset() {
        int defaultColor = getResources().getColor(
                com.android.internal.R.color.holo_blue_dark);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_CIRCLE_BATTERY_COLOR, defaultColor);
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_CIRCLE_BATTERY_TEXT_COLOR, defaultColor);
        String hexColor = String.format("#%08x", (0xffffffff & defaultColor));
        mCircleTextColor.setNewPreviewColor(defaultColor);
        mCircleColor.setNewPreviewColor(defaultColor);
        mCircleColor.setSummary(hexColor);
        mCircleTextColor.setSummary(hexColor);
    }

    private void updateBatteryBarOptions() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
               Settings.System.STATUSBAR_BATTERY_BAR, 0) == 0) {
            mBatteryBarStyle.setEnabled(false);
            mBatteryBarThickness.setEnabled(false);
            mBatteryBarChargingAnimation.setEnabled(false);
            mBatteryBarColor.setEnabled(false);
        } else {
            mBatteryBarStyle.setEnabled(true);
            mBatteryBarThickness.setEnabled(true);
            mBatteryBarChargingAnimation.setEnabled(true);
            mBatteryBarColor.setEnabled(true);
        }
    }

    private void updateBatteryIconOptions() {
        int batteryIconStat = Settings.System.getInt(getActivity().getContentResolver(),
               Settings.System.STATUS_BAR_BATTERY, 0);

        if (batteryIconStat == 0 || batteryIconStat == 1 || batteryIconStat == 5) {
            mCircleColor.setEnabled(false);
            mCircleTextColor.setEnabled(false);
            mCircleAnimSpeed.setEnabled(false);
            mCircleColorReset.setEnabled(false);
        } else if (batteryIconStat == 2) {
            mCircleColor.setEnabled(true);
            mCircleTextColor.setEnabled(false);
            mCircleAnimSpeed.setEnabled(true);
            mCircleColorReset.setEnabled(true);
        } else {
            mCircleColor.setEnabled(true);
            mCircleTextColor.setEnabled(true);
            mCircleAnimSpeed.setEnabled(true);
            mCircleColorReset.setEnabled(true);
        }
    }

}
