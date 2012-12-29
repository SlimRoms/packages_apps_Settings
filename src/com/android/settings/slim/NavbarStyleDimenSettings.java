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
import android.app.Dialog;
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
import com.android.settings.util.Helpers;
import com.android.settings.widget.SeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NavbarStyleDimenSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NavBarStyleDimen";
    private static final String PREF_NAV_COLOR = "nav_bar_color";
    private static final String PREF_NAV_BUTTON_COLOR = "nav_button_color";
    private static final String PREF_NAV_GLOW_COLOR = "nav_button_glow_color";
    private static final String NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String NAVIGATION_BAR_HEIGHT_LANDSCAPE = "navigation_bar_height_landscape";
    private static final String NAVIGATION_BAR_WIDTH = "navigation_bar_width";

    private static final int DIALOG_NAVBAR_HEIGHT_REBOOT = 204;

    ColorPickerPreference mNavigationBarColor;
    ColorPickerPreference mNavigationBarButtonColor;
    ColorPickerPreference mNavigationBarGlowColor;
    ListPreference mNavigationBarHeight;
    ListPreference mNavigationBarHeightLandscape;
    ListPreference mNavigationBarWidth;
    SeekBarPreference mButtonAlpha;

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
        addPreferencesFromResource(R.xml.navbar_style_dimen_settings);

        prefs = getPreferenceScreen();

        mNavigationBarColor = (ColorPickerPreference) findPreference(PREF_NAV_COLOR);
        mNavigationBarColor.setOnPreferenceChangeListener(this);
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_TINT, 0xff000000);
        String hexColor = String.format("#%08x", (0xffffffff & intColor));
        mNavigationBarColor.setNewPreviewColor(intColor);


        mNavigationBarGlowColor = (ColorPickerPreference) findPreference(PREF_NAV_GLOW_COLOR);
        mNavigationBarGlowColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_TINT, 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mNavigationBarGlowColor.setNewPreviewColor(intColor);

        mNavigationBarButtonColor = (ColorPickerPreference) findPreference(PREF_NAV_BUTTON_COLOR);
        mNavigationBarButtonColor.setNewPreviewColor(0xffffffff);
        mNavigationBarButtonColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, 0x00000000);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        if (intColor == 0x00000000) {
            mNavigationBarButtonColor.setSummary(getResources().getString(R.string.none));
        } else {
            mNavigationBarButtonColor.setNewPreviewColor(intColor);
        }

        float defaultAlpha;
        try{
            defaultAlpha = Settings.System.getFloat(getActivity()
                     .getContentResolver(), Settings.System.NAVIGATION_BAR_BUTTON_ALPHA);
        } catch (Exception e) {
            defaultAlpha = 0.3f;
                     Settings.System.putFloat(getActivity().getContentResolver(), Settings.System.NAVIGATION_BAR_BUTTON_ALPHA, 0.3f);
        }
        mButtonAlpha = (SeekBarPreference) findPreference("button_transparency");
        mButtonAlpha.setProperty(Settings.System.NAVIGATION_BAR_BUTTON_ALPHA);
        mButtonAlpha.setInitValue((int) (defaultAlpha * 100));
        mButtonAlpha.setOnPreferenceChangeListener(this);

        mNavigationBarHeight = (ListPreference) findPreference("navigation_bar_height");
        mNavigationBarHeight.setOnPreferenceChangeListener(this);

        mNavigationBarHeightLandscape = (ListPreference) findPreference("navigation_bar_height_landscape");
        mNavigationBarHeightLandscape.setOnPreferenceChangeListener(this);

        mNavigationBarWidth = (ListPreference) findPreference("navigation_bar_width");
        mNavigationBarWidth.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.nav_bar_style_dimen, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_TINT, 0xff000000);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_BUTTON_TINT, 0x00000000);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NAVIGATION_BAR_GLOW_TINT, 0xffffffff);

                Settings.System.putFloat(getActivity().getContentResolver(),
                       Settings.System.NAVIGATION_BAR_BUTTON_ALPHA, 0.3f);

                refreshSettings();
                return true;
            case R.id.reset_dimen:
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

                showDialog(DIALOG_NAVBAR_HEIGHT_REBOOT);
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
        if (preference == mNavigationBarColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_TINT, intHex);
            return true;
        } else if (preference == mNavigationBarGlowColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_GLOW_TINT, intHex);
            return true;
        } else if (preference == mNavigationBarButtonColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_BUTTON_TINT, intHex);
            return true;
        } else if (preference == mNavigationBarWidth) {
            String newVal = (String) newValue;
            int dp = Integer.parseInt(newVal);
            int width = mapChosenDpToPixels(dp);
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_BAR_WIDTH,
                    width);
            showDialog(DIALOG_NAVBAR_HEIGHT_REBOOT);
            return true;
        } else if (preference == mNavigationBarHeight) {
            String newVal = (String) newValue;
            int dp = Integer.parseInt(newVal);
            int height = mapChosenDpToPixels(dp);
            Settings.System.putInt(getContentResolver(), Settings.System.NAVIGATION_BAR_HEIGHT,
                    height);
            showDialog(DIALOG_NAVBAR_HEIGHT_REBOOT);
            return true;
        } else if (preference == mNavigationBarHeightLandscape) {
            String newVal = (String) newValue;
            int dp = Integer.parseInt(newVal);
            int height = mapChosenDpToPixels(dp);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT_LANDSCAPE,
                    height);
            showDialog(DIALOG_NAVBAR_HEIGHT_REBOOT);
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
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_NAVBAR_HEIGHT_REBOOT:
                return new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.navbar_height_dialog_title))
                        .setMessage(
                                getResources().getString(R.string.navbar_height_dialog_summary))
                        .setCancelable(false)
                        .setNeutralButton(
                                getResources()
                                        .getString(R.string.navbar_height_dialog_button_later),
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .setPositiveButton(
                                getResources().getString(
                                        R.string.navbar_height_dialog_button_reboot),
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        PowerManager pm = (PowerManager) getActivity()
                                                .getSystemService(Context.POWER_SERVICE);
                                        pm.reboot("Rebooting with new bar height");
                                    }
                                })
                        .create();
        }
        return null;
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
        }
        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
