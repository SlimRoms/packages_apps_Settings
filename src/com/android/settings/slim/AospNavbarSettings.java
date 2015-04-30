/*
 * Copyright (C) 2014 TeamEos
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

import android.app.ActionBar;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.R;

public class AospNavbarSettings extends ActionFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String KEY_FORCE_SHOW_MENU = "softkey_persist_menu";

    private SwitchPreference mShowMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.aosp_navbar_settings);

        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(R.string.interface_navbar);
        }

        mShowMenu = (SwitchPreference) findPreference(KEY_FORCE_SHOW_MENU);
        if (mShowMenu != null) {
            mShowMenu.setChecked(Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_FORCE_SHOW_MENU,
                    0, UserHandle.USER_CURRENT) == 1);
            mShowMenu.setOnPreferenceChangeListener(this);
        }
        onPreferenceScreenLoaded();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mShowMenu)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            return Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_FORCE_SHOW_MENU, enabled ? 1 : 0,
                    UserHandle.USER_CURRENT);
        }
        return false;
    }
}
