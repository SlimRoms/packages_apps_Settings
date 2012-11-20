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
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.cnd.DisplayRotation;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.ArrayList;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "DisplaySettings";
    
    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_WAKEUP_CATEGORY = "category_wakeup_options";
    private static final String KEY_VOLUME_WAKE = "pref_volume_wake";
    private static final String KEY_LOCKSCREEN_ROTATION = "lockscreen_rotation";
    private static final String KEY_BATTERY_LED = "battery_light_settings";

    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";
    private static final String ROTATION_ANGLE_DELIM = ", ";
    private static final String ROTATION_ANGLE_DELIM_FINAL = " & ";
    private static final String LOCKSCREEN_ROTATION_MODE = "Lock screen";

    private CheckBoxPreference mVolumeWake;
    private PreferenceScreen mDisplayRotationPreference;
    private CheckBoxPreference mLockScreenRotation;
    private PreferenceScreen mBatteryLightSettings;

    private ContentObserver mAccelerometerRotationObserver = 
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    private final Configuration mCurConfig = new Configuration();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.display_settings_rom);

        mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);
        mLockScreenRotation = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ROTATION);
        mBatteryLightSettings = (PreferenceScreen) findPreference(KEY_BATTERY_LED);

        mVolumeWake = (CheckBoxPreference) findPreference(KEY_VOLUME_WAKE);
        if (mVolumeWake != null) {
            if (!getResources().getBoolean(R.bool.config_show_volumeRockerWake)) {
                getPreferenceScreen().removePreference(mVolumeWake);
                getPreferenceScreen().removePreference((PreferenceCategory) findPreference(KEY_WAKEUP_CATEGORY));
            } else {
                mVolumeWake.setChecked(Settings.System.getInt(resolver,
                        Settings.System.VOLUME_WAKE_SCREEN, 0) == 1);	
            }	
        }
        if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveBatteryLed)) {
            getPreferenceScreen().removePreference(mBatteryLightSettings);
        }
    }

    private void updateDisplayRotationPreferenceDescription() {
        PreferenceScreen preference = mDisplayRotationPreference;
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        Boolean lockScreenRotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_ROTATION, 0) != 0;
        int mode = Settings.System.getInt(getContentResolver(),
            Settings.System.ACCELEROMETER_ROTATION_ANGLES,
            DisplayRotation.ROTATION_0_MODE|DisplayRotation.ROTATION_90_MODE|DisplayRotation.ROTATION_270_MODE);
        if (mLockScreenRotation != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_enableLockScreenRotation)) {
                getPreferenceScreen().removePreference(mLockScreenRotation);
            } else {
                mLockScreenRotation.setChecked(Settings.System.getInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_ROTATION, 0) != 1);
            }
        }

        if (!rotationEnabled) {
            summary.append(getString(R.string.display_rotation_disabled));
        } else {
            ArrayList<String> rotationList = new ArrayList<String>();
            String delim = "";
            summary.append(getString(R.string.display_rotation_enabled) + " ");
            if (lockScreenRotationEnabled) {
	                rotationList.add(LOCKSCREEN_ROTATION_MODE);
            }
            if ((mode & DisplayRotation.ROTATION_0_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_0);
            }
            if ((mode & DisplayRotation.ROTATION_90_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_90);
            }
            if ((mode & DisplayRotation.ROTATION_180_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_180);
            }
            if ((mode & DisplayRotation.ROTATION_270_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_270);
            }
            for(int i=0;i<rotationList.size();i++) {
                summary.append(delim).append(rotationList.get(i));
                if (rotationList.size() >= 2 && (rotationList.size() - 2) == i) {
                    delim = " " + ROTATION_ANGLE_DELIM_FINAL + " ";
                } else {
                    delim = ROTATION_ANGLE_DELIM + " ";
                }
            }
            summary.append(" " + getString(R.string.display_rotation_unit));
        }
        preference.setSummary(summary);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateDisplayRotationPreferenceDescription();

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        if (preference == mVolumeWake) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_WAKE_SCREEN,
                    mVolumeWake.isChecked() ? 1 : 0);	
            return true;
        } else if (preference == mLockScreenRotation) {
            Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_ROTATION,
                    mLockScreenRotation.isChecked() ? 1 : 0);
            updateDisplayRotationPreferenceDescription();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        return true;
    }
}
