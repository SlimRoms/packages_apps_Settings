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

import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.display.AutoRotatePreferenceController;
import com.android.settings.display.CameraGesturePreferenceController;
import com.android.settings.display.DozePreferenceController;
import com.android.settings.display.FontSizePreferenceController;
import com.android.settings.display.LiftToWakePreferenceController;
import com.android.settings.display.NightDisplayPreferenceController;
import com.android.settings.display.NightModePreferenceController;
import com.android.settings.display.ScreenSaverPreferenceController;
import com.android.settings.display.TapToWakePreferenceController;
import com.android.settings.display.ThemePreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.display.VrDisplayPreferenceController;
import com.android.settings.display.WallpaperPreferenceController;
import com.android.settings.gestures.DoubleTapScreenPreferenceController;
import com.android.settings.gestures.PickupGesturePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

import slim.provider.SlimSettings;

import static android.provider.Settings.Secure.CAMERA_GESTURE_DISABLED;
import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;
import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class DisplaySettings extends DashboardFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "DisplaySettings";

    public static final String KEY_DISPLAY_SIZE = "screen_zoom";

    private static final String KEY_PICK_UP = "gesture_pick_up_display_summary";
    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen_display_summary";

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE_CATEGORY = "category_doze_options";
    private static final String KEY_DOZE = "doze";
    private static final String KEY_ADVANCED_DOZE_OPTIONS = "advanced_doze_options";
    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_DISPLAY = "night_display";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_CAMERA_GESTURE = "camera_gesture";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_VR_DISPLAY_PREF = "vr_display_pref";
    private static final String KEY_NETWORK_NAME_DISPLAYED = "network_operator_display";
    private static final String SHOW_NETWORK_NAME_MODE = "show_network_name_mode";
    private static final int SHOW_NETWORK_NAME_ON = 1;
    private static final int SHOW_NETWORK_NAME_OFF = 0;
    private static final String PREF_KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power"; //temp
    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";

    private Preference mFontSizePref;

    private TimeoutListPreference mScreenTimeoutPreference;
    private ListPreference mNightModePreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private PreferenceCategory mDozeCategory;
    private SwitchPreference mDozePreference;
    private PreferenceScreen mAdvancedDozeOptions;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mCameraGesturePreference;
    private SwitchPreference mCameraDoubleTapPowerGesturePreference;
    private SwitchPreference mNetworkNameDisplayedPreference = null;
    private SwitchPreference mProximityCheckOnWakePreference;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        // Double tap power for camera - temp
        if (isCameraDoubleTapPowerGestureAvailable(getResources())) {
            mCameraDoubleTapPowerGesturePreference =
                    (SwitchPreference) findPreference(PREF_KEY_DOUBLE_TAP_POWER);
            mCameraDoubleTapPowerGesturePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(PREF_KEY_DOUBLE_TAP_POWER);
        }

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (TimeoutListPreference) findPreference(KEY_SCREEN_TIMEOUT);

        mFontSizePref = findPreference(KEY_FONT_SIZE);

        if (isAutomaticBrightnessAvailable(getResources())) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_BRIGHTNESS);
        }

        boolean enableOperatorName = this.getResources().
                getBoolean(com.android.internal.R.bool.config_showOperatorNameInStatusBar);
        if(enableOperatorName) {
            mNetworkNameDisplayedPreference = (SwitchPreference) findPreference(
                KEY_NETWORK_NAME_DISPLAYED);
            mNetworkNameDisplayedPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_NETWORK_NAME_DISPLAYED);
        }

        if (!NightDisplayController.isAvailable(activity)) {
            removePreference(KEY_NIGHT_DISPLAY);
        }

        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }

        mDozeCategory = (PreferenceCategory) findPreference(KEY_DOZE_CATEGORY);
        if (isDozeAvailable(activity)) {
            // Doze master switch
            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            mDozePreference.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mDozeCategory);
        }

        if (isTapToWakeAvailable(getResources())) {
            mTapToWakePreference = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
            mTapToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_TAP_TO_WAKE);
        }

        if (isCameraGestureAvailable(getResources())) {
            mCameraGesturePreference = (SwitchPreference) findPreference(KEY_CAMERA_GESTURE);
            mCameraGesturePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_CAMERA_GESTURE);
        }

        if (RotationPolicy.isRotationLockToggleVisible(activity)) {
            DropDownPreference rotatePreference =
                    (DropDownPreference) findPreference(KEY_AUTO_ROTATE);
            int rotateLockedResourceId;
            // The following block sets the string used when rotation is locked.
            // If the device locks specifically to portrait or landscape (rather than current
            // rotation), then we use a different string to include this information.
            if (allowAllRotations(activity)) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
            } else {
                if (RotationPolicy.getRotationLockOrientation(activity)
                        == Configuration.ORIENTATION_PORTRAIT) {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_portrait;
                } else {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_landscape;
                }
            }
            rotatePreference.setEntries(new CharSequence[] {
                    activity.getString(R.string.display_auto_rotate_rotate),
                    activity.getString(rotateLockedResourceId),
            });
            rotatePreference.setEntryValues(new CharSequence[] { "0", "1" });
            rotatePreference.setValueIndex(RotationPolicy.isRotationLocked(activity) ?
                    1 : 0);
            rotatePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean locked = Integer.parseInt((String) newValue) != 0;
                    MetricsLogger.action(getActivity(), MetricsEvent.ACTION_ROTATION_LOCK,
                            locked);
                    RotationPolicy.setRotationLock(activity, locked);
                    return true;
                }
            });
        } else {
            removePreference(KEY_AUTO_ROTATE);
        }

        if (isVrDisplayModeAvailable(activity)) {
            DropDownPreference vrDisplayPref =
                    (DropDownPreference) findPreference(KEY_VR_DISPLAY_PREF);
            vrDisplayPref.setEntries(new CharSequence[] {
                    activity.getString(R.string.display_vr_pref_low_persistence),
                    activity.getString(R.string.display_vr_pref_off),
            });
            vrDisplayPref.setEntryValues(new CharSequence[] { "0", "1" });

            final Context c = activity;
            int currentUser = ActivityManager.getCurrentUser();
            int current = Settings.Secure.getIntForUser(c.getContentResolver(),
                            Settings.Secure.VR_DISPLAY_MODE,
                            /*default*/Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
                            currentUser);
            vrDisplayPref.setValueIndex(current);
            vrDisplayPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.parseInt((String) newValue);
                    int u = ActivityManager.getCurrentUser();
                    if (!Settings.Secure.putIntForUser(c.getContentResolver(),
                            Settings.Secure.VR_DISPLAY_MODE,
                            i, u)) {
                        Log.e(TAG, "Could not change setting for " +
                                Settings.Secure.VR_DISPLAY_MODE);
                    }
                    return true;
                }
            });
        } else {
            removePreference(KEY_VR_DISPLAY_PREF);
        }

        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                    Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
        }

        mProximityCheckOnWakePreference = (SwitchPreference) findPreference(KEY_PROXIMITY_WAKE);
        boolean proximityCheckOnWake = getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (!proximityCheckOnWake) {
            if (mProximityCheckOnWakePreference != null) {
                removePreference(KEY_PROXIMITY_WAKE);
            }
            SlimSettings.System.putInt(resolver, SlimSettings.System.PROXIMITY_ON_WAKE, 0);
        }

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

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isCameraDoubleTapPowerGestureAvailable(Resources res) { //temp
        return res.getBoolean(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private static boolean isCameraGestureAvailable(Resources res) {
        boolean configSet = res.getInteger(
                com.android.internal.R.integer.config_cameraLaunchGestureSensorType) != -1;
        return configSet &&
                !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    private static boolean isVrDisplayModeAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        TimeoutListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (preference.isDisabledByAdmin()) {
            summary = getString(R.string.disabled_by_policy_title);
        } else if (currentTimeout < 0) {
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
                summary = getString(R.string.screen_timeout_summary, entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressiveDisclosureMixin.setTileLimit(4);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.display_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private static List<PreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new AutoBrightnessPreferenceController(context, KEY_AUTO_BRIGHTNESS));
        controllers.add(new AutoRotatePreferenceController(context));
        controllers.add(new CameraGesturePreferenceController(context));
        controllers.add(new DozePreferenceController(context));
        controllers.add(new FontSizePreferenceController(context));
        controllers.add(new LiftToWakePreferenceController(context));
        controllers.add(new NightDisplayPreferenceController(context));
        controllers.add(new NightModePreferenceController(context));
        controllers.add(new ScreenSaverPreferenceController(context));
        AmbientDisplayConfiguration ambientDisplayConfig = new AmbientDisplayConfiguration(context);
        controllers.add(new PickupGesturePreferenceController(
                context, lifecycle, ambientDisplayConfig, UserHandle.myUserId(), KEY_PICK_UP));
        controllers.add(new DoubleTapScreenPreferenceController(
                context, lifecycle, ambientDisplayConfig, UserHandle.myUserId(),
                KEY_DOUBLE_TAP_SCREEN));
        controllers.add(new TapToWakePreferenceController(context));
        controllers.add(new TimeoutPreferenceController(context, KEY_SCREEN_TIMEOUT));
        controllers.add(new VrDisplayPreferenceController(context));
        controllers.add(new WallpaperPreferenceController(context));
        controllers.add(new ThemePreferenceController(context));
        return controllers;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_DISPLAY_SIZE);
                    return keys;
                }

                @Override
                public List<PreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null);
                }
            };
}
