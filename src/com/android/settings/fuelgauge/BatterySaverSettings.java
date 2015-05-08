/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGING;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.SettingPref;
import com.android.settings.widget.SwitchBar;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class BatterySaverSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, OnPreferenceChangeListener {
    private static final String TAG = "BatterySaverSettings";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String KEY_TURN_ON_AUTOMATICALLY = "turn_on_automatically";
    private static final long WAIT_FOR_SWITCH_ANIM = 500;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

    private static final String PREF_COLOR_PICKER = "battery_saver_color";

    private final Handler mHandler = new Handler();
    private final SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);
    private final Receiver mReceiver = new Receiver();

    private Context mContext;
    private boolean mCreated;
    private SettingPref mTriggerPref;
    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private ColorPickerPreference mColorPicker;
    private boolean mValidListener;
    private PowerManager mPowerManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            mSwitchBar.show();
            return;
        }
        mCreated = true;
        addPreferencesFromResource(R.xml.battery_saver_settings);

        mContext = getActivity();
        mSwitchBar = ((SettingsActivity) mContext).getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();

        mTriggerPref = new SettingPref(SettingPref.TYPE_GLOBAL, KEY_TURN_ON_AUTOMATICALLY,
                Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                0, /*default*/
                getResources().getIntArray(R.array.battery_saver_trigger_values)) {
            @Override
            protected String getCaption(Resources res, int value) {
                if (value > 0 && value < 100) {
                    return res.getString(R.string.battery_saver_turn_on_automatically_pct,
                                         Utils.formatPercentage(value));
                }
                return res.getString(R.string.battery_saver_turn_on_automatically_never);
            }
        };
        mTriggerPref.init(this);

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mColorPicker = (ColorPickerPreference) findPreference(PREF_COLOR_PICKER);
        mColorPicker.setOnPreferenceChangeListener(this);
        initColorPicker();

        setHasOptionsMenu(true);
    }

    private void initColorPicker() {
        int intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.BATTERY_SAVER_MODE_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                    com.android.internal.R.color.battery_saver_mode_color);
            mColorPicker.setSummary(getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mColorPicker.setSummary(hexColor);
        }
        mColorPicker.setNewPreviewColor(intColor);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mColorPicker) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BATTERY_SAVER_MODE_COLOR, intHex);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettingsObserver.setListening(true);
        mReceiver.setListening(true);
        if (!mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
            mValidListener = true;
        }
        updateSwitch();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.setListening(false);
        mReceiver.setListening(false);
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mValidListener = false;
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        mHandler.removeCallbacks(mStartMode);
        if (isChecked) {
            mHandler.postDelayed(mStartMode, WAIT_FOR_SWITCH_ANIM);
        } else {
            if (DEBUG) Log.d(TAG, "Stopping low power mode from settings");
            trySetPowerSaveMode(false);
        }
    }

    private void trySetPowerSaveMode(boolean mode) {
        if (!mPowerManager.setPowerSaveMode(mode)) {
            if (DEBUG) Log.d(TAG, "Setting mode failed, fallback to current value");
            mHandler.post(mUpdateSwitch);
        }
    }

    private void updateSwitch() {
        final boolean mode = mPowerManager.isPowerSaveMode();
        if (DEBUG) Log.d(TAG, "updateSwitch: isChecked=" + mSwitch.isChecked() + " mode=" + mode);
        if (mode == mSwitch.isChecked()) return;

        // set listener to null so that that code below doesn't trigger onCheckedChanged()
        if (mValidListener) {
            mSwitchBar.removeOnSwitchChangeListener(this);
        }
        mSwitch.setChecked(mode);
        if (mValidListener) {
            mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    private final Runnable mUpdateSwitch = new Runnable() {
        @Override
        public void run() {
            updateSwitch();
        }
    };

    private final Runnable mStartMode = new Runnable() {
        @Override
        public void run() {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.d(TAG, "Starting low power mode from settings");
                    trySetPowerSaveMode(true);
                }
            });
        }
    };

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        BatterySaverSettings getOwner() {
            return (BatterySaverSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.battery_saver_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                Settings.System.BATTERY_SAVER_MODE_COLOR, -2);
                            getOwner().initColorPicker();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) Log.d(TAG, "Received " + intent.getAction());
            mHandler.post(mUpdateSwitch);
        }

        public void setListening(boolean listening) {
            if (listening && !mRegistered) {
                mContext.registerReceiver(this, new IntentFilter(ACTION_POWER_SAVE_MODE_CHANGING));
                mRegistered = true;
            } else if (!listening && mRegistered) {
                mContext.unregisterReceiver(this);
                mRegistered = false;
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri LOW_POWER_MODE_TRIGGER_LEVEL_URI
                = Global.getUriFor(Global.LOW_POWER_MODE_TRIGGER_LEVEL);

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (LOW_POWER_MODE_TRIGGER_LEVEL_URI.equals(uri)) {
                mTriggerPref.update(mContext);
            }
        }

        public void setListening(boolean listening) {
            final ContentResolver cr = getContentResolver();
            if (listening) {
                cr.registerContentObserver(LOW_POWER_MODE_TRIGGER_LEVEL_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }
    }
}
