/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.WirelessSettings;
import com.android.settings.search.Index;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.WifiSettings;

import java.util.concurrent.atomic.AtomicBoolean;

public class MobileNetworksEnabler extends GenericSwitchToggle  {
    private TelephonyManager mTelephonyManager;
    private IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyManager.ACTION_PRECISE_DATA_CONNECTION_STATE_CHANGED.equals(action)) {
                updateState();
            }
        }
    };

    public MobileNetworksEnabler(Context context, SwitchBar switchBar) {
        super(context, switchBar);

        init();
        setupSwitches();
    }

    public MobileNetworksEnabler(Context context, Switch switch_) {
        super(context, switch_);

        init();
        setupSwitches();
    }

    private void init() {
        mTelephonyManager = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mIntentFilter = new IntentFilter(
                TelephonyManager.ACTION_PRECISE_DATA_CONNECTION_STATE_CHANGED);
    }

    private void setupSwitches() {
        updateState();
        if (mSwitchBar != null) {
            mSwitchBar.show();
        }
    }

    private void updateState() {
        setEnabled(mTelephonyManager.getDataState() != TelephonyManager.DATA_UNKNOWN);
        setChecked(mTelephonyManager.getDataEnabled());
    }

    @Override
    public void resume(Context context) {
        super.resume(context);
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void pause() {
        super.pause();
        mContext.unregisterReceiver(mReceiver);
    }

    private boolean isAirplaneModeOn() {
        return (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
    }

    public boolean isRadioAllowed(String type) {
        if (!isAirplaneModeOn()) {
            return true;
        }
        // Here we use the same logic in onCreate().
        String toggleable = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return toggleable != null && toggleable.contains(type);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }
        if (isChecked && !isRadioAllowed(Settings.Global.RADIO_CELL)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            setChecked(false);
            return;
        }

        mTelephonyManager.setDataEnabled(isChecked);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        super.onCheckedChanged(buttonView, isChecked);
    }
}
