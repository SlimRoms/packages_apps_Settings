/*
 * Copyright (C) 2013 The Dirty Unicorns Project
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
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;

public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "QuickSettings";

    private static final String QS_QUICK_ACCESS = "qs_quick_access";
    private static final String QS_QUICK_ACCESS_LINKED = "qs_quick_access_linked";

    private CheckBoxPreference mQSQuickAccess;
    private CheckBoxPreference mQSQuickAccess_linked;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.quick_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        ContentResolver resolver = getActivity().getContentResolver();

        mQSQuickAccess = (CheckBoxPreference) prefSet.findPreference(QS_QUICK_ACCESS);
        mQSQuickAccess.setChecked((Settings.System.getInt(resolver,
                Settings.System.QS_QUICK_ACCESS, 0) == 1));

        mQSQuickAccess_linked = (CheckBoxPreference) prefSet.findPreference(QS_QUICK_ACCESS_LINKED);
        mQSQuickAccess_linked.setChecked((Settings.System.getInt(resolver,
                Settings.System.QS_QUICK_ACCESS_LINKED, 0) == 1));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();

        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;

        if (preference == mQSQuickAccess) {
            value = mQSQuickAccess.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.QS_QUICK_ACCESS, value ? 1 : 0);
            Helpers.restartSystemUI();
        } else if (preference == mQSQuickAccess_linked) {
            value = mQSQuickAccess_linked.isChecked();
            Settings.System.putInt(resolver,
                    Settings.System.QS_QUICK_ACCESS_LINKED, value ? 1 : 0);
           Helpers.restartSystemUI();
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
}
