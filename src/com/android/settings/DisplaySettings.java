/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import com.android.internal.view.RotationPolicy;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.notification.DropDownPreference.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.settings.slim.DisplayRotation;

import java.util.ArrayList;
import java.util.List;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_LCD_DENSITY = "lcd_density";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE = "doze";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED = "wakeup_when_plugged_unplugged";
    private static final String KEY_WAKEUP_CATEGORY = "category_wakeup_options";
    private static final String KEY_VOLUME_WAKE = "pref_volume_wake";
    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";
    private static final String KEY_SCREEN_OFF_GESTURE_SETTINGS = "screen_off_gesture_settings";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";


    private PreferenceScreen mDisplayRotationPreference;
    private WarnedListPreference mFontSizePref;
    private SwitchPreference mWakeUpWhenPluggedOrUnplugged;
    private PreferenceCategory mWakeUpOptions;
    private final Configuration mCurConfig = new Configuration();
    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mDozePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mVolumeWake;
    private ListPreference mLcdDensityPreference;

    private ContentObserver mAccelerometerRotationObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mLcdDensityPreference = (ListPreference) findPreference(KEY_LCD_DENSITY);
        if (mLcdDensityPreference != null) {
            int defaultDensity = DisplayMetrics.DENSITY_DEVICE;
            int currentDensity = DisplayMetrics.DENSITY_CURRENT;
            if (currentDensity < 10 || currentDensity >= 1000) {
                // Unsupported value, force default
                currentDensity = defaultDensity;
            }

            int factor = defaultDensity >= 480 ? 40 : 20;
            int minimumDensity = defaultDensity - 4 * factor;
            int currentIndex = -1;
            String[] densityEntries = new String[7];
            String[] densityValues = new String[7];
            for (int idx = 0; idx < 7; ++idx) {
                int val = minimumDensity + factor * idx;
                int valueFormatResId = val == defaultDensity
                        ? R.string.lcd_density_default_value_format
                        : R.string.lcd_density_value_format;

                densityEntries[idx] = getString(valueFormatResId, val);
                densityValues[idx] = Integer.toString(val);
                if (currentDensity == val) {
                    currentIndex = idx;
                }
            }
            mLcdDensityPreference.setEntries(densityEntries);
            mLcdDensityPreference.setEntryValues(densityValues);
            if (currentIndex != -1) {
                mLcdDensityPreference.setValueIndex(currentIndex);
            }
            mLcdDensityPreference.setOnPreferenceChangeListener(this);
            updateLcdDensityPreferenceDescription(currentDensity);
        }

        mFontSizePref = (WarnedListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        if (isAutomaticBrightnessAvailable(getResources())) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_BRIGHTNESS);
        }

        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }

        if (isDozeAvailable(activity)) {
            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            mDozePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE);
        }

        if (RotationPolicy.isRotationSupported(activity)) {
            mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);
        } else {
            removePreference(KEY_DISPLAY_ROTATION);
        }


        mWakeUpOptions = (PreferenceCategory) prefSet.findPreference(KEY_WAKEUP_CATEGORY);
        int counter = 0;
        mVolumeWake = (SwitchPreference) findPreference(KEY_VOLUME_WAKE);
        if (mVolumeWake != null) {
            if (!getResources().getBoolean(R.bool.config_show_volumeRockerWake)) {
                mWakeUpOptions.removePreference(mVolumeWake);
                counter++;
            } else {
                mVolumeWake.setChecked(Settings.System.getInt(resolver,
                        Settings.System.VOLUME_WAKE_SCREEN, 0) == 1);
                mVolumeWake.setOnPreferenceChangeListener(this);
            }
        }

        mWakeUpWhenPluggedOrUnplugged =
            (SwitchPreference) findPreference(KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED);
        // hide option if device is already set to never wake up
        if(!getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen)) {
                mWakeUpOptions.removePreference(mWakeUpWhenPluggedOrUnplugged);
                counter++;
        } else {
            mWakeUpWhenPluggedOrUnplugged.setChecked(Settings.System.getInt(resolver,
                        Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED, 1) == 1);
            mWakeUpWhenPluggedOrUnplugged.setOnPreferenceChangeListener(this);
        }

        boolean proximityCheckOnWait = getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (!proximityCheckOnWait) {
            counter++;
            mWakeUpOptions.removePreference(findPreference(KEY_PROXIMITY_WAKE));
            Settings.System.putInt(getContentResolver(), Settings.System.PROXIMITY_ON_WAKE, 1);
        }

        if (counter == 3) {
            prefSet.removePreference(mWakeUpOptions);
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_OFF_GESTURE_SETTINGS);

    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private void updateLcdDensityPreferenceDescription(int currentDensity) {
        final int summaryResId = currentDensity == DisplayMetrics.DENSITY_DEVICE
                ? R.string.lcd_density_default_value_format : R.string.lcd_density_value_format;
        mLcdDensityPreference.setSummary(getString(summaryResId, currentDensity));
    }

    private void updateDisplayRotationPreferenceDescription() {
        if (mDisplayRotationPreference == null) {
            return;
        }
        PreferenceScreen preference = mDisplayRotationPreference;
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                DisplayRotation.ROTATION_0_MODE|DisplayRotation.ROTATION_90_MODE
                |DisplayRotation.ROTATION_270_MODE);

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

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
        updateDisplayRotationPreferenceDescription();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();

        // Update auto brightness if it is available.
        if (mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            mAutoBrightnessPreference.setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED,
                    getActivity().getResources().getBoolean(
                    com.android.internal.R.bool.config_doze_enabled_by_default) ? 1 : 0);
            mDozePreference.setChecked(value != 0);
        }
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    private void writeLcdDensityPreference(final Context context, int value) {
        try {
            SystemProperties.set("persist.sys.lcd_density", Integer.toString(value));
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to save LCD density");
            return;
        }
        final IActivityManager am = ActivityManagerNative.asInterface(
                ServiceManager.checkService("activity"));
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                ProgressDialog dialog = new ProgressDialog(context);
                dialog.setMessage(getResources().getString(R.string.restarting_ui));
                dialog.setCancelable(false);
                dialog.setIndeterminate(true);
                dialog.show();
            }
            @Override
            protected Void doInBackground(Void... params) {
                // Give the user a second to see the dialog
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
                // Restart the UI
                try {
                    am.restart();
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to restart");
                }
                return null;
            }
        };
        task.execute();
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_LCD_DENSITY.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                writeLcdDensityPreference(preference.getContext(), value);
                updateLcdDensityPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist display density setting", e);
            }
        }
        if (KEY_VOLUME_WAKE.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.VOLUME_WAKE_SCREEN,
                    (Boolean) objValue ? 1 : 0);
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        }
        if (KEY_WAKEUP_WHEN_PLUGGED_UNPLUGGED.equals(key)) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.WAKEUP_WHEN_PLUGGED_UNPLUGGED,
                    (Boolean) objValue ? 1 : 0);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!isAutomaticBrightnessAvailable(context.getResources())) {
                        result.add(KEY_AUTO_BRIGHTNESS);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE);
                    }
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    return result;
                }
            };
}
