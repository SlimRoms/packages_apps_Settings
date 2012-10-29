/*
 * Copyright (C) 2012 CyanogenMod
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

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class PowerMenuSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "PowerMenuSettings";

    private static final String KEY_REBOOT = "power_menu_reboot";
    private static final String KEY_PROFILES = "power_menu_profiles";
	private static final String KEY_FULLSCREEN = "power_menu_fullscreen";
    private static final String KEY_SCREENSHOT = "power_menu_screenshot";
    private static final String KEY_AIRPLANEMODE = "power_menu_airplanemode";
    private static final String KEY_SILENTTOGGLE = "power_menu_silenttoggle";

    private CheckBoxPreference mRebootPref;
    private CheckBoxPreference mProfilesPref;
	private CheckBoxPreference mExpandDesktopModeOn;
    private CheckBoxPreference mScreenshotPref;
    private CheckBoxPreference mAirplaneModePref;
    private CheckBoxPreference mSilentTogglePref;

    private final Configuration mCurConfig = new Configuration();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.power_menu_settings);

        mRebootPref = (CheckBoxPreference) findPreference(KEY_REBOOT);
        mRebootPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_REBOOT_ENABLED, 1) == 1));

        mProfilesPref = (CheckBoxPreference) findPreference(KEY_PROFILES);
        mProfilesPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1));
        
        mExpandDesktopModeOn = (CheckBoxPreference) findPreference(KEY_FULLSCREEN);
        mExpandDesktopModeOn.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED, 1) == 1));				

        mScreenshotPref = (CheckBoxPreference) findPreference(KEY_SCREENSHOT);
        mScreenshotPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_SCREENSHOT_ENABLED, 0) == 1));

        mAirplaneModePref = (CheckBoxPreference) findPreference(KEY_AIRPLANEMODE);
        mAirplaneModePref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_AIRPLANEMODE_ENABLED, 1) == 1));

        mSilentTogglePref = (CheckBoxPreference) findPreference(KEY_SILENTTOGGLE);
        mSilentTogglePref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_SILENTTOGGLE_ENABLED, 1) == 1));
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mRebootPref) {
            value = mRebootPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_REBOOT_ENABLED,
                    value ? 1 : 0);

        } else if (preference == mProfilesPref) {
            value = mProfilesPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SYSTEM_PROFILES_ENABLED,
                    value ? 1 : 0);
					
        } else if (preference == mExpandDesktopModeOn) {
            value = mExpandDesktopModeOn.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_EXPANDED_DESKTOP_ENABLED,
                    value ? 1 : 0);					

        } else if (preference == mScreenshotPref) {
            value = mScreenshotPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_SCREENSHOT_ENABLED,
                    value ? 1 : 0);

        } else if (preference == mAirplaneModePref) {
            value = mAirplaneModePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_AIRPLANEMODE_ENABLED,
                    value ? 1 : 0);

        } else if (preference == mSilentTogglePref) {
            value = mSilentTogglePref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_SILENTTOGGLE_ENABLED,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        return true;
    }
}
