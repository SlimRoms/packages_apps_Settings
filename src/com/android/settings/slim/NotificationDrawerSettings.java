/*
 * Copyright (C) 2012 Slimroms
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

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

public class NotificationDrawerSettings extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener  {

    public static final String TAG = "NotificationDrawerSettings";
    private static final String PREF_NOTIFICATION_SHOW_WIFI_SSID = "notification_show_wifi_ssid";
    private static final String PREF_NOTIFICATION_OPTIONS = "options";
    private static final String PREF_NOTIFICATION_POWER_WIDGET = "power_widget";
    private static final String PREF_NOTIFICATION_QUICK_SETTINGS = "quick_settings_panel";
    private static final String PREF_NOTIFICATION_SETTINGS_BTN = "notification_settings_btn";
    private static final String KEY_NOTIFICATION_BEHAVIOUR = "notifications_behaviour";
    private static final String PREF_NOTIFICATION_HIDE_CARRIER = "notification_hide_carrier";

    PreferenceCategory mAdditionalOptions;
    Preference mPowerWidget;
    Preference mQuickSettings;
    CheckBoxPreference mShowWifiName;
    CheckBoxPreference mSettingsBtn;
    ListPreference mNotificationsBehavior;
    CheckBoxPreference mHideCarrier;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notification_drawer_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mPowerWidget = findPreference(PREF_NOTIFICATION_POWER_WIDGET);
        if (mPowerWidget != null) {
              updatePowerWidgetDescription();
        }

        mQuickSettings = findPreference(PREF_NOTIFICATION_QUICK_SETTINGS);
        if (mQuickSettings != null) {
              updateQuickSettingsDescription();
        }

        int CurrentBehavior = Settings.System.getInt(getContentResolver(), Settings.System.NOTIFICATIONS_BEHAVIOUR, 0);
        mNotificationsBehavior = (ListPreference) findPreference(KEY_NOTIFICATION_BEHAVIOUR);
        mNotificationsBehavior.setValue(String.valueOf(CurrentBehavior));
        mNotificationsBehavior.setSummary(mNotificationsBehavior.getEntry());
        mNotificationsBehavior.setOnPreferenceChangeListener(this);

        mShowWifiName = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SHOW_WIFI_SSID);
        mShowWifiName.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_SHOW_WIFI_SSID, 0) == 1);
        mShowWifiName.setOnPreferenceChangeListener(this);

        mHideCarrier = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_HIDE_CARRIER);
        boolean hideCarrier = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_HIDE_CARRIER, 0) == 1;
        mHideCarrier.setChecked(hideCarrier);
        mShowWifiName.setEnabled(!hideCarrier);
        mHideCarrier.setOnPreferenceChangeListener(this);

        mSettingsBtn = (CheckBoxPreference) findPreference(PREF_NOTIFICATION_SETTINGS_BTN);
        mSettingsBtn.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_SETTINGS_BUTTON, 0) == 1);
        mSettingsBtn.setOnPreferenceChangeListener(this);

        mAdditionalOptions = (PreferenceCategory) prefs.findPreference(PREF_NOTIFICATION_OPTIONS);

        PackageManager pm = getPackageManager();
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        if (!Utils.isPhone(getActivity()) || !isMobileData) {
            // Nothing for tablets, large screen devices and non mobile devices which doesn't show
            // information in notification drawer.....remove options
            mAdditionalOptions.removePreference(mHideCarrier);
            mAdditionalOptions.removePreference(mShowWifiName);
        }

    }

    private void updatePowerWidgetDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.EXPANDED_VIEW_WIDGET, 0) == 1) {
            mPowerWidget.setSummary(getString(R.string.power_widget_enabled));
        } else {
            mPowerWidget.setSummary(getString(R.string.power_widget_disabled));
         }
    }

    private void updateQuickSettingsDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QS_DISABLE_PANEL, 0) == 0) {
            mQuickSettings.setSummary(getString(R.string.quick_settings_enabled));
        } else {
            mQuickSettings.setSummary(getString(R.string.quick_settings_disabled));
         }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePowerWidgetDescription();
        updateQuickSettingsDescription();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNotificationsBehavior) {
            String val = (String) newValue;
                     Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATIONS_BEHAVIOUR,
            Integer.valueOf(val));
            int index = mNotificationsBehavior.findIndexOfValue(val);
            mNotificationsBehavior.setSummary(mNotificationsBehavior.getEntries()[index]);
            return true;
        } else if (preference == mShowWifiName) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NOTIFICATION_SHOW_WIFI_SSID,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mHideCarrier) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NOTIFICATION_HIDE_CARRIER,
                    (Boolean) newValue ? 1 : 0);
            mShowWifiName.setEnabled(!((Boolean) newValue));
            return true;
        } else if (preference == mSettingsBtn) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NOTIFICATION_SETTINGS_BUTTON,
                    (Boolean) newValue ? 1 : 0);
            return true;
        }
        return false;
    }

}
