/*
 * Copyright (C) 2013-2016 SlimRoms project
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

package com.android.settings.slim.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.slim.preference.SlimSeekBarPreference;
import org.slim.provider.SlimSettings;

public class DozeSettingsFragment extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String KEY_DOZE_TIMEOUT = "doze_timeout";
    private static final String KEY_DOZE_TRIGGER_PICKUP = "doze_trigger_pickup";
    private static final String KEY_DOZE_TRIGGER_SIGMOTION = "doze_trigger_sigmotion";
    private static final String KEY_DOZE_TRIGGER_NOTIFICATION = "doze_trigger_notification";
    private static final String KEY_DOZE_SCHEDULE = "doze_schedule";
    private static final String KEY_DOZE_BRIGHTNESS = "doze_brightness";
    private static final String KEY_DOZE_WAKEUP_DOUBLETAP = "doze_wakeup_doubletap";

    private static final String SYSTEMUI_METADATA_NAME = "com.android.systemui";

    private SlimSeekBarPreference mDozeTimeout;
    private SwitchPreference mDozeTriggerPickup;
    private SwitchPreference mDozeTriggerSigmotion;
    private SwitchPreference mDozeTriggerNotification;
    private SwitchPreference mDozeSchedule;
    private SlimSeekBarPreference mDozeBrightness;
    private SwitchPreference mDozeWakeupDoubleTap;

    private float mBrightnessScale;
    private float mDefaultBrightnessScale;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        PreferenceScreen prefSet = getPreferenceScreen();
        Resources res = getResources();

        addPreferencesFromResource(R.xml.doze_settings);

        // Doze timeout seekbar
        mDozeTimeout = (SlimSeekBarPreference) findPreference(KEY_DOZE_TIMEOUT);
        mDozeTimeout.setDefault(dozeTimeoutDefault(activity));
        mDozeTimeout.isMilliseconds(true);
        mDozeTimeout.setInterval(1);
        mDozeTimeout.minimumValue(200);
        mDozeTimeout.multiplyValue(200);
        mDozeTimeout.setOnPreferenceChangeListener(this);

        // Doze triggers
        if (isPickupSensorUsedByDefault(activity)) {
            mDozeTriggerPickup = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_PICKUP);
            mDozeTriggerPickup.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_PICKUP);
        }
        if (isSigmotionSensorUsedByDefault(activity)) {
            mDozeTriggerSigmotion = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_SIGMOTION);
            mDozeTriggerSigmotion.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE_TRIGGER_SIGMOTION);
        }
        mDozeTriggerNotification = (SwitchPreference) findPreference(KEY_DOZE_TRIGGER_NOTIFICATION);
        mDozeTriggerNotification.setOnPreferenceChangeListener(this);

        // Doze schedule
        mDozeSchedule = (SwitchPreference) findPreference(KEY_DOZE_SCHEDULE);
        mDozeSchedule.setOnPreferenceChangeListener(this);

        // Doze brightness
        mDefaultBrightnessScale =
                (float) res.getInteger(
                com.android.internal.R.integer.config_screenBrightnessDoze) / res.getInteger(
                com.android.internal.R.integer.config_screenBrightnessSettingMaximum);
        float defaultBrightness = (mDefaultBrightnessScale * 100);
        mDozeBrightness = (SlimSeekBarPreference) findPreference(KEY_DOZE_BRIGHTNESS);
        mDozeBrightness.setDefault((int) defaultBrightness);
        mDozeBrightness.setInterval(1);
        mDozeBrightness.setOnPreferenceChangeListener(this);

        // Double-tap to wake from doze
        mDozeWakeupDoubleTap = (SwitchPreference) findPreference(KEY_DOZE_WAKEUP_DOUBLETAP);
        mDozeWakeupDoubleTap.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDozeTimeout) {
            int dozeTimeout = Integer.valueOf((String) newValue);
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TIMEOUT, dozeTimeout);
        }
        if (preference == mDozeTriggerPickup) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_PICKUP, value ? 1 : 0);
        }
        if (preference == mDozeTriggerSigmotion) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_SIGMOTION, value ? 1 : 0);
        }
        if (preference == mDozeTriggerNotification) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_NOTIFICATION, value ? 1 : 0);
        }
        if (preference == mDozeSchedule) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_SCHEDULE, value ? 1 : 0);
        }
        if (preference == mDozeBrightness) {
            float valNav = Float.parseFloat((String) newValue);
            SlimSettings.System.putFloat(getContentResolver(),
                    SlimSettings.System.DOZE_BRIGHTNESS, valNav / 100);
        }
        if (preference == mDozeWakeupDoubleTap) {
            boolean value = (Boolean) newValue;
            SlimSettings.System.putInt(getContentResolver(),
                    SlimSettings.System.DOZE_WAKEUP_DOUBLETAP, value ? 1 : 0);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
    }

    private void updateState() {
        final Activity activity = getActivity();

        // Update doze preferences
        if (mDozeTimeout != null) {
            final int statusDozeTimeout = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TIMEOUT, dozeTimeoutDefault(activity));
            // minimum 200 is 1 interval of the 200 multiplier
            mDozeTimeout.setInitValue((statusDozeTimeout / 200) - 1);
        }
        if (mDozeTriggerPickup != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_PICKUP, 1);
            mDozeTriggerPickup.setChecked(value != 0);
        }
        if (mDozeTriggerSigmotion != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_SIGMOTION, 1);
            mDozeTriggerSigmotion.setChecked(value != 0);
        }
        if (mDozeTriggerNotification != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_TRIGGER_NOTIFICATION, 1);
            mDozeTriggerNotification.setChecked(value != 0);
        }
        if (mDozeSchedule != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_SCHEDULE, 1);
            mDozeSchedule.setChecked(value != 0);
        }
        if (mDozeBrightness != null) {
            mBrightnessScale = SlimSettings.System.getFloat(getContentResolver(),
                    SlimSettings.System.DOZE_BRIGHTNESS, mDefaultBrightnessScale);
            mDozeBrightness.setInitValue((int) (mBrightnessScale * 100));
        }
        if (mDozeWakeupDoubleTap != null) {
            int value = SlimSettings.System.getInt(getContentResolver(),
                    SlimSettings.System.DOZE_WAKEUP_DOUBLETAP, 0);
            mDozeWakeupDoubleTap.setChecked(value != 0);
        }
    }

    private static boolean isPickupSensorUsedByDefault(Context context) {
        return getConfigBoolean(context, "doze_pulse_on_pick_up");
    }

    private static boolean isSigmotionSensorUsedByDefault(Context context) {
        return getConfigBoolean(context, "doze_pulse_on_significant_motion");
    }

    private static int dozeTimeoutDefault(Context context) {
        return getConfigInteger(context, "doze_pulse_duration_visible");
    }

    private static Boolean getConfigBoolean(Context context, String configBooleanName) {
        int resId = -1;
        Boolean b = true;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("DozeSettings:", "can't access systemui resources",e);
            return null;
        }

        resId = systemUiResources.getIdentifier(
            SYSTEMUI_METADATA_NAME + ":bool/" + configBooleanName, null, null);
        if (resId > 0) {
            b = systemUiResources.getBoolean(resId);
        }
        return b;
    }

    private static Integer getConfigInteger(Context context, String configIntegerName) {
        int resId = -1;
        Integer i = 1;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("DozeSettings:", "can't access systemui resources",e);
            return null;
        }

        resId = systemUiResources.getIdentifier(
            SYSTEMUI_METADATA_NAME + ":integer/" + configIntegerName, null, null);
        if (resId > 0) {
            i = systemUiResources.getInteger(resId);
        }
        return i;
    }
}
