/*
 * Copyright (C) 2013 SlimRoms
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.Spannable;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class InterfaceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String PREF_USE_ALT_RESOLVER = "use_alt_resolver";
    private static final String PREF_HIGH_END_GFX = "high_end_gfx";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String PREF_RECENTS_RAM_BAR = "recents_ram_bar";
    private static final String PREF_RECENTS_CLEAR_ALL_ON_RIGHT = "recents_clear_all_on_right";
    private static final String CATEGORY_INTERFACE = "interface_settings_action_prefs";
    private static final String KEY_LISTVIEW_ANIMATION = "listview_animation";
    private static final String KEY_LISTVIEW_INTERPOLATOR = "listview_interpolator";

    private Preference mCustomLabel;
    private Preference mLcdDensity;
    private Preference mRamBar;
    private CheckBoxPreference mUseAltResolver;
    private CheckBoxPreference mHighEndGfx;
    private CheckBoxPreference mClearAll;
    private ListPreference mListViewAnimation;
    private ListPreference mListViewInterpolator;

    private String mCustomLabelText = null;
    private int newDensityValue;

    DensityChanger densityFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.slim_interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();
        PreferenceCategory category = (PreferenceCategory) prefs.findPreference(CATEGORY_INTERFACE);

        mUseAltResolver = (CheckBoxPreference) findPreference(PREF_USE_ALT_RESOLVER);
        mUseAltResolver.setOnPreferenceChangeListener(this);
        mUseAltResolver.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.ACTIVITY_RESOLVER_USE_ALT, 0) == 1);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        mCustomLabel.setOnPreferenceClickListener(mCustomLabelClicked);

        updateCustomLabelTextSummary();

        mLcdDensity = findPreference("lcd_density_setup");
        mLcdDensity.setOnPreferenceChangeListener(this);
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            prefs.removePreference(mLcdDensity);
        }
        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

        mHighEndGfx = (CheckBoxPreference) findPreference(PREF_HIGH_END_GFX);
        mHighEndGfx.setOnPreferenceChangeListener(this);

        if (!ActivityManager.isHighEndGfx()) {
            // Only show this if the device does not have HighEndGfx enabled natively
            try {
                mHighEndGfx.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.HIGH_END_GFX_ENABLED) == 1);
            } catch (Exception e) {
                Settings.System.putInt(getContentResolver(), Settings.System.HIGH_END_GFX_ENABLED, mHighEndGfx.isChecked() ? 1 : 0 );
            }
        } else {
            category.removePreference(mHighEndGfx);
        }

        mRamBar = findPreference(PREF_RECENTS_RAM_BAR);
        mRamBar.setOnPreferenceChangeListener(this);
        updateRamBar();

        mListViewAnimation = (ListPreference) findPreference(KEY_LISTVIEW_ANIMATION);
        int listviewanimation = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_ANIMATION, 0);
        mListViewAnimation.setValue(String.valueOf(listviewanimation));
        mListViewAnimation.setSummary(mListViewAnimation.getEntry());
        mListViewAnimation.setOnPreferenceChangeListener(this);

        mListViewInterpolator = (ListPreference) findPreference(KEY_LISTVIEW_INTERPOLATOR);
        int listviewinterpolator = Settings.System.getInt(getContentResolver(),
                Settings.System.LISTVIEW_INTERPOLATOR, 0);
        mListViewInterpolator.setValue(String.valueOf(listviewinterpolator));
        mListViewInterpolator.setSummary(mListViewInterpolator.getEntry());
        mListViewInterpolator.setOnPreferenceChangeListener(this);
        mListViewInterpolator.setEnabled(listviewanimation > 0);

        mClearAll = (CheckBoxPreference) findPreference(PREF_RECENTS_CLEAR_ALL_ON_RIGHT);
        mClearAll.setOnPreferenceChangeListener(this);
        mClearAll.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.RECENTS_CLEAR_ALL_ON_RIGHT, 0) == 1);
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    private void updateRamBar() {
        int ramBarMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode != 0)
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_enabled));
        else
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_disabled));
    }

    @Override
    public void onResume() {
        super.onResume();

        updateRamBar();
    }

    @Override
    public void onPause() {
        super.onPause();

        updateRamBar();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mUseAltResolver) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACTIVITY_RESOLVER_USE_ALT,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mHighEndGfx) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HIGH_END_GFX_ENABLED,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mListViewAnimation) {
            int listviewanimation = Integer.valueOf((String) newValue);
            int index = mListViewAnimation.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_ANIMATION,
                    listviewanimation);
            mListViewAnimation.setSummary(mListViewAnimation.getEntries()[index]);
            mListViewInterpolator.setEnabled(listviewanimation > 0);
            return true;
        } else if (preference == mListViewInterpolator) {
            int listviewinterpolator = Integer.valueOf((String) newValue);
            int index = mListViewInterpolator.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LISTVIEW_INTERPOLATOR,
                    listviewinterpolator);
            mListViewInterpolator.setSummary(mListViewInterpolator.getEntries()[index]);
            return true;
        } else if (preference == mClearAll) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.RECENTS_CLEAR_ALL_ON_RIGHT,
                (Boolean) newValue ? 1 : 0);
            return true;
        }
        return false;
    }

    public OnPreferenceClickListener mCustomLabelClicked = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    mContext.sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
            return true;
        }
    };
}
