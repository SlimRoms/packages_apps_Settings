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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class NotificationDrawerSettings extends SettingsPreferenceFragment {

    public static final String TAG = "NotificationDrawerSettings";

    Preference mPowerWidget;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notification_drawer_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mPowerWidget = findPreference("power_widget");
        if (mPowerWidget != null) {
              updatePowerWidgetDescription();
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

    @Override
    public void onResume() {
        super.onResume();
        updatePowerWidgetDescription();
    }

}
