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
import com.android.settings.Utils;
import com.android.settings.widget.SeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NavbarStyleDimenSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NavBarStyleDimen";
    private static final String PREF_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String PREF_NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";
    private static final String PREF_NAVIGATION_BAR_WIDTH = "navigation_bar_width";
    private static final String KEY_DIMEN_OPTIONS = "navbar_dimen";
    private static final String PREF_NAV_BAR_ALPHA = "nav_bar_alpha";
    private static final String PREF_NAV_BAR_ALPHA_MODE = "nav_bar_alpha_mode";
    private static final String PREF_NAV_BAR_COLOR = "nav_bar_color";
    private static final String PREF_NAV_BAR_COLOR_MODE = "nav_bar_color_mode";

    private static final int MENU_RESET = Menu.FIRST;

    private boolean mCheckPreferences;

    ListPreference mNavigationBarHeight;
    ListPreference mNavigationBarHeightLandscape;
    ListPreference mNavigationBarWidth;
    ListPreference mAlphaMode;
    CheckBoxPreference mColorMode;
    SeekBarPreference mNavBarTransparency;
    ColorPickerPreference mNavBarColor;

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
        addPreferencesFromResource(R.xml.navbar_style_dimen_settings);

        prefs = getPreferenceScreen();

        mNavBarColor = (ColorPickerPreference) findPreference(PREF_NAV_BAR_COLOR);
        mNavBarColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_TINT, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.black);
            mNavBarColor.setSummary(getResources().getString(R.string.color_default));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mNavBarColor.setSummary(hexColor);
        }
        mNavBarColor.setNewPreviewColor(intColor);

        float navBarTransparency = 0.0f;
        try{
            navBarTransparency = Settings.System.getFloat(getActivity()
                 .getContentResolver(), Settings.System.NAVIGATION_BAR_ALPHA);
        } catch (Exception e) {
            navBarTransparency = 0.0f;
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_ALPHA, 0.0f);
        }
        mNavBarTransparency = (SeekBarPreference) findPreference(PREF_NAV_BAR_ALPHA);
        mNavBarTransparency.setProperty(Settings.System.NAVIGATION_BAR_ALPHA);
        mNavBarTransparency.setInitValue((int) (navBarTransparency * 100));
        mNavBarTransparency.setOnPreferenceChangeListener(this);

        mAlphaMode = (ListPreference) prefs.findPreference(PREF_NAV_BAR_ALPHA_MODE);
        int alphaMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1);
        mAlphaMode.setValue(String.valueOf(alphaMode));
        mAlphaMode.setSummary(mAlphaMode.getEntry());
        mAlphaMode.setOnPreferenceChangeListener(this);

        mNavigationBarHeight = (ListPreference) findPreference(PREF_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setOnPreferenceChangeListener(this);

        mNavigationBarHeightLandscape = (ListPreference) findPreference(PREF_NAVIGATION_BAR_HEIGHT_LANDSCAPE);
        mNavigationBarHeightLandscape.setOnPreferenceChangeListener(this);

        mNavigationBarWidth = (ListPreference) findPreference(PREF_NAVIGATION_BAR_WIDTH);
        if (!Utils.isPhone(getActivity())) {
            PreferenceCategory dimenCategory = (PreferenceCategory) findPreference(KEY_DIMEN_OPTIONS);
            if (mNavigationBarWidth != null)
                dimenCategory.removePreference(mNavigationBarWidth);
        } else {
            mNavigationBarWidth.setOnPreferenceChangeListener(this);
        }

        mColorMode = (CheckBoxPreference) findPreference(PREF_NAV_BAR_COLOR_MODE);
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
        alertDialog.setTitle(R.string.navbar_reset);
        alertDialog.setMessage(R.string.navbar_dimensions_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_NAV_BAR_COLOR_MODE, 1);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_TINT, -2);

                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_ALPHA, 0.0f);
                int height = mapChosenDpToPixels(48);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE,
                        height);
                height = mapChosenDpToPixels(48);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NAVIGATION_BAR_HEIGHT,
                        height);
                height = mapChosenDpToPixels(42);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.NAVIGATION_BAR_WIDTH,
                        height);
                mNavigationBarHeight.setValue("48");
                mNavigationBarHeightLandscape.setValue("48");
                mNavigationBarWidth.setValue("42");
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
        if (preference == mNavigationBarWidth) {
            String newVal = (String) newValue;
            int dp = Integer.parseInt(newVal);
            int width = mapChosenDpToPixels(dp);
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_BAR_WIDTH,
                    width);
            return true;
        } else if (preference == mNavigationBarHeight) {
            String newVal = (String) newValue;
            int dp = Integer.parseInt(newVal);
            int height = mapChosenDpToPixels(dp);
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_BAR_HEIGHT,
                    height);
            return true;
        } else if (preference == mNavigationBarHeightLandscape) {
            String newVal = (String) newValue;
            int dp = Integer.parseInt(newVal);
            int height = mapChosenDpToPixels(dp);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE,
                    height);
            return true;
        } else if (preference == mNavBarTransparency) {
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
        } else if (preference == mColorMode) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_NAV_BAR_COLOR_MODE,
                    mColorMode.isChecked() ? 0 : 1);
            return true;
        }
        return false;
    }

    public int mapChosenDpToPixels(int dp) {
        switch (dp) {
            case 48:
                return getResources().getDimensionPixelSize(R.dimen.navigation_bar_48);
            case 44:
                return getResources().getDimensionPixelSize(R.dimen.navigation_bar_44);
            case 42:
                return getResources().getDimensionPixelSize(R.dimen.navigation_bar_42);
            case 40:
                return getResources().getDimensionPixelSize(R.dimen.navigation_bar_40);
            case 36:
                return getResources().getDimensionPixelSize(R.dimen.navigation_bar_36);
            case 30:
                return getResources().getDimensionPixelSize(R.dimen.navigation_bar_30);
            case 24:
                return getResources().getDimensionPixelSize(R.dimen.navigation_bar_24);
            case 0:
                return 0;
        }
        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
