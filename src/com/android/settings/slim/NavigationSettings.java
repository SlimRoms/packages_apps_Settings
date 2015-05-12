/*
 * Copyright (C) 2013 SlimRoms
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
import android.os.Bundle;
import android.os.ServiceManager;
import android.provider.Settings;
import android.preference.Preference;
<<<<<<< HEAD
import android.preference.PreferenceScreen;
=======
>>>>>>> 498e430... Settings: Slims navbar and navring customizations
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NavigationSettings extends SettingsPreferenceFragment {

<<<<<<< HEAD
    private static final String KEY_HARDWARE_KEYS = "hardwarekeys_settings";
=======
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_PIE_SETTINGS = "pie_settings";
>>>>>>> 498e430... Settings: Slims navbar and navring customizations

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.slim_navigation_settings);
<<<<<<< HEAD

        // Hide Hardware Keys menu if device doesn't have any
        PreferenceScreen hardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        if (deviceKeys == 0 && hardwareKeys != null) {
            getPreferenceScreen().removePreference(hardwareKeys);
        }
=======
>>>>>>> 498e430... Settings: Slims navbar and navring customizations
    }

}
