/*
 * Copyright (C) 2013 Slimroms
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

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SeekBarPreference;

public class PieTriggerSettings extends SettingsPreferenceFragment
                        implements Preference.OnPreferenceChangeListener {

    private static final int DEFAULT_POSITION = 1 << 0; // this equals Position.LEFT.FLAG

    private static final String PREF_PIE_TRIGGER_SIZE = "pie_trigger_size";
    private static final String PREF_PIE_DISABLE_IME_TRIGGERS = "pie_disable_ime_triggers";
    private static final String[] TRIGGER = {
        "pie_control_trigger_left",
        "pie_control_trigger_bottom",
        "pie_control_trigger_right",
        "pie_control_trigger_top"
    };

    private float mMaxTriggerSize;
    private float mMinTriggerSize;

    private SeekBarPreference mPieTriggerSize;
    private CheckBoxPreference mDisableImeTriggers;
    private CheckBoxPreference[] mTrigger = new CheckBoxPreference[4];

    Resources mSystemUiResources;

    private ContentObserver mPieTriggerObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updatePieTriggers();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pie_trigger);

        PreferenceScreen prefSet = getPreferenceScreen();

        PackageManager pm = mContext.getPackageManager();

        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
                Log.e("PIETriggerSettings:", "can't access systemui resources",e);
            }
        }

        mPieTriggerSize = (SeekBarPreference) findPreference(PREF_PIE_TRIGGER_SIZE);
        mPieTriggerSize.setOnPreferenceChangeListener(this);

        mDisableImeTriggers = (CheckBoxPreference) findPreference(PREF_PIE_DISABLE_IME_TRIGGERS);
        mDisableImeTriggers.setOnPreferenceChangeListener(this);

        for (int i = 0; i < TRIGGER.length; i++) {
            mTrigger[i] = (CheckBoxPreference) prefSet.findPreference(TRIGGER[i]);
            mTrigger[i].setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPieTriggerSize) {
            float val = Float.parseFloat((String) newValue);
            float value = (val * ((mMaxTriggerSize - mMinTriggerSize)
                    / 100)) + mMinTriggerSize;
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_TRIGGER_SIZE,
                    value);
            return true;
        } else if (preference == mDisableImeTriggers) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_ADJUST_TRIGGER_FOR_IME,
                    (Boolean) newValue ? 1 : 0);
            updatePieTriggers();
        } else {
            int triggerSlots = 0;
            for (int i = 0; i < mTrigger.length; i++) {
                boolean checked = preference == mTrigger[i]
                        ? (Boolean) newValue : mTrigger[i].isChecked();
                if (checked) {
                    triggerSlots |= 1 << i;
                }
            }
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_GRAVITY, triggerSlots);
            updatePieTriggers();
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_TRIGGER_SHOW,
                1);

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.PIE_GRAVITY), true,
                mPieTriggerObserver);

        updatePieTriggers();
    }

    @Override
    public void onPause() {
        super.onPause();
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.PIE_TRIGGER_SHOW,
                0);
        getContentResolver().unregisterContentObserver(mPieTriggerObserver);
    }

    private void updatePieTriggers() {
        int triggerSlots = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_GRAVITY, DEFAULT_POSITION);

        for (int i = 0; i < mTrigger.length; i++) {
            if ((triggerSlots & (0x01 << i)) != 0) {
                mTrigger[i].setChecked(true);
            } else {
                mTrigger[i].setChecked(false);
            }
        }

        mDisableImeTriggers.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_ADJUST_TRIGGER_FOR_IME, 1) == 1);

        float triggerSize;
        try{
            triggerSize = Settings.System.getFloat(getActivity()
                    .getContentResolver(), Settings.System.PIE_TRIGGER_SIZE);
        } catch (Exception e) {
            triggerSize = mSystemUiResources.getDimension(
                    mSystemUiResources.getIdentifier("pie_trigger_height",
                    "dimen", "com.android.systemui"));
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_TRIGGER_SIZE, triggerSize);
        }
        mMaxTriggerSize = mSystemUiResources.getDimension(
                    mSystemUiResources.getIdentifier("pie_trigger_max_height",
                    "dimen", "com.android.systemui"));
        mMinTriggerSize = mSystemUiResources.getDimension(
                    mSystemUiResources.getIdentifier("pie_trigger_min_height",
                    "dimen", "com.android.systemui"));
        float triggerSizeValue = ((triggerSize - mMinTriggerSize) /
                ((mMaxTriggerSize - mMinTriggerSize) / 100)) / 100;
        mPieTriggerSize.setProperty(String.valueOf(triggerSizeValue));
        mPieTriggerSize.setInitValue((int) (triggerSizeValue * 100));
    }

}
