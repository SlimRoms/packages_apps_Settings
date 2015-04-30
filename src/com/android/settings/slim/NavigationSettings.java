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

import com.android.internal.util.actions.ActionUtils;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

public class NavigationSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, BaseSystemSettingSwitchBar.SwitchBarChangeCallback {

    private static final String KEY_NAVBAR_MODE = "systemui_navbar_mode";
    private static final String KEY_NAVMODE_SETTINGS = "navigation_mode_settings";
    private static final String KEY_CATEGORY_NAVIGATION_GENERAL = "category_navbar_general";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";

    private ListPreference mNavbarMode;
    private PreferenceScreen mSettingsTarget;
    private SwitchPreference mNavigationBarLeftPref;

    private BaseSystemSettingSwitchBar mEnabledSwitch;
    private boolean mLastEnabledState;
    private ViewGroup mPrefsContainer;
    private TextView mDisabledText;
    private boolean mHasHardwareKeys;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.navigation_settings);

        // only show switch bar to enable navigation bar on hardware key devices
        mHasHardwareKeys = ActionUtils.isCapKeyDevice(getActivity());

        mSettingsTarget = (PreferenceScreen) findPreference(KEY_NAVMODE_SETTINGS);

        mNavbarMode = (ListPreference) findPreference(KEY_NAVBAR_MODE);
        int val = Settings.System.getInt(getContentResolver(), Settings.System.NAVIGATION_BAR_MODE, 0);
        mNavbarMode.setDefaultValue(val);
        mNavbarMode.setValue(String.valueOf(val));
        mNavbarMode.setOnPreferenceChangeListener(this);
        updateSummaryFromValue(mNavbarMode, R.array.systemui_navbar_mode_entries,
                R.array.systemui_navbar_mode_values);
        updateSettingsTarget(val);

        // Navigation bar left
        mNavigationBarLeftPref = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);
        if (!ActionUtils.isNormalScreen()) {
            PreferenceCategory navbarGeneral = (PreferenceCategory) findPreference(KEY_CATEGORY_NAVIGATION_GENERAL);
            navbarGeneral.removePreference(mNavigationBarLeftPref);
            mNavigationBarLeftPref = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // build navigation settings disabled container for hardware key devices
        // all hardware key devices can force show the navigation bar
        // if hardware key disabler is supported, the disabler will be activated
        // while the navigation bar is showing. This container should never show
        // on devices without hardware keys        
        View v = inflater.inflate(R.layout.hideable_fragment, container, false);
        mPrefsContainer = (ViewGroup) v.findViewById(R.id.prefs_container);
        mDisabledText = (TextView) v.findViewById(R.id.disabled_text);

        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.force_navbar_disabled_notice));

//        CmHardwareManager cmHardwareManager =
//                (CmHardwareManager) getSystemService(Context.CMHW_SERVICE);
//        boolean hasKeyDisabler = cmHardwareManager
//                .isSupported(CmHardwareManager.FEATURE_KEY_DISABLE);
        boolean hasKeyDisabler = false; // TODO: restore if/when cmhw is supported
        builder.append(" ").append(getString(hasKeyDisabler
                ? R.string.force_navbar_key_disable_supported
                : R.string.force_navbar_key_disable_unsupported));
        mDisabledText.setText(builder.toString());

        View prefs = super.onCreateView(inflater, mPrefsContainer, savedInstanceState);
        mPrefsContainer.addView(prefs);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        if (mHasHardwareKeys) {
            mEnabledSwitch = new BaseSystemSettingSwitchBar(activity, activity.getSwitchBar(),
                    Settings.System.DEV_FORCE_SHOW_NAVBAR, false, this);
        } else {
            mEnabledSwitch = null;
            updateEnabledState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final SettingsActivity activity = (SettingsActivity) getActivity();
        if (mHasHardwareKeys && mEnabledSwitch != null) {
            mEnabledSwitch.resume(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mHasHardwareKeys && mEnabledSwitch != null) {
            mEnabledSwitch.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mHasHardwareKeys && mEnabledSwitch != null) {
            mEnabledSwitch.teardownSwitchBar();
        }
    }

    @Override
    public void onEnablerChanged(boolean isEnabled) {
        if (mHasHardwareKeys) {
            mLastEnabledState = isEnabled;
            updateEnabledState();
            //ButtonSettings.restoreKeyDisabler(getActivity());
        }
    }

    private void updateEnabledState() {
        // container always hidden on devices without hardware keys
        if (!mHasHardwareKeys) {
            mLastEnabledState = true;
        }
        mPrefsContainer.setVisibility(mLastEnabledState ? View.VISIBLE : View.GONE);
        mDisabledText.setVisibility(mLastEnabledState ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mNavbarMode)) {
            int val = Integer.parseInt(((String) newValue).toString());
            boolean ret = Settings.System.putInt(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_MODE, val);
            mNavbarMode.setValue(String.valueOf(val));
            updateSummaryFromValue(mNavbarMode, R.array.systemui_navbar_mode_entries,
                    R.array.systemui_navbar_mode_values);
            updateSettingsTarget(val);
            return ret;
        }
        return false;
    }

    private void updateSummaryFromValue(ListPreference pref, int entryRes, int valueRes) {
        String[] entries = getResources().getStringArray(entryRes);
        String[] vals = getResources().getStringArray(valueRes);
        String currentVal = pref.getValue();
        String newEntry = "";
        for (int i = 0; i < vals.length; i++) {
            if (vals[i].equals(currentVal)) {
                newEntry = entries[i];
                break;
            }
        }
        pref.setSummary(newEntry);
    }

    private void updateSettingsTarget(int val) {
        mSettingsTarget.setFragment(getResources().getStringArray(
                R.array.systemui_navbar_settings_fragments)[val]);
        mSettingsTarget.setTitle(getResources().getStringArray(
                R.array.systemui_navbar_mode_settings)[val]);
    }
}
