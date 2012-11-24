/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cnd;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

public class Clock extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String KEY_SHOW_CLOCK = "lockscreen_clock";
	private static final String KEY_CLOCK_ALIGN = "lockscreen_clock_align";
	private static final String KEY_CLOCK_STYLE = "lockscreen_clock_style";

	private ListPreference mClockAlign;
    private CheckBoxPreference mClockPref;
    private CheckBoxPreference mClockStyle;

    private ContentResolver mResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResolver = getContentResolver();

        addPreferencesFromResource(R.xml.clock_prefs);

        PreferenceScreen prefSet = getPreferenceScreen();

        mClockPref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_CLOCK);
        mClockPref.setChecked(Settings.System.getInt(mResolver,
                Settings.System.LOCKSCREEN_CLOCK, 1) == 1);

		mClockAlign = (ListPreference) findPreference(KEY_CLOCK_ALIGN);
        mClockAlign.setOnPreferenceChangeListener(this);

        mClockStyle = (CheckBoxPreference) prefSet.findPreference(KEY_CLOCK_STYLE);
        mClockStyle.setChecked(Settings.System.getInt(mResolver,
                Settings.System.LOCKSCREEN_CLOCK_STYLE, 0) ==  1);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mClockPref) {
            value = mClockPref.isChecked();
            Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_CLOCK, value ? 1 : 0);
            return true;
        } else if (preference == mClockStyle) {
            value = mClockStyle.isChecked();
            Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_CLOCK_STYLE, value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set the clock align value
        if (mClockAlign != null) {
            int clockAlign = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_CLOCK_ALIGN, 2);
            mClockAlign.setValue(String.valueOf(clockAlign));
            mClockAlign.setSummary(mClockAlign.getEntries()[clockAlign]);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
         if (preference == mClockAlign) {
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_ALIGN, value);
            mClockAlign.setSummary(mClockAlign.getEntries()[value]);
            return true;
        }

        return false;
    }
}
