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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SeekBarPreference;

public class PieTriggerSettings extends SettingsPreferenceFragment
                        implements Preference.OnPreferenceChangeListener {

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DEFAULT_POSITION = 1 << 0; // this equals Position.LEFT.FLAG

    private static final String PREF_PIE_TRIGGER_THICKNESS = "pie_trigger_thickness";
    private static final String PREF_PIE_TRIGGER_HEIGHT = "pie_trigger_height";
    private static final String PREF_PIE_TRIGGER_GRAVITY_LET_RIGHT = "pie_trigger_gravity_left_right";
    private static final String PREF_PIE_DISABLE_IME_TRIGGERS = "pie_disable_ime_triggers";
    private static final String[] TRIGGER = {
        "pie_control_trigger_left",
        "pie_control_trigger_bottom",
        "pie_control_trigger_right",
        "pie_control_trigger_top"
    };

    private float mMaxTriggerThickness;
    private float mMinTriggerThickness;
    private final float PIE_TRIGGER_HEIGHT_MAX = 1.0f;
    private final float PIE_TRIGGER_HEIGHT_MIN = 0.2f;

    private SeekBarPreference mPieTriggerThickness;
    private SeekBarPreference mPieTriggerHeight;
    private ListPreference mPieTriggerGravityLeftRight;
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

        mPieTriggerThickness = (SeekBarPreference) findPreference(PREF_PIE_TRIGGER_THICKNESS);
        mPieTriggerThickness.setOnPreferenceChangeListener(this);
        mPieTriggerHeight = (SeekBarPreference) findPreference(PREF_PIE_TRIGGER_HEIGHT);
        mPieTriggerHeight.setOnPreferenceChangeListener(this);
        mPieTriggerGravityLeftRight = (ListPreference) prefSet.findPreference(PREF_PIE_TRIGGER_GRAVITY_LET_RIGHT);
        mPieTriggerGravityLeftRight.setOnPreferenceChangeListener(this);
        mDisableImeTriggers = (CheckBoxPreference) findPreference(PREF_PIE_DISABLE_IME_TRIGGERS);
        mDisableImeTriggers.setOnPreferenceChangeListener(this);

        for (int i = 0; i < TRIGGER.length; i++) {
            mTrigger[i] = (CheckBoxPreference) prefSet.findPreference(TRIGGER[i]);
            mTrigger[i].setOnPreferenceChangeListener(this);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.pie_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.pie_reset);
        alertDialog.setMessage(R.string.pie_trigger_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_TRIGGER_THICKNESS,
                    mSystemUiResources.getDimension(
                    mSystemUiResources.getIdentifier("pie_trigger_thickness",
                    "dimen", "com.android.systemui")));
                Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_TRIGGER_HEIGHT, 0.7f);

                Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_TRIGGER_GRAVITY_LEFT_RIGHT, 16);
                updatePieTriggers();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPieTriggerThickness) {
            float val = Float.parseFloat((String) newValue);
            float value = (val * ((mMaxTriggerThickness - mMinTriggerThickness)
                    / 100)) + mMinTriggerThickness;
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_TRIGGER_THICKNESS,
                    value);
            return true;
        } else if (preference == mPieTriggerHeight) {
            float val = Float.parseFloat((String) newValue);
            float value = (val * ((PIE_TRIGGER_HEIGHT_MAX - PIE_TRIGGER_HEIGHT_MIN)
                    / 100)) + PIE_TRIGGER_HEIGHT_MIN;
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.PIE_TRIGGER_HEIGHT,
                    value);
            return true;
        } else if (preference == mPieTriggerGravityLeftRight) {
            int index = mPieTriggerGravityLeftRight.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_TRIGGER_GRAVITY_LEFT_RIGHT,
                    value);
            mPieTriggerGravityLeftRight.setSummary(
                mPieTriggerGravityLeftRight.getEntries()[index]);
        } else if (preference == mDisableImeTriggers) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_ADJUST_TRIGGER_FOR_IME,
                    (Boolean) newValue ? 1 : 0);
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
        }
        updatePieTriggers();
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

        // default value is Gravity.CENTER_VERTICAL = 16
        // see AOSP docu
        mPieTriggerGravityLeftRight.setValue(String.valueOf(
                Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_TRIGGER_GRAVITY_LEFT_RIGHT, 16)));
        mPieTriggerGravityLeftRight.setSummary(
                mPieTriggerGravityLeftRight.getEntry());

        float triggerThickness;
        try{
            triggerThickness = Settings.System.getFloat(getActivity()
                    .getContentResolver(), Settings.System.PIE_TRIGGER_THICKNESS);
        } catch (Exception e) {
            triggerThickness = mSystemUiResources.getDimension(
                    mSystemUiResources.getIdentifier("pie_trigger_thickness",
                    "dimen", "com.android.systemui"));
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_TRIGGER_THICKNESS, triggerThickness);
        }
        mMaxTriggerThickness = mSystemUiResources.getDimension(
                    mSystemUiResources.getIdentifier("pie_trigger_max_thickness",
                    "dimen", "com.android.systemui"));
        mMinTriggerThickness = mSystemUiResources.getDimension(
                    mSystemUiResources.getIdentifier("pie_trigger_min_thickness",
                    "dimen", "com.android.systemui"));
        float triggerThicknessValue = ((triggerThickness - mMinTriggerThickness) /
                ((mMaxTriggerThickness - mMinTriggerThickness) / 100)) / 100;
        mPieTriggerThickness.setInitValue((int) (triggerThicknessValue * 100));
        mPieTriggerThickness.disablePercentageValue(true);

        float triggerHeight;
        try{
            triggerHeight = Settings.System.getFloat(getActivity()
                    .getContentResolver(), Settings.System.PIE_TRIGGER_HEIGHT);
        } catch (Exception e) {
            triggerHeight = 0.7f;
            Settings.System.putFloat(getActivity().getContentResolver(),
                Settings.System.PIE_TRIGGER_HEIGHT, triggerHeight);
        }
        float triggerHeightValue = ((triggerHeight - PIE_TRIGGER_HEIGHT_MIN) /
                ((PIE_TRIGGER_HEIGHT_MAX - PIE_TRIGGER_HEIGHT_MIN) / 100)) / 100;
        mPieTriggerHeight.setInitValue((int) (triggerHeightValue * 100));
        mPieTriggerHeight.disablePercentageValue(true);
    }

}
