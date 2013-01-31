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

package com.android.settings.slim;

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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.slim.DisplayRotation;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.ArrayList;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "DisplaySettings";
    
    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_WAKEUP_CATEGORY = "category_wakeup_options";
    private static final String KEY_VOLUME_WAKE = "pref_volume_wake";
    private static final String KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED = "wakeup_when_plugged_unplugged";
    private static final String KEY_LIGHT_OPTIONS = "category_light_options";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_TOUCHKEY_LIGHT = "touchkey_light_timeout";

    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";

    private CheckBoxPreference mVolumeWake;
    private CheckBoxPreference mWakeUpWhenPluggedOrUnplugged;
    private PreferenceCategory mWakeUpOptions;
    private PreferenceScreen mDisplayRotationPreference;
    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mBatteryPulse;
    private ListPreference mTouchKeyLights;

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

        PreferenceScreen prefSet = getPreferenceScreen();

        mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);

        mWakeUpOptions = (PreferenceCategory) prefSet.findPreference(KEY_WAKEUP_CATEGORY);
        mVolumeWake = (CheckBoxPreference) findPreference(KEY_VOLUME_WAKE);
        if (mVolumeWake != null) {
            if (!getResources().getBoolean(R.bool.config_show_volumeRockerWake)) {
                mWakeUpOptions.removePreference(mVolumeWake);
            } else {
                mVolumeWake.setChecked(Settings.System.getInt(resolver,
                        Settings.System.VOLUME_WAKE_SCREEN, 0) == 1);
            }
        }

        mWakeUpWhenPluggedOrUnplugged = (CheckBoxPreference) findPreference(KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED);
        // hide option if device is already set to never wake up
        if(!getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen)) {
            if(mWakeUpWhenPluggedOrUnplugged != null)
                mWakeUpOptions.removePreference(mWakeUpWhenPluggedOrUnplugged);
        } else {
            mWakeUpWhenPluggedOrUnplugged.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED, 1) == 1);
        }

        if (!getResources().getBoolean(R.bool.config_show_volumeRockerWake) &&
            !getResources().getBoolean(com.android.internal.R.bool.config_unplugTurnsOnScreen)) {
                if (mWakeUpOptions != null)
                    getPreferenceScreen().removePreference(mWakeUpOptions);
        }

        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                getPreferenceScreen().removePreference(mNotificationPulse);
            } else {
                updateLightPulseDescription();
            }
        }

        mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
        if (mBatteryPulse != null) {
            if (getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveBatteryLed) == false) {
                getPreferenceScreen().removePreference(mBatteryPulse);
            } else {
                updateBatteryPulseDescription();
            }
        }

        mTouchKeyLights = (ListPreference) prefSet.findPreference(KEY_TOUCHKEY_LIGHT);
        if (getResources().getBoolean(R.bool.config_show_touchKeyDur) == false) {
            if (mTouchKeyLights != null) {
                getPreferenceScreen().removePreference(mTouchKeyLights);
            }
        } else {
            int touchKeyLights = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.TOUCHKEY_LIGHT_DUR, 5000);
            mTouchKeyLights.setValue(String.valueOf(touchKeyLights));
            mTouchKeyLights.setSummary(mTouchKeyLights.getEntry());
            mTouchKeyLights.setOnPreferenceChangeListener(this);
        }
    }

    private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
         }
    }

    private void updateBatteryPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.notification_light_disabled));
        }
     }

    private void updateDisplayRotationPreferenceDescription() {
        PreferenceScreen preference = mDisplayRotationPreference;
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        int mode = Settings.System.getInt(getContentResolver(),
            Settings.System.ACCELEROMETER_ROTATION_ANGLES,
            DisplayRotation.ROTATION_0_MODE|DisplayRotation.ROTATION_90_MODE|DisplayRotation.ROTATION_270_MODE);

        if (!rotationEnabled) {
            summary.append(getString(R.string.display_rotation_disabled));
        } else {
            ArrayList<String> rotationList = new ArrayList<String>();
            String delim = "";
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
            for (int i = 0; i < rotationList.size(); i++) {
                summary.append(delim).append(rotationList.get(i));
                if ((rotationList.size() - i) > 2) {
                    delim = ", ";
                } else {
                    delim = " & ";
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
        updateLightPulseDescription();
        updateBatteryPulseDescription();

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
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.VOLUME_WAKE_SCREEN,
                    mVolumeWake.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mWakeUpWhenPluggedOrUnplugged) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED,
                    mWakeUpWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mTouchKeyLights) {
            int touchKeyLights = Integer.valueOf((String) objValue);
            int index = mTouchKeyLights.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.TOUCHKEY_LIGHT_DUR, touchKeyLights);
            mTouchKeyLights.setSummary(mTouchKeyLights.getEntries()[index]);
            return true;
        }
        return false;
    }
}
