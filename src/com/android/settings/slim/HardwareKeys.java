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

package com.android.settings.slim;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Slog;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.util.ShortcutPickerHelper;
import com.android.settings.SettingsPreferenceFragment;

public class HardwareKeys extends SettingsPreferenceFragment implements
        ShortcutPickerHelper.OnPickListener, OnPreferenceChangeListener {

    private static final String HARDWARE_KEYS_CATEGORY_BINDINGS = "hardware_keys_bindings";
    private static final String HARDWARE_KEYS_ENABLE_CUSTOM = "hardware_keys_enable_custom";
    private static final String HARDWARE_KEYS_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String HARDWARE_KEYS_MENU_PRESS = "hardware_keys_menu_press";
    private static final String HARDWARE_KEYS_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String HARDWARE_KEYS_ASSIST_PRESS = "hardware_keys_assist_press";
    private static final String HARDWARE_KEYS_ASSIST_LONG_PRESS = "hardware_keys_assist_long_press";
    private static final String HARDWARE_KEYS_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String HARDWARE_KEYS_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";
    private static final String HARDWARE_KEYS_SHOW_OVERFLOW = "hardware_keys_show_overflow";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;
    private static final int ACTION_IN_APP_SEARCH = 5;
    private static final int ACTION_LAST_APP = 6;
    private static final int ACTION_POWER = 7;
    private static final int ACTION_CUSTOM_APP = 8;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    private static final int KEY_MASK_HOME = 0x01;
    private static final int KEY_MASK_BACK = 0x02;
    private static final int KEY_MASK_MENU = 0x04;
    private static final int KEY_MASK_ASSIST = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;

    private CheckBoxPreference mEnableCustomBindings;
    private ListPreference mHomeLongPressAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mAssistPressAction;
    private ListPreference mAssistLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private CheckBoxPreference mShowActionOverflow;

    private ShortcutPickerHelper mPicker;
    private Preference mCustomAppPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        addPreferencesFromResource(R.xml.hardware_keys);
        PreferenceScreen prefSet = getPreferenceScreen();

        mPicker = new ShortcutPickerHelper(this, this);

        mEnableCustomBindings = (CheckBoxPreference) prefSet.findPreference(
                HARDWARE_KEYS_ENABLE_CUSTOM);
        mHomeLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_HOME_LONG_PRESS);
        mMenuPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_MENU_PRESS);
        mMenuLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_MENU_LONG_PRESS);
        mAssistPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_ASSIST_PRESS);
        mAssistLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_ASSIST_LONG_PRESS);
        mAppSwitchPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_APP_SWITCH_PRESS);
        mAppSwitchLongPressAction = (ListPreference) prefSet.findPreference(
                HARDWARE_KEYS_APP_SWITCH_LONG_PRESS);
        mShowActionOverflow = (CheckBoxPreference) prefSet.findPreference(
                HARDWARE_KEYS_SHOW_OVERFLOW);
        PreferenceCategory bindingsCategory = (PreferenceCategory) prefSet.findPreference(
                HARDWARE_KEYS_CATEGORY_BINDINGS);

        if (hasHomeKey) {
            String homeLongPressAction = Settings.System.getString(getContentResolver(),
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION);
            if (hasAppSwitchKey) {
                if (homeLongPressAction == null)
                    homeLongPressAction = Integer.toString(ACTION_NOTHING);
            } else {
                if (homeLongPressAction == null)
                    homeLongPressAction = Integer.toString(ACTION_APP_SWITCH);
            }
            try {
                Integer.parseInt(homeLongPressAction);
                mHomeLongPressAction.setValue(homeLongPressAction);
                mHomeLongPressAction.setSummary(mHomeLongPressAction.getEntry());
            } catch (NumberFormatException e) {
                mHomeLongPressAction.setValue(Integer.toString(ACTION_CUSTOM_APP));
                mHomeLongPressAction.setSummary(mPicker.getFriendlyNameForUri(homeLongPressAction));
            }

            mHomeLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mHomeLongPressAction);
        }

        if (hasMenuKey) {
            String menuPressAction = Settings.System.getString(getContentResolver(),
                    Settings.System.KEY_MENU_ACTION);
            if (menuPressAction == null)
                menuPressAction = Integer.toString(ACTION_MENU);

            try {
                Integer.parseInt(menuPressAction);
                mMenuPressAction.setValue(menuPressAction);
                mMenuPressAction.setSummary(mMenuPressAction.getEntry());
            } catch (NumberFormatException e) {
                mMenuPressAction.setValue(Integer.toString(ACTION_CUSTOM_APP));
                mMenuPressAction.setSummary(mPicker.getFriendlyNameForUri(menuPressAction));
            }

            mMenuPressAction.setOnPreferenceChangeListener(this);

            String menuLongPressAction;
            menuLongPressAction = Settings.System.getString(getContentResolver(),
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION);
            if (hasAssistKey) {
                if (menuLongPressAction == null)
                    menuLongPressAction = Integer.toString(ACTION_NOTHING);
            } else {
                if (menuLongPressAction == null)
                    menuLongPressAction = Integer.toString(ACTION_SEARCH);
            }
            try {
                Integer.parseInt(menuLongPressAction);
                mMenuLongPressAction.setValue(menuLongPressAction);
                mMenuLongPressAction.setSummary(mMenuLongPressAction.getEntry());
            } catch (NumberFormatException e) {
                mMenuLongPressAction.setValue(Integer.toString(ACTION_CUSTOM_APP));
                mMenuLongPressAction.setSummary(mPicker.getFriendlyNameForUri(menuLongPressAction));
            }

            mMenuLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mMenuPressAction);
            bindingsCategory.removePreference(mMenuLongPressAction);
        }

        if (hasAssistKey) {
            String assistPressAction = Settings.System.getString(getContentResolver(),
                    Settings.System.KEY_ASSIST_ACTION);
            if (assistPressAction == null)
                assistPressAction = Integer.toString(ACTION_SEARCH);

            try {
                Integer.parseInt(assistPressAction);
                mAssistPressAction.setValue(assistPressAction);
                mAssistPressAction.setSummary(mAssistPressAction.getEntry());
            } catch (NumberFormatException e) {
                mAssistPressAction.setValue(Integer.toString(ACTION_CUSTOM_APP));
                mAssistPressAction.setSummary(mPicker.getFriendlyNameForUri(assistPressAction));
            }

            mAssistPressAction.setOnPreferenceChangeListener(this);

            String assistLongPressAction = Settings.System.getString(getContentResolver(),
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION);
            if (assistLongPressAction == null)
                assistLongPressAction = Integer.toString(ACTION_VOICE_SEARCH);

            try {
                Integer.parseInt(assistLongPressAction);
                mAssistLongPressAction.setValue(assistLongPressAction);
                mAssistLongPressAction.setSummary(mAssistLongPressAction.getEntry());
            } catch (NumberFormatException e) {
                mAssistLongPressAction.setValue(Integer.toString(ACTION_CUSTOM_APP));
                mAssistLongPressAction.setSummary(mPicker.getFriendlyNameForUri(assistLongPressAction));
            }

            mAssistLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mAssistPressAction);
            bindingsCategory.removePreference(mAssistLongPressAction);
        }

        if (hasAppSwitchKey) {
            String appSwitchPressAction = Settings.System.getString(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_ACTION);
            if (appSwitchPressAction == null)
                appSwitchPressAction = Integer.toString(ACTION_APP_SWITCH);

            try {
                Integer.parseInt(appSwitchPressAction);
                mAppSwitchPressAction.setValue(appSwitchPressAction);
                mAppSwitchPressAction.setSummary(mAppSwitchPressAction.getEntry());
            } catch (NumberFormatException e) {
                mAppSwitchPressAction.setValue(Integer.toString(ACTION_CUSTOM_APP));
                mAppSwitchPressAction.setSummary(mPicker.getFriendlyNameForUri(appSwitchPressAction));
            }

            mAppSwitchPressAction.setOnPreferenceChangeListener(this);

            String appSwitchLongPressAction = Settings.System.getString(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);
            if (appSwitchLongPressAction == null)
                appSwitchLongPressAction = Integer.toString(ACTION_NOTHING);

            try {
                Integer.parseInt(appSwitchLongPressAction);
                mAppSwitchLongPressAction.setValue(appSwitchLongPressAction);
                mAppSwitchLongPressAction.setSummary(mAppSwitchLongPressAction.getEntry());
            } catch (NumberFormatException e) {
                mAppSwitchLongPressAction.setValue(Integer.toString(ACTION_CUSTOM_APP));
                mAppSwitchLongPressAction.setSummary(mPicker.getFriendlyNameForUri(appSwitchLongPressAction));
            }

            mAppSwitchLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mAppSwitchPressAction);
            bindingsCategory.removePreference(mAppSwitchLongPressAction);
        }

        mEnableCustomBindings.setChecked((Settings.System.getInt(getActivity().
                getApplicationContext().getContentResolver(),
                Settings.System.HARDWARE_KEY_REBINDING, 0) == 1));
        mShowActionOverflow.setChecked((Settings.System.getInt(getActivity().
                getApplicationContext().getContentResolver(),
                Settings.System.UI_FORCE_OVERFLOW_BUTTON, 0) == 1));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.valueOf((String) newValue);
        if (value == ACTION_CUSTOM_APP) {
            mCustomAppPreference = preference;
            mPicker.pickShortcut();
            return true;
        } else {
            if (preference == mHomeLongPressAction) {
                int index = mHomeLongPressAction.findIndexOfValue((String) newValue);
                mHomeLongPressAction.setSummary(
                        mHomeLongPressAction.getEntries()[index]);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.KEY_HOME_LONG_PRESS_ACTION, value);
                return true;
            } else if (preference == mMenuPressAction) {
                int index = mMenuPressAction.findIndexOfValue((String) newValue);
                mMenuPressAction.setSummary(
                        mMenuPressAction.getEntries()[index]);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.KEY_MENU_ACTION, value);
                return true;
            } else if (preference == mMenuLongPressAction) {
                int index = mMenuLongPressAction.findIndexOfValue((String) newValue);
                mMenuLongPressAction.setSummary(
                        mMenuLongPressAction.getEntries()[index]);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION, value);
                return true;
            } else if (preference == mAssistPressAction) {
                int index = mAssistPressAction.findIndexOfValue((String) newValue);
                mAssistPressAction.setSummary(
                        mAssistPressAction.getEntries()[index]);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.KEY_ASSIST_ACTION, value);
                return true;
            } else if (preference == mAssistLongPressAction) {
                int index = mAssistLongPressAction.findIndexOfValue((String) newValue);
                mAssistLongPressAction.setSummary(
                        mAssistLongPressAction.getEntries()[index]);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, value);
                return true;
            } else if (preference == mAppSwitchPressAction) {
                int index = mAppSwitchPressAction.findIndexOfValue((String) newValue);
                mAppSwitchPressAction.setSummary(
                        mAppSwitchPressAction.getEntries()[index]);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.KEY_APP_SWITCH_ACTION, value);
                return true;
            } else if (preference == mAppSwitchLongPressAction) {
                int index = mAppSwitchLongPressAction.findIndexOfValue((String) newValue);
                mAppSwitchLongPressAction.setSummary(
                        mAppSwitchLongPressAction.getEntries()[index]);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, value);
                return true;
            }
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableCustomBindings) {
            Settings.System.putInt(getContentResolver(), Settings.System.HARDWARE_KEY_REBINDING,
                    mEnableCustomBindings.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mShowActionOverflow) {
            boolean enabled = mShowActionOverflow.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.UI_FORCE_OVERFLOW_BUTTON,
                    enabled ? 1 : 0);
            // Show appropriate
            if (enabled) {
                Toast.makeText(getActivity(), R.string.hardware_keys_show_overflow_toast_enable,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.hardware_keys_show_overflow_toast_disable,
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return false;
    }

    public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
        Preference preference = mCustomAppPreference;
        if (preference == mHomeLongPressAction) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION, uri);
        } else if (preference == mMenuPressAction) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.KEY_MENU_ACTION, uri);
        } else if (preference == mMenuLongPressAction) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION, uri);
        } else if (preference == mAssistPressAction) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.KEY_ASSIST_ACTION, uri);
        } else if (preference == mAssistLongPressAction) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION, uri);
        } else if (preference == mAppSwitchPressAction) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_ACTION, uri);
        } else if (preference == mAppSwitchLongPressAction) {
            Settings.System.putString(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, uri);
        }
        preference.setSummary(friendlyName);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
