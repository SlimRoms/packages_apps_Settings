/*
 * Copyright (C) 2015 TeamEos
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
import android.content.Context;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Switch;

import com.android.settings.widget.SwitchBar;

public class SecureSettingCurrentUserSwitchBar extends BaseSystemSettingSwitchBar {

    public SecureSettingCurrentUserSwitchBar(Context context, SwitchBar switchBar, String key,
            boolean defaultState, SwitchBarChangeCallback callback) {
        super(context, switchBar, key, defaultState, callback);
        mSettingsObserver = new SecureSettingsObserver(new Handler());
    }

    @Override
    protected void setSwitchState() {
        boolean enabled = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                mSettingKey, mDefaultState, UserHandle.USER_CURRENT) == 1;
        mStateMachineEvent = true;
        setSwitchBarChecked(enabled);
        mStateMachineEvent = false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        // Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }

        // Handle a switch change
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                mSettingKey, isChecked ? 1 : 0, UserHandle.USER_CURRENT);

        if (mCallback != null) {
            mCallback.onEnablerChanged(isChecked);
        }
    }

    class SecureSettingsObserver extends SettingsObserver {
        SecureSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        protected void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    mSettingKey), false, this);
            update();
        }
    }
}
