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
import com.android.settings.widget.SeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NavBarButtonStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NavBarButtonStyle";
    private static final String PREF_NAV_BUTTON_COLOR = "nav_button_color";
    private static final String PREF_BUTTON_TRANSPARENCY = "button_transparency";
    private static final String PREF_NAV_BUTTON_COLOR_MODE = "nav_button_color_mode";

    private boolean mCheckPreferences;

    ColorPickerPreference mNavigationBarButtonColor;
    ListPreference mNavigationBarButtonColorMode;
    SeekBarPreference mButtonAlpha;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    private PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.nav_bar_button_style);

        prefs = getPreferenceScreen();

        mNavigationBarButtonColor = (ColorPickerPreference) findPreference(PREF_NAV_BUTTON_COLOR);
        mNavigationBarButtonColor.setNewPreviewColor(0xffffffff);
        mNavigationBarButtonColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, 0x00000000);
        if (intColor == 0x00000000) {
            mNavigationBarButtonColor.setSummary(getResources().getString(R.string.none));
        } else {
            mNavigationBarButtonColor.setNewPreviewColor(intColor);
        }

        mNavigationBarButtonColorMode = (ListPreference) prefs.findPreference(PREF_NAV_BUTTON_COLOR_MODE);
        int navigationBarButtonColorMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0);
        mNavigationBarButtonColorMode.setValue(String.valueOf(navigationBarButtonColorMode));
        mNavigationBarButtonColorMode.setSummary(mNavigationBarButtonColorMode.getEntry());
        mNavigationBarButtonColorMode.setOnPreferenceChangeListener(this);

        float defaultAlpha;
        try{
            defaultAlpha = Settings.System.getFloat(getActivity()
                     .getContentResolver(), Settings.System.NAVIGATION_BAR_BUTTON_ALPHA);
        } catch (Exception e) {
            defaultAlpha = 0.3f;
                     Settings.System.putFloat(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_BUTTON_ALPHA, 0.3f);
        }
        mButtonAlpha = (SeekBarPreference) findPreference(PREF_BUTTON_TRANSPARENCY);
        mButtonAlpha.setProperty(Settings.System.NAVIGATION_BAR_BUTTON_ALPHA);
        mButtonAlpha.setInitValue((int) (defaultAlpha * 100));
        mButtonAlpha.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.nav_bar_button_style, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_BUTTON_TINT, 0x00000000);
                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_BUTTON_ALPHA, 0.3f);
                Settings.System.putInt(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0);

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
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mNavigationBarButtonColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, intHex);
            return true;
        } else if (preference == mNavigationBarButtonColorMode) {
            int index = mNavigationBarButtonColorMode.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE,
                    value);
            mNavigationBarButtonColorMode.setSummary(mNavigationBarButtonColorMode.getEntries()[index]);
            return true;
       } else if (preference == mButtonAlpha) {
            float val = Float.parseFloat((String) newValue);
            Log.e("R", "value: " + val / 100);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_ALPHA,
                    val / 100);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
