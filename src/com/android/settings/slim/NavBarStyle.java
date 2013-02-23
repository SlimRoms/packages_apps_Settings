/*
 * Copyright (C) 2012 Slimroms
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;
import com.android.settings.widget.SeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NavBarStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NavBarStyle";
    private static final String PREF_NAV_BAR_ALPHA = "nav_bar_alpha";
    private static final String PREF_NAV_BAR_ALPHA_MODE = "nav_bar_alpha_mode";
    private static final String PREF_NAV_BAR_COLOR = "nav_bar_color";

    private SeekBarPreference mNavBarTransparency;
    private ColorPickerPreference mNavBarColor;
    private ListPreference mAlphaMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public void refreshSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.nav_bar_style);

        prefs = getPreferenceScreen();

        mNavBarColor = (ColorPickerPreference) findPreference(PREF_NAV_BAR_COLOR);
        mNavBarColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_TINT, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.black);
        }
        mNavBarColor.setNewPreviewColor(intColor);

        float statBarTransparency = 0.0f;
        try{
            statBarTransparency = Settings.System.getFloat(getActivity()
                 .getContentResolver(), Settings.System.NAVIGATION_BAR_ALPHA);
        } catch (Exception e) {
            statBarTransparency = 0.0f;
            Settings.System.putFloat(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_ALPHA, 0.0f);
        }
        mNavBarTransparency = (SeekBarPreference) findPreference(PREF_NAV_BAR_ALPHA);
        mNavBarTransparency.setProperty(Settings.System.NAVIGATION_BAR_ALPHA);
        mNavBarTransparency.setInitValue((int) (statBarTransparency * 100));
        mNavBarTransparency.setOnPreferenceChangeListener(this);

        mAlphaMode = (ListPreference) prefs.findPreference(PREF_NAV_BAR_ALPHA_MODE);
        int alphaMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1);
        mAlphaMode.setValue(String.valueOf(alphaMode));
        mAlphaMode.setSummary(mAlphaMode.getEntry());
        mAlphaMode.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.nav_bar_style, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_TINT, -2);

                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_ALPHA, 0.0f);

                refreshSettings();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNavBarTransparency) {
            float valStat = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_ALPHA,
                    valStat / 100);
            return true;
        } else if (preference == mNavBarColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_TINT, intHex);
            return true;
        } else if (preference == mAlphaMode) {
            int alphaMode = Integer.valueOf((String) newValue);
            int index = mAlphaMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_NAV_BAR_ALPHA_MODE, alphaMode);
            mAlphaMode.setSummary(mAlphaMode.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
