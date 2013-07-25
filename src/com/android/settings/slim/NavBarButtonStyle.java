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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
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
    private static final String PREF_NAV_GLOW_COLOR = "nav_button_glow_color";
    private static final String PREF_GLOW_TIMES = "glow_times";

    private static final int MENU_RESET = Menu.FIRST;

    private boolean mCheckPreferences;

    ColorPickerPreference mNavigationBarButtonColor;
    ListPreference mNavigationBarButtonColorMode;
    SeekBarPreference mButtonAlpha;
    ListPreference mGlowTimes;
    ColorPickerPreference mNavigationBarGlowColor;

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
        addPreferencesFromResource(R.xml.navbar_button_style);

        prefs = getPreferenceScreen();

        mNavigationBarButtonColor = (ColorPickerPreference) findPreference(PREF_NAV_BUTTON_COLOR);
        mNavigationBarButtonColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.white);
            mNavigationBarButtonColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mNavigationBarButtonColor.setSummary(hexColor);
        }
        mNavigationBarButtonColor.setNewPreviewColor(intColor);

        mNavigationBarGlowColor = (ColorPickerPreference) findPreference(PREF_NAV_GLOW_COLOR);
        mNavigationBarGlowColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_TINT, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.white);
            mNavigationBarGlowColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mNavigationBarGlowColor.setSummary(hexColor);
        }
        mNavigationBarGlowColor.setNewPreviewColor(intColor);

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

        mGlowTimes = (ListPreference) findPreference(PREF_GLOW_TIMES);
        mGlowTimes.setOnPreferenceChangeListener(this);

        updateGlowTimesSummary();

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.navbar_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.navbar_reset);
        alertDialog.setMessage(R.string.navbar_button_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_BUTTON_TINT, -2);
                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_BUTTON_ALPHA, 0.3f);
                Settings.System.putInt(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_BUTTON_TINT_MODE, 0);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_GLOW_TINT, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_GLOW_DURATION[0], 50);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_GLOW_DURATION[1], 500);
                refreshSettings();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
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
        } if (preference == mNavigationBarGlowColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_TINT, intHex);
            return true;
        } else if (preference == mGlowTimes) {
            // format is (on|off) both in MS
            String value = (String) newValue;
            String[] breakIndex = value.split("\\|");
            int onTime = Integer.valueOf(breakIndex[0]);
            int offTime = Integer.valueOf(breakIndex[1]);

            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_DURATION[0], offTime);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_DURATION[1], onTime);
            updateGlowTimesSummary();
            return true;
        }
        return false;
    }

    private void updateGlowTimesSummary() {
        int resId;
        String combinedTime = Settings.System.getString(getContentResolver(),
                Settings.System.NAVIGATION_BAR_GLOW_DURATION[1]) + "|" +
                Settings.System.getString(getContentResolver(),
                        Settings.System.NAVIGATION_BAR_GLOW_DURATION[0]);

        String[] glowArray = getResources().getStringArray(R.array.glow_times_values);

        if (glowArray[0].equals(combinedTime)) {
            resId = R.string.glow_times_superquick;
            mGlowTimes.setValueIndex(0);
        } else if (glowArray[1].equals(combinedTime)) {
            resId = R.string.glow_times_quick;
            mGlowTimes.setValueIndex(1);
        } else {
            resId = R.string.glow_times_normal;
            mGlowTimes.setValueIndex(2);
        }
        mGlowTimes.setSummary(getResources().getString(resId));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
