/*
 * Copyright (C) 2012 Slimroms Project
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

import android.content.Context;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class WifiSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    public static final String TAG = "WifiSettings";

    private static final String KEY_COUNTRY_CODE = "wifi_countrycode";
    private static final String KEY_WIFI_PRIORITY = "wifi_priority";

    private ListPreference mCcodePref;

    private WifiManager mWifiManager;
    private Preference mWifiPriority;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.wifi_settings_rom);

        PreferenceScreen prefs = getPreferenceScreen();

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mCcodePref = (ListPreference) findPreference(KEY_COUNTRY_CODE);
        mCcodePref.setOnPreferenceChangeListener(this);

        mWifiPriority = findPreference(KEY_WIFI_PRIORITY);

        updateWifiCodeSummary();
        updateWifiPriority();

    }

    @Override
    public void onResume() {
        super.onResume();
        updateWifiCodeSummary();
        updateWifiPriority();
    }

    @Override
    public void onPause() {
        super.onResume();
        updateWifiCodeSummary();
        updateWifiPriority();
    }

    private void updateWifiCodeSummary() {
        if (mCcodePref != null) {
            String value = (mWifiManager.getCountryCode()).toUpperCase();
            if (value != null) {
                mCcodePref.setValue(value);
                mCcodePref.setSummary(mCcodePref.getEntry());
            } else {
                Log.e(TAG, "Failed to fetch country code");
            }
            if (mWifiManager.isWifiEnabled()) {
                mCcodePref.setEnabled(true);
            } else {
                mCcodePref.setEnabled(false);
                mCcodePref.setSummary(R.string.wifi_setting_countrycode_disabled);
            }
        }

    }

    private void updateWifiPriority() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiPriority.setEnabled(true);
            mWifiPriority.setSummary(R.string.wifi_setting_priority_summary);
        } else {
            mWifiPriority.setEnabled(false);
            mWifiPriority.setSummary(R.string.wifi_priority_disabled);
        }

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mCcodePref) {
            try {
                Settings.Global.putString(mContext.getContentResolver(),
                       Settings.Global.WIFI_COUNTRY_CODE_USER,
                       (String) newValue);
                mWifiManager.setCountryCode((String) newValue, true);
                int index = mCcodePref.findIndexOfValue((String) newValue);
                mCcodePref.setSummary(mCcodePref.getEntries()[index]);
                return true;
            } catch (IllegalArgumentException e) {
                Toast.makeText(getActivity(), R.string.wifi_setting_countrycode_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
