/*
 * Copyright (C) 2013 Slimroms
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
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PieControl extends SettingsPreferenceFragment
                        implements Preference.OnPreferenceChangeListener {

    private static final String PIE_CONTROL = "pie_control";
    private static final String PIE_BUTTON = "pie_button";
    private static final String PIE_SHOW_SNAP = "pie_show_snap";
    private static final String PIE_MENU = "pie_menu";
    private static final String PIE_SHOW_TEXT = "pie_show_text";
    private static final String PIE_SHOW_BACKGROUND = "pie_show_background";
    private static final String PIE_DISABLE_STATUSBAR_INFO = "pie_disable_statusbar_info";
    private static final String PIE_STYLE = "pie_style";
    private static final String PIE_TRIGGER = "pie_trigger";

    private ListPreference mPieControl;
    private CheckBoxPreference mShowSnap;
    private ListPreference mPieMenuDisplay;
    private CheckBoxPreference mShowText;
    private CheckBoxPreference mShowBackground;
    private CheckBoxPreference mDisableStatusBarInfo;
    private PreferenceScreen mStyle;
    private PreferenceScreen mTrigger;
    private PreferenceScreen mButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pie_control);

        PreferenceScreen prefSet = getPreferenceScreen();

        mShowSnap = (CheckBoxPreference) prefSet.findPreference(PIE_SHOW_SNAP);
        mShowSnap.setOnPreferenceChangeListener(this);
        mShowText = (CheckBoxPreference) prefSet.findPreference(PIE_SHOW_TEXT);
        mShowText.setOnPreferenceChangeListener(this);
        mShowBackground = (CheckBoxPreference) prefSet.findPreference(PIE_SHOW_BACKGROUND);
        mShowBackground.setOnPreferenceChangeListener(this);
        mDisableStatusBarInfo = (CheckBoxPreference) prefSet.findPreference(PIE_DISABLE_STATUSBAR_INFO);
        mDisableStatusBarInfo.setOnPreferenceChangeListener(this);
        mStyle = (PreferenceScreen) prefSet.findPreference(PIE_STYLE);
        mTrigger = (PreferenceScreen) prefSet.findPreference(PIE_TRIGGER);
        mButton = (PreferenceScreen) prefSet.findPreference(PIE_BUTTON);
        mPieControl = (ListPreference) prefSet.findPreference(PIE_CONTROL);
        mPieControl.setOnPreferenceChangeListener(this);
        mPieMenuDisplay = (ListPreference) prefSet.findPreference(PIE_MENU);
        mPieMenuDisplay.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPieControl) {
            int index = mPieControl.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_CONTROLS,
                    value);
            mPieControl.setSummary(mPieControl.getEntries()[index]);
            if (value == 0) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.PIE_DISABLE_STATUSBAR_INFO, 0);
                mDisableStatusBarInfo.setChecked(false);
            }
            propagatePieControl(value != 0);
        } else if (preference == mShowSnap) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_SHOW_SNAP, (Boolean) newValue ? 1 : 0);
        } else if (preference == mPieMenuDisplay) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_MENU, Integer.parseInt((String) newValue));
        } else if (preference == mShowText) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_SHOW_TEXT, (Boolean) newValue ? 1 : 0);
            if ((Boolean) newValue == false) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.PIE_DISABLE_STATUSBAR_INFO, 0);
                mDisableStatusBarInfo.setChecked(false);
            }
        } else if (preference == mDisableStatusBarInfo) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_DISABLE_STATUSBAR_INFO, (Boolean) newValue ? 1 : 0);
        } else if (preference == mShowBackground) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_SHOW_BACKGROUND, (Boolean) newValue ? 1 : 0);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        int pieControl = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_CONTROLS, 0);
        mPieControl.setValue(String.valueOf(pieControl));
        mPieControl.setSummary(mPieControl.getEntry());
        propagatePieControl(pieControl != 0);

        mPieMenuDisplay.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_MENU,
                2) + "");

        mShowSnap.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_SHOW_SNAP, 1) == 1);
        mShowText.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_SHOW_TEXT, 1) == 1);
        mDisableStatusBarInfo.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_DISABLE_STATUSBAR_INFO, 0) == 1);
        mShowBackground.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_SHOW_BACKGROUND, 1) == 1);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void propagatePieControl(boolean value) {
        mShowSnap.setEnabled(value);
        mShowText.setEnabled(value);
        mDisableStatusBarInfo.setEnabled(value);
        mShowBackground.setEnabled(value);
        mStyle.setEnabled(value);
        mButton.setEnabled(value);
        mTrigger.setEnabled(value);
        mPieMenuDisplay.setEnabled(value);
    }

}
