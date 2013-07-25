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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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

public class WidgetsStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "WidgetsStyle";
    private static final String PREF_NAVBAR_WIDGETS_ALPHA = "navbar_widgets_alpha";
    private static final String PREF_NAVBAR_WIDGETS_BG_COLOR = "navbar_widgets_bg_color";
    private static final String PREF_NAVBAR_WIDGETS_TEXT_COLOR = "navbar_widgets_text_color";

    private static final int MENU_RESET = Menu.FIRST;

    private boolean mCheckPreferences;

    private SeekBarPreference mWidgetsTransparency;
    private ColorPickerPreference mWidgetsBGColor;
    private ColorPickerPreference mWidgetsTextColor;

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
        addPreferencesFromResource(R.xml.widgets_style);

        prefs = getPreferenceScreen();

        mWidgetsBGColor = (ColorPickerPreference) findPreference(PREF_NAVBAR_WIDGETS_BG_COLOR);
        mWidgetsBGColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_WIDGETS_BG_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.black);
        }
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mWidgetsBGColor.setSummary(hexColor);
        mWidgetsBGColor.setNewPreviewColor(intColor);

        mWidgetsTextColor = (ColorPickerPreference) findPreference(PREF_NAVBAR_WIDGETS_TEXT_COLOR);
        mWidgetsTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_WIDGETS_TEXT_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.holo_blue_light);
        }
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mWidgetsTextColor.setSummary(hexColor);
        mWidgetsTextColor.setNewPreviewColor(intColor);

        float widgetsTransparency;
        try{
            widgetsTransparency = Settings.System.getFloat(getActivity()
                 .getContentResolver(), Settings.System.NAVIGATION_BAR_WIDGETS_ALPHA);
        } catch (Exception e) {
            widgetsTransparency = 0.25f;
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.NAVIGATION_BAR_WIDGETS_ALPHA, widgetsTransparency);
        }
        mWidgetsTransparency = (SeekBarPreference) findPreference(PREF_NAVBAR_WIDGETS_ALPHA);
        mWidgetsTransparency.setProperty(Settings.System.NAVIGATION_BAR_WIDGETS_ALPHA);
        mWidgetsTransparency.setInitValue((int) (widgetsTransparency * 100));
        mWidgetsTransparency.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.widgets_reset)
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
        alertDialog.setTitle(R.string.widgets_reset);
        alertDialog.setMessage(R.string.widgets_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_WIDGETS_BG_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_WIDGETS_TEXT_COLOR, -2);

                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_WIDGETS_ALPHA, 0.25f);

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
        if (preference == mWidgetsTransparency) {
            float valStat = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_WIDGETS_ALPHA,
                    valStat / 100);
            return true;
        } else if (preference == mWidgetsBGColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_WIDGETS_BG_COLOR, intHex);
            return true;
        } else if (preference == mWidgetsTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_WIDGETS_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
