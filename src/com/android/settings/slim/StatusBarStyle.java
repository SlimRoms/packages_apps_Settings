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

public class StatusBarStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "StatusBarStyle";
    private static final String PREF_STATUS_BAR_ALPHA = "status_bar_alpha";
    private static final String PREF_STATUS_BAR_ALPHA_MODE = "status_bar_alpha_mode";
    private static final String PREF_STATUS_BAR_COLOR = "status_bar_color";
    private static final String PREF_STATUS_BAR_COLOR_MODE = "status_bar_color_mode";

    private static final int MENU_RESET = Menu.FIRST;

    private boolean mCheckPreferences;

    private SeekBarPreference mStatusbarTransparency;
    private ColorPickerPreference mStatusBarColor;
    private ListPreference mAlphaMode;
    private CheckBoxPreference mColorMode;

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
        addPreferencesFromResource(R.xml.status_bar_style);

        prefs = getPreferenceScreen();

        mStatusBarColor = (ColorPickerPreference) findPreference(PREF_STATUS_BAR_COLOR);
        mStatusBarColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.black);
            mStatusBarColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mStatusBarColor.setSummary(hexColor);
        }
        mStatusBarColor.setNewPreviewColor(intColor);

        float statBarTransparency = 0.0f;
        try{
            statBarTransparency = Settings.System.getFloat(getActivity()
                 .getContentResolver(), Settings.System.STATUS_BAR_ALPHA);
        } catch (Exception e) {
            statBarTransparency = 0.0f;
            Settings.System.putFloat(getActivity().getContentResolver(), Settings.System.STATUS_BAR_ALPHA, 0.0f);
        }
        mStatusbarTransparency = (SeekBarPreference) findPreference(PREF_STATUS_BAR_ALPHA);
        mStatusbarTransparency.setProperty(Settings.System.STATUS_BAR_ALPHA);
        mStatusbarTransparency.setInitValue((int) (statBarTransparency * 100));
        mStatusbarTransparency.setOnPreferenceChangeListener(this);

        mAlphaMode = (ListPreference) prefs.findPreference(PREF_STATUS_BAR_ALPHA_MODE);
        int alphaMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1);
        mAlphaMode.setValue(String.valueOf(alphaMode));
        mAlphaMode.setSummary(mAlphaMode.getEntry());
        mAlphaMode.setOnPreferenceChangeListener(this);

        mColorMode = (CheckBoxPreference) findPreference(PREF_STATUS_BAR_COLOR_MODE);
        mColorMode.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.STATUS_NAV_BAR_COLOR_MODE, 1) == 1);
        mColorMode.setOnPreferenceChangeListener(this);

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
        alertDialog.setTitle(R.string.status_bar_reset);
        alertDialog.setMessage(R.string.status_bar_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_NAV_BAR_COLOR_MODE, 1);

                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.STATUS_BAR_ALPHA, 0.0f);

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
        if (preference == mStatusbarTransparency) {
            float valStat = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_ALPHA,
                    valStat / 100);
            return true;
        } else if (preference == mStatusBarColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_COLOR, intHex);
            return true;
        } else if (preference == mAlphaMode) {
            int alphaMode = Integer.valueOf((String) newValue);
            int index = mAlphaMode.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_NAV_BAR_ALPHA_MODE, alphaMode);
            mAlphaMode.setSummary(mAlphaMode.getEntries()[index]);
            return true;
        } else if (preference == mColorMode) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_NAV_BAR_COLOR_MODE,
                    mColorMode.isChecked() ? 0 : 1);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
