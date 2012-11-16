/*
 * Copyright (C) 2012 CyanogenMod
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

package com.android.settings.performance;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class PerformanceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "PerformanceSettings";

    private static final String DISABLE_BOOTANIMATION_PREF = "pref_disable_bootanimation";
            
    private static final String DISABLE_BOOTANIMATION_PERSIST_PROP = "persist.sys.nobootanimation";

    private static final String GENERIC_TWEAKS_CATEGORY = "generic_performance_tweaks";

    private static final String KEY_HIGH_END_GFX = "high_end_gfx";

    private static final String DISABLE_BOOTANIMATION_DEFAULT = "0";
            
    private CheckBoxPreference mDisableBootanimPref;

    private CheckBoxPreference mHighEndGfx;

    private final Configuration mCurConfig = new Configuration();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.performance_settings);

        mDisableBootanimPref = (CheckBoxPreference) getPreferenceScreen().findPreference(DISABLE_BOOTANIMATION_PREF);

        String disableBootanimation = SystemProperties.get(DISABLE_BOOTANIMATION_PERSIST_PROP,
                                                           DISABLE_BOOTANIMATION_DEFAULT);
        mDisableBootanimPref.setChecked("1".equals(disableBootanimation));

        boolean isHighEndGfx = ActivityManager.isHighEndGfx(getActivity().getWindowManager().getDefaultDisplay());
        PreferenceCategory mGenericTweaks = (PreferenceCategory) findPreference(GENERIC_TWEAKS_CATEGORY);

        mHighEndGfx = (CheckBoxPreference) mGenericTweaks.findPreference(KEY_HIGH_END_GFX);

        if (!isHighEndGfx) {
            // Only show this if the device does not have HighEndGfx enabled natively
            int highEndGfxDefault;
            try {
                highEndGfxDefault = Settings.System.getInt(getContentResolver(),Settings.System.HIGH_END_GFX_ENABLED);
                mHighEndGfx.setChecked(highEndGfxDefault == 1);
            }
            catch (Exception e) {
                highEndGfxDefault = mHighEndGfx.isChecked() ? 1 : 0;
                Settings.System.putInt(getContentResolver(),Settings.System.HIGH_END_GFX_ENABLED, highEndGfxDefault);
            }
        } else {
            mGenericTweaks.removePreference(mHighEndGfx);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDisableBootanimPref) {
            SystemProperties.set(DISABLE_BOOTANIMATION_PERSIST_PROP,
                                 mDisableBootanimPref.isChecked() ? "1" : "0");
        } else if (preference == mHighEndGfx) {
            Settings.System.putInt(getContentResolver(),Settings.System.HIGH_END_GFX_ENABLED, mHighEndGfx.isChecked() ? 1 : 0);
        
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        return true;
    }
}
