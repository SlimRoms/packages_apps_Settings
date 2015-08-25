/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2014 SlimRoms Project
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
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class DisplayRotation extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "DisplayRotation";

    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String LOCKSCREEN_ROTATION = "lockscreen_rotation";
    private static final String ROTATION_0_PREF = "display_rotation_0";
    private static final String ROTATION_90_PREF = "display_rotation_90";
    private static final String ROTATION_180_PREF = "display_rotation_180";
    private static final String ROTATION_270_PREF = "display_rotation_270";

    private SwitchPreference mAccelerometer;
    private SwitchPreference mLockScreenRotationPref;
    private SwitchPreference mRotation0Pref;
    private SwitchPreference mRotation90Pref;
    private SwitchPreference mRotation180Pref;
    private SwitchPreference mRotation270Pref;

    public static final int ROTATION_0_MODE = 1;
    public static final int ROTATION_90_MODE = 2;
    public static final int ROTATION_180_MODE = 4;
    public static final int ROTATION_270_MODE = 8;

    private ContentObserver mAccelerometerRotationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateAccelerometerRotationCheckbox();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.display_rotation);

        PreferenceScreen prefSet = getPreferenceScreen();

        mAccelerometer = (SwitchPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setPersistent(false);
        mAccelerometer.setOnPreferenceChangeListener(this);

        mLockScreenRotationPref = (SwitchPreference) prefSet.findPreference(LOCKSCREEN_ROTATION);
        mRotation0Pref = (SwitchPreference) prefSet.findPreference(ROTATION_0_PREF);
        mRotation90Pref = (SwitchPreference) prefSet.findPreference(ROTATION_90_PREF);
        mRotation180Pref = (SwitchPreference) prefSet.findPreference(ROTATION_180_PREF);
        mRotation270Pref = (SwitchPreference) prefSet.findPreference(ROTATION_270_PREF);

        boolean configEnableLockRotation = getResources().
                        getBoolean(com.android.internal.R.bool.config_enableLockScreenRotation);
        boolean lockScreenRotationEnabled = Settings.System.getInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_ROTATION, configEnableLockRotation ? 1 : 0) != 0;

        mLockScreenRotationPref.setChecked(lockScreenRotationEnabled);

        int mode = Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                        ROTATION_0_MODE|ROTATION_90_MODE|ROTATION_270_MODE);

        mRotation0Pref.setChecked((mode & ROTATION_0_MODE) != 0);
        mRotation90Pref.setChecked((mode & ROTATION_90_MODE) != 0);
        mRotation180Pref.setChecked((mode & ROTATION_180_MODE) != 0);
        mRotation270Pref.setChecked((mode & ROTATION_270_MODE) != 0);

        mLockScreenRotationPref.setOnPreferenceChangeListener(this);
        mRotation0Pref.setOnPreferenceChangeListener(this);
        mRotation90Pref.setOnPreferenceChangeListener(this);
        mRotation180Pref.setOnPreferenceChangeListener(this);
        mRotation270Pref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateState();
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    private void updateState() {
        updateAccelerometerRotationCheckbox();
    }

    private void updateAccelerometerRotationCheckbox() {
        mAccelerometer.setChecked(!RotationPolicy.isRotationLocked(getActivity()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mAccelerometer) {
            RotationPolicy.setRotationLockForAccessibility(getActivity(),
                !((Boolean) objValue));
        } else if (preference == mRotation0Pref ||
                preference == mRotation90Pref ||
                preference == mRotation180Pref ||
                preference == mRotation270Pref) {
            int mode = 0;
            if (preference == mRotation0Pref && ((Boolean) objValue)
                    || preference != mRotation0Pref && mRotation0Pref.isChecked()) {
                mode |= ROTATION_0_MODE;
            }
            if (preference == mRotation90Pref && ((Boolean) objValue)
                    || preference != mRotation90Pref && mRotation90Pref.isChecked()) {
                mode |= ROTATION_90_MODE;
            }
            if (preference == mRotation180Pref && ((Boolean) objValue)
                    || preference != mRotation180Pref && mRotation180Pref.isChecked()) {
                mode |= ROTATION_180_MODE;
            }
            if (preference == mRotation270Pref && ((Boolean) objValue)
                    || preference != mRotation270Pref && mRotation270Pref.isChecked()) {
                mode |= ROTATION_270_MODE;
            }
            if (mode == 0) {
                mode |= ROTATION_0_MODE;
                mRotation0Pref.setChecked(true);
            }
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION_ANGLES, mode);
            return true;
        } else if (preference == mLockScreenRotationPref) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ROTATION, ((Boolean) objValue) ? 1 : 0);
            return true;
        }
        return false;
    }

}
