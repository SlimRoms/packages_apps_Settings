/*
 * Copyright (C) 2013 SlimRoms Project
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
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.internal.util.slim.DeviceUtils;
import com.android.internal.widget.LockPatternUtils;

public class LockscreenWidgets extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String TAG = "LockscreenWidgets";

    private static final String KEY_ENABLE_WIDGETS =
            "lockscreen_enable_widgets";
    private static final String KEY_LOCKSCREEN_ALL_WIDGETS =
            "lockscreen_all_widgets";
    private static final String KEY_DISABLE_FRAME =
            "lockscreen_disable_frame";
    private static final String KEY_LOCKSCREEN_CAMERA_WIDGET =
            "lockscreen_camera_widget";
    private static final String KEY_LOCKSCREEN_MAXIMIZE_WIDGETS =
            "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_DISABLE_HINTS =
            "lockscreen_disable_hints";
    private static final String PREF_LOCKSCREEN_USE_CAROUSEL =
            "lockscreen_use_widget_container_carousel";

    private static final int DLG_ALL_WIDGETS = 0;

    private CheckBoxPreference mEnableWidgets;
    private CheckBoxPreference mAllWidgets;
    private CheckBoxPreference mDisableFrame;
    private CheckBoxPreference mCameraWidget;
    private CheckBoxPreference mMaximizeWidgets;
    private CheckBoxPreference mLockscreenHints;
    private CheckBoxPreference mLockscreenUseCarousel;

    private boolean mCameraWidgetAttached;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_widgets);

        PreferenceScreen prefSet = getPreferenceScreen();

        mEnableWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        final boolean enabled = new LockPatternUtils(getActivity()).getWidgetsEnabled();
        if (!enabled) {
            mEnableWidgets.setSummary(R.string.disabled);
        } else {
            mEnableWidgets.setSummary(R.string.enabled);
        }
        mEnableWidgets.setChecked(enabled);
        mEnableWidgets.setOnPreferenceChangeListener(this);

        mAllWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ALL_WIDGETS);
        mAllWidgets.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0) == 1);
        mAllWidgets.setOnPreferenceChangeListener(this);

        Resources keyguardResources = null;
        PackageManager pm = getPackageManager();
        try {
            keyguardResources = pm.getResourcesForApplication("com.android.keyguard");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mDisableFrame = (CheckBoxPreference) findPreference(KEY_DISABLE_FRAME);
        mDisableFrame.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED, 0) == 1);
        mDisableFrame.setOnPreferenceChangeListener(this);

        final boolean cameraDefault = keyguardResources != null
                ? keyguardResources.getBoolean(keyguardResources.getIdentifier(
                "com.android.keyguard:bool/kg_enable_camera_default_widget", null, null)) : false;

        DevicePolicyManager dpm = (DevicePolicyManager)getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mCameraWidget = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_CAMERA_WIDGET);
        if (dpm.getCameraDisabled(null)
                || (dpm.getKeyguardDisabledFeatures(null)
                    & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA) != 0) {
            prefSet.removePreference(mCameraWidget);
        } else {
            mCameraWidgetAttached = true;
            mCameraWidget.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_WIDGET, cameraDefault ? 1 : 0) == 1);
            mCameraWidget.setOnPreferenceChangeListener(this);
        }

        mMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MAXIMIZE_WIDGETS);
        if (!DeviceUtils.isPhone(getActivity())) {
            if (mMaximizeWidgets != null) {
                prefSet.removePreference(mMaximizeWidgets);
            }
        } else {
            mMaximizeWidgets.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, 0) == 1);
        }
        mMaximizeWidgets.setOnPreferenceChangeListener(this);

        mLockscreenHints = (CheckBoxPreference)findPreference(KEY_LOCKSCREEN_DISABLE_HINTS);
        mLockscreenHints.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_DISABLE_HINTS, 1) == 1);
        mLockscreenHints.setOnPreferenceChangeListener(this);

        mLockscreenUseCarousel = (CheckBoxPreference)findPreference(PREF_LOCKSCREEN_USE_CAROUSEL);
        mLockscreenUseCarousel.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_USE_WIDGET_CONTAINER_CAROUSEL, 0) == 1);
        mLockscreenUseCarousel.setOnPreferenceChangeListener(this);

        updatePreferences(!mEnableWidgets.isChecked()
                && (mCameraWidgetAttached ? !mCameraWidget.isChecked() : true));
    }

    private void updatePreferences(boolean disable) {
        mLockscreenUseCarousel.setEnabled(!disable);
        mLockscreenHints.setEnabled(!disable);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnableWidgets) {
            new LockPatternUtils(getActivity()).setWidgetsEnabled((Boolean) newValue);
            mEnableWidgets.setSummary((Boolean) newValue ? R.string.enabled : R.string.disabled);
            updatePreferences(!((Boolean) newValue)
                    && (mCameraWidgetAttached ? !mCameraWidget.isChecked() : true));
            return true;
        } else if (preference == mAllWidgets) {
            final boolean checked = (Boolean) newValue;
            if (checked) {
                showDialogInner(DLG_ALL_WIDGETS);
            } else {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0);
            }
            return true;
        } else if (preference == mDisableFrame) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mCameraWidget) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_CAMERA_WIDGET,
                    (Boolean) newValue ? 1 : 0);
            updatePreferences(!((Boolean) newValue) && !mEnableWidgets.isChecked());
            return true;
        } else if (preference == mMaximizeWidgets) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mLockscreenHints) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_DISABLE_HINTS,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mLockscreenUseCarousel) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_USE_WIDGET_CONTAINER_CAROUSEL,
                    (Boolean) newValue ? 1 : 0);
            return true;
        }
        return false;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        LockscreenWidgets getOwner() {
            return (LockscreenWidgets) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_ALL_WIDGETS:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lockscreen_allow_all_title)
                    .setMessage(R.string.lockscreen_allow_all_warning)
                    .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            disableSetting();
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 1);
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_ALL_WIDGETS:
                    disableSetting();
                    break;
                default:
                    // None for now
            }
        }

        private void disableSetting() {
            if (getOwner().mAllWidgets != null) {
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0);
                getOwner().mAllWidgets.setChecked(false);
            }
        }
    }

}

