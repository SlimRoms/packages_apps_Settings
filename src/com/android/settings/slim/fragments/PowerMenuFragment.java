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

package com.android.settings.slim.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class PowerMenuFragment extends SettingsPreferenceFragment {

    private static final String POWER_MENU_TEXT_COLOR =
        "pref_power_menu_text_color";
    private static final String POWER_MENU_ICON_COLOR =
        "pref_power_menu_icon_color";
    private static final String POWER_MENU_COLOR_MODE =
        "pref_power_menu_color_mode";

    private ColorPickerPreference mPowerMenuColor;
    private ColorPickerPreference mPowerMenuTextColor;
    private ListPreference mPowerMenuColorMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_fragment);

        PreferenceScreen prefSet = getPreferenceScreen();

        mPowerMenuColor =
            (ColorPickerPreference) findPreference(POWER_MENU_ICON_COLOR);
        int intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.POWER_MENU_ICON_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                com.android.internal.R.color.power_menu_icon_default_color);
            mPowerMenuColor.setSummary(
                getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPowerMenuColor.setSummary(hexColor);
        }
        mPowerMenuColor.setNewPreviewColor(intColor);

        mPowerMenuColor.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.POWER_MENU_ICON_COLOR, intHex);
                return true;
            }
        });

        mPowerMenuTextColor =
            (ColorPickerPreference) findPreference(POWER_MENU_TEXT_COLOR);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.POWER_MENU_TEXT_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                com.android.internal.R.color.power_menu_text_default_color);
            mPowerMenuTextColor.setSummary(
                getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPowerMenuTextColor.setSummary(hexColor);
        }
        mPowerMenuTextColor.setNewPreviewColor(intColor);

        mPowerMenuTextColor.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.POWER_MENU_TEXT_COLOR, intHex);
                return true;
            }
        });

        mPowerMenuColorMode = (ListPreference) prefSet.findPreference(
                POWER_MENU_COLOR_MODE);
        mPowerMenuColorMode.setValue(String.valueOf(
                Settings.System.getIntForUser(getContentResolver(),
                Settings.System.POWER_MENU_ICON_COLOR_MODE, 0,
                UserHandle.USER_CURRENT_OR_SELF)));
        mPowerMenuColorMode.setSummary(mPowerMenuColorMode.getEntry());

        mPowerMenuColorMode.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                String val = (String) newValue;
                Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_ICON_COLOR_MODE,
                    Integer.valueOf(val));
                int index = mPowerMenuColorMode.findIndexOfValue(val);
                mPowerMenuColorMode.setSummary(
                    mPowerMenuColorMode.getEntries()[index]);
                updateColorPreference();
                return true;
            }
        });
        updateColorPreference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        // our container already takes care of the padding
        if (list != null) {
            int paddingTop = list.getPaddingTop();
            int paddingBottom = list.getPaddingBottom();
            list.setPadding(0, paddingTop, 0, paddingBottom);
        }
        return view;
    }

    private void updateColorPreference() {
        int colorMode = Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_ICON_COLOR_MODE, 0);
        mPowerMenuColor.setEnabled(colorMode != 3);
    }

}
