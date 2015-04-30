/*
 * Copyright (C) 2014 TeamEos project
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

import com.android.internal.util.actions.ActionHandler;
import com.android.internal.util.actions.ActionHandler.ActionBundle;

import android.content.Context;
import android.os.UserHandle;
import android.preference.Preference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;

public class ActionPreference extends Preference {
    private static final String SETTINGSNS = "http://schemas.android.com/apk/res/com.android.settings";
    private static final String ATTR_URI = "observedUri";
    private static final String ATTR_DEF_VAL = "defaultVal";
    private static final String EMPTY = "empty";

    private String mActionUri;
    private String mDefaultAction;
    private Context mContext;

    private ActionBundle mBundle;

    public ActionPreference(Context context) {
        this(context, null);
    }

    public ActionPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        mContext = context;

        int lpRes = attrs.getAttributeResourceValue(SETTINGSNS, ATTR_URI, -1);
        mActionUri = lpRes != -1 ? mContext.getResources().getString(lpRes) : EMPTY;

        int defVal = attrs.getAttributeResourceValue(SETTINGSNS, ATTR_DEF_VAL, -1);
        mDefaultAction = defVal != -1 ? mContext.getResources().getString(defVal)
                : ActionHandler.SYSTEMUI_TASK_NO_ACTION;
    }

    public String getAction() {
        return mBundle.action;
    }

    public String getDefaultAction() {
        return mDefaultAction;
    }

    public void load() {
        String action = Settings.System.getStringForUser(mContext.getContentResolver(), mActionUri,
                UserHandle.USER_CURRENT);
        if (action == null || TextUtils.isEmpty(action)) {
            action = mDefaultAction;
        }
        mBundle = new ActionBundle(mContext, action);
        setSummary(mBundle.label);
    }

    public void updateAction(ActionBundle bundle) {
        mBundle = bundle;
        Settings.System.putStringForUser(mContext.getContentResolver(),
                mActionUri, bundle.action, UserHandle.USER_CURRENT);
        setSummary(mBundle.label);
    }
}
