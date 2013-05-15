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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
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

public class PieButtonStyleSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "PieButtonStyleSettings";
    private static final String PREF_PIE_BUTTON_COLOR = "pie_button_color";
    private static final String PREF_PIE_BUTTON_PRESSED_COLOR = "pie_button_pressed_color";
    private static final String PREF_PIE_BUTTON_LONG_PRESSED_COLOR = "pie_button_long_pressed_color";
    private static final String PREF_PIE_BUTTON_OUTLINE_COLOR = "pie_button_outline_color";
    private static final String PREF_PIE_ICON_COLOR = "pie_icon_color";
    private static final String PREF_PIE_ICON_COLOR_MODE = "pie_icon_color_mode";
    private static final String PREF_PIE_BUTTON_ALPHA = "pie_button_alpha";
    private static final String PREF_PIE_BUTTON_PRESSED_ALPHA = "pie_button_pressed_alpha";

    Resources mSystemUiResources;
    private boolean mCheckPreferences;

    ColorPickerPreference mPieButtonColor;
    ColorPickerPreference mPieButtonPressedColor;
    ColorPickerPreference mPieButtonLongPressedColor;
    ColorPickerPreference mPieButtonOutlineColor;
    ColorPickerPreference mPieIconColor;
    ListPreference mPieIconColorMode;
    SeekBarPreference mPieButtonAlpha;
    SeekBarPreference mPieButtonPressedAlpha;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pie_button_style);

        PreferenceScreen prefs = getPreferenceScreen();

        PackageManager pm = mContext.getPackageManager();

        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
                Log.e("PIEButtonStyle:", "can't access systemui resources",e);
            }
        }

        mPieButtonColor = (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_COLOR);
        mPieButtonColor.setOnPreferenceChangeListener(this);

        mPieButtonPressedColor = (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_PRESSED_COLOR);
        mPieButtonPressedColor.setOnPreferenceChangeListener(this);

        mPieButtonLongPressedColor = (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_LONG_PRESSED_COLOR);
        mPieButtonLongPressedColor.setOnPreferenceChangeListener(this);

        mPieButtonOutlineColor = (ColorPickerPreference) findPreference(PREF_PIE_BUTTON_OUTLINE_COLOR);
        mPieButtonOutlineColor.setOnPreferenceChangeListener(this);

        mPieIconColor = (ColorPickerPreference) findPreference(PREF_PIE_ICON_COLOR);
        mPieIconColor.setOnPreferenceChangeListener(this);

        mPieButtonAlpha = (SeekBarPreference) findPreference(PREF_PIE_BUTTON_ALPHA);
        mPieButtonAlpha.setOnPreferenceChangeListener(this);

        mPieButtonPressedAlpha = (SeekBarPreference) findPreference(PREF_PIE_BUTTON_PRESSED_ALPHA);
        mPieButtonPressedAlpha.setOnPreferenceChangeListener(this);

        mPieIconColorMode = (ListPreference) prefs.findPreference(PREF_PIE_ICON_COLOR_MODE);
        mPieIconColorMode.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.pie_button_style, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.PIE_BUTTON_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.PIE_BUTTON_PRESSED_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.PIE_BUTTON_LONG_PRESSED_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.PIE_BUTTON_OUTLINE_COLOR, -2);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.PIE_ICON_COLOR, -2);

                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.PIE_BUTTON_ALPHA, 0.3f);
                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.PIE_BUTTON_PRESSED_ALPHA, 0.0f);

                Settings.System.putInt(getActivity().getContentResolver(),
                       Settings.System.PIE_ICON_COLOR_MODE, 0);

                updateStyleValues();
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
        if (preference == mPieButtonColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_COLOR, intHex);
            return true;
        } else if (preference == mPieButtonPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_PRESSED_COLOR, intHex);
            return true;
        } else if (preference == mPieButtonLongPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_LONG_PRESSED_COLOR, intHex);
            return true;
        } else if (preference == mPieButtonOutlineColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_OUTLINE_COLOR, intHex);
            return true;
        } else if (preference == mPieIconColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_ICON_COLOR, intHex);
            return true;
       } else if (preference == mPieButtonAlpha) {
            float val = Float.parseFloat((String) newValue);
            Log.e("R", "value: " + val / 100);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_ALPHA,
                    val / 100);
            return true;
       } else if (preference == mPieButtonPressedAlpha) {
            float val = Float.parseFloat((String) newValue);
            Log.e("R", "value: " + val / 100);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_PRESSED_ALPHA,
                    val / 100);
            return true;
       } else if (preference == mPieIconColorMode) {
            int index = mPieIconColorMode.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_ICON_COLOR_MODE,
                    value);
            mPieIconColorMode.setSummary(mPieIconColorMode.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStyleValues();
    }

    private void updateStyleValues() {
        mCheckPreferences = false;
        String hexColor;
        int intColor;

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier("pie_background_color", "color", "com.android.systemui"));
            mPieButtonColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonColor.setSummary(hexColor);
        }
        mPieButtonColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_PRESSED_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier("pie_selected_color", "color", "com.android.systemui"));
            mPieButtonPressedColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonPressedColor.setSummary(hexColor);
        }
        mPieButtonPressedColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_LONG_PRESSED_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier("pie_long_pressed_color", "color", "com.android.systemui"));
            mPieButtonLongPressedColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonLongPressedColor.setSummary(hexColor);
        }
        mPieButtonLongPressedColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTON_OUTLINE_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier("pie_outline_color", "color", "com.android.systemui"));
            mPieButtonOutlineColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieButtonOutlineColor.setSummary(hexColor);
        }
        mPieButtonOutlineColor.setNewPreviewColor(intColor);

        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.PIE_ICON_COLOR, -2);
        if (intColor == -2) {
            intColor = mSystemUiResources.getColor(
                    mSystemUiResources.getIdentifier("pie_foreground_color", "color", "com.android.systemui"));
            mPieIconColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPieIconColor.setSummary(hexColor);
        }
        mPieIconColor.setNewPreviewColor(intColor);

        float defaultAlpha;
        try{
            defaultAlpha = Settings.System.getFloat(getActivity()
                     .getContentResolver(), Settings.System.PIE_BUTTON_ALPHA);
        } catch (Exception e) {
            defaultAlpha = 0.3f;
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_ALPHA, defaultAlpha);
        }
        mPieButtonAlpha.setProperty(Settings.System.PIE_BUTTON_ALPHA);
        mPieButtonAlpha.setInitValue((int) (defaultAlpha * 100));

        try{
            defaultAlpha = Settings.System.getFloat(getActivity()
                     .getContentResolver(), Settings.System.PIE_BUTTON_PRESSED_ALPHA);
        } catch (Exception e) {
            defaultAlpha = 0.0f;
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTON_PRESSED_ALPHA, defaultAlpha);
        }
        mPieButtonPressedAlpha.setProperty(Settings.System.PIE_BUTTON_PRESSED_ALPHA);
        mPieButtonPressedAlpha.setInitValue((int) (defaultAlpha * 100));

        int pieIconColorMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_ICON_COLOR_MODE, 0);
        mPieIconColorMode.setValue(String.valueOf(pieIconColorMode));
        mPieIconColorMode.setSummary(mPieIconColorMode.getEntry());

        mCheckPreferences = true;
    }

}
