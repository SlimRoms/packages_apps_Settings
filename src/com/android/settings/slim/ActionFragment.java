/*
 * Copyright (C) 2015 TeamEos project
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
 *
 * Handle assignable action dialogs and instances of the ActionPreference
 * class that holds target widget state
 */

package com.android.settings.slim;

import java.util.ArrayList;

import com.android.internal.util.actions.ActionHandler;
import com.android.internal.util.actions.ActionHandler.ActionBundle;
import com.android.internal.util.cm.QSUtils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.slim.util.ShortcutPickHelper;

public class ActionFragment extends SettingsPreferenceFragment implements
        ShortcutPickHelper.OnPickListener {

    private static final String ACTION_DEFAULT = "action_default";
    private static final String ACTION_APP = "action_app";
    private static final String ACTION_SYSTEM = "action_system";

    private ShortcutPickHelper mPicker;
    private ActionHolder mActionCategories;
    private ActionHolder mSystemActions;
    protected ArrayList<ActionPreference> mPrefHolder;
    private String mHolderKey;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPicker = new ShortcutPickHelper(getActivity(), this);
        mPrefHolder = new ArrayList<ActionPreference>();
        createActionCategoryList();
        createSystemActionList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPicker.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        // activity dialogs pass null here if they are dismissed
        // if null, do nothing, no harm
        if (uri == null) {
            return;
        }
        findAndUpdatePreference(uri, false);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof ActionPreference) {
            mHolderKey = preference.getKey();
            createAndShowCategoryDialog();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onStart() {
        super.onStart();
        for (ActionPreference pref : mPrefHolder) {
            pref.load();
        }
        onActionPolicyEnforced(mPrefHolder);
    }

    private void createActionCategoryList() {
        mActionCategories = new ActionHolder();
        mActionCategories.addAction(getString(R.string.action_entry_default_action), ACTION_DEFAULT);
        mActionCategories.addAction(getString(R.string.actions_system_action), ACTION_SYSTEM);
        mActionCategories.addAction(getString(R.string.select_application), ACTION_APP);
    }

    private void createSystemActionList() {
        mSystemActions = new ActionHolder();

        ArrayList<ActionBundle> actions = ActionHandler.getSystemActions(getActivity());
        for (ActionBundle b : actions) {
            mSystemActions.addAction(b.label, b.action);
        }

        // filter actions based on environment
        if (!usesExtendedActionsList()) {
            mSystemActions.removeAction(ActionHandler.SYSTEMUI_TASK_HOME);
            mSystemActions.removeAction(ActionHandler.SYSTEMUI_TASK_BACK);
        }

        if (!QSUtils.deviceSupportsMobileData(getActivity())) {
            mSystemActions.removeAction(ActionHandler.SYSTEMUI_TASK_WIFIAP);
        }

        if (!QSUtils.deviceSupportsBluetooth()) {
            mSystemActions.removeAction(ActionHandler.SYSTEMUI_TASK_BT);
        }

        if (!QSUtils.deviceSupportsFlashLight(getActivity())) {
            mSystemActions.removeAction(ActionHandler.SYSTEMUI_TASK_TORCH);
        }

        // only use for FFC only, i.e. Grouper
        // all other devices set action from packages
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            mSystemActions.removeAction(ActionHandler.SYSTEMUI_TASK_CAMERA);
        }
    }

    // subclass overrides to include back and home actions
    protected boolean usesExtendedActionsList() {
        return false;
    }

    protected void onActionPolicyEnforced(ArrayList<ActionPreference> prefs) {
    }

    // populate holder list once everything is added and removed
    protected void onPreferenceScreenLoaded() {
        final PreferenceScreen prefScreen = getPreferenceScreen();
        for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
            Preference pref = prefScreen.getPreference(i);
            if (pref instanceof PreferenceCategory) {
                PreferenceCategory cat = (PreferenceCategory) pref;
                for (int j = 0; j < cat.getPreferenceCount(); j++) {
                    Preference child = cat.getPreference(j);
                    if (child instanceof ActionPreference) {
                        mPrefHolder.add((ActionPreference) child);
                    }
                }
            } else if (pref instanceof ActionPreference) {
                mPrefHolder.add((ActionPreference) pref);
            }
        }
    }

    private void onTargetChange(String uri) {
        if (uri == null) {
            return;
        } else if (uri.equals(ACTION_DEFAULT)) {
            findAndUpdatePreference(ACTION_DEFAULT, true);
        } else if (uri.equals(ACTION_APP)) {
            mPicker.pickShortcut(null, null, getId());
        } else if (uri.equals(ACTION_SYSTEM)) {
            createAndShowSystemActionDialog();
        }
    }

    private void findAndUpdatePreference(String action, boolean setDefault) {
        for (ActionPreference pref : mPrefHolder) {
            if (pref.getKey().equals(mHolderKey)) {
                if (setDefault) {
                    action = pref.getDefaultAction();
                }
                ActionBundle b = new ActionBundle(getActivity(), action);
                pref.updateAction(b);
                onActionPolicyEnforced(mPrefHolder);
                break;
            }
        }
    }

    private void createAndShowCategoryDialog() {
        final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                onTargetChange(mActionCategories.getAction(item));
                dialog.dismiss();
            }
        };

        final DialogInterface.OnCancelListener cancel = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onTargetChange(null);
            }
        };

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.lockscreen_choose_action_title)
                .setItems(mActionCategories.getEntries(), l)
                .setOnCancelListener(cancel)
                .create();
        dialog.show();
    }

    private void createAndShowSystemActionDialog() {
        final DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                findAndUpdatePreference(mSystemActions.getAction(item), false);
                dialog.dismiss();
            }
        };

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Set action")
                .setItems(mSystemActions.getEntries(), l)
                .create();
        dialog.show();
    }

    private class ActionHolder {
        private ArrayList<CharSequence> mAvailableEntries = new ArrayList<CharSequence>();
        private ArrayList<String> mAvailableValues = new ArrayList<String>();

        public void addAction(String entry, String value) {
            mAvailableEntries.add(entry);
            mAvailableValues.add(value);
        }

        public void removeAction(String action) {
            int index = getActionIndex(action);
            if (index > -1) {
                mAvailableEntries.remove(index);
                mAvailableValues.remove(index);
            }
        }

        public int getActionIndex(String action) {
            int count = mAvailableValues.size();
            for (int i = 0; i < count; i++) {
                if (TextUtils.equals(mAvailableValues.get(i), action)) {
                    return i;
                }
            }
            return -1;
        }

        public String getAction(int index) {
            if (index > mAvailableValues.size()) {
                return null;
            }
            return mAvailableValues.get(index);
        }

        public CharSequence[] getEntries() {
            return mAvailableEntries.toArray(new CharSequence[mAvailableEntries.size()]);
        }
    }
}
