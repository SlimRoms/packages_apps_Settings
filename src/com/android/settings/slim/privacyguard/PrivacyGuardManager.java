/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.slim.privacyguard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;

public class PrivacyGuardManager extends Fragment
        implements OnItemClickListener, OnItemLongClickListener {

    private static final String TAG = "PrivacyGuardManager";

    private boolean mShowSystemApps;
    private boolean mFilterAppPermissions;
    private boolean mFirstHelpWasShown;

    private TextView mNoUserAppsInstalled;
    private ListView mAppsList;
    private PrivacyGuardAppListAdapter mAdapter;
    private List<PrivacyGuardAppInfo> mApps;

    private PackageManager mPm;
    private Activity mActivity;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    // array off critical permissions where privacy guard
    // can hide the information
    private String mPermissionsFilter[] = new String[] {
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
            "android.permission.READ_HISTORY_BOOKMARKS",
            "android.permission.WRITE_HISTORY_BOOKMARKS",
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "com.android.voicemail.permission.READ_WRITE_ALL_VOICEMAIL",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.WRITE_SMS",
            "android.permission.BROADCAST_SMS"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mActivity = getActivity();
        mPm = mActivity.getPackageManager();

        return inflater.inflate(R.layout.privacy_guard_manager, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mNoUserAppsInstalled = (TextView) mActivity.findViewById(R.id.error);

        mAppsList = (ListView) mActivity.findViewById(R.id.appsList);
        mAppsList.setOnItemClickListener(this);
        mAppsList.setOnItemLongClickListener(this);

        mAdapter = new PrivacyGuardAppListAdapter(mActivity.getApplicationContext());

        // get shared preference
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity.getApplicationContext());
        mShowSystemApps = mPreferences.getBoolean("privacyGuardManagerShowSystemApps", false);
        mFilterAppPermissions = mPreferences.getBoolean("privacyGuardManagerFilterAppPermissions", true);
        mFirstHelpWasShown = mPreferences.getBoolean("privacyGuardManagerFirstHelpWasShown", false);

        if (!mFirstHelpWasShown) {
            showHelp();
        }

        // load preference fragment
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PrivacyGuardPrefs privacyGuardPrefs = new PrivacyGuardPrefs();
        fragmentTransaction.replace(R.id.privacyGuardPrefs, privacyGuardPrefs);
        fragmentTransaction.commit();

        // load apps and construct the list
        loadApps();
        setHasOptionsMenu(true);
    }

    private void loadApps() {
        mApps = loadInstalledApps();

        // if app list is empty inform the user
        // else go ahead and construct the list
        if (mApps == null
                || mApps.size() == 0) {
            if (mFilterAppPermissions) {
                mNoUserAppsInstalled.setText(R.string.privacy_guard_filter_does_not_match);
            } else {
                mNoUserAppsInstalled.setText(R.string.privacy_guard_no_user_apps);
            }
            mNoUserAppsInstalled.setVisibility(View.VISIBLE);
            mAppsList.setVisibility(View.GONE);
        } else {
            mNoUserAppsInstalled.setVisibility(View.GONE);
            mAppsList.setVisibility(View.VISIBLE);
            mAdapter.setListItems(mApps);
            mAppsList.setAdapter(mAdapter);

            new LoadIconsTask().execute(mApps.toArray(new PrivacyGuardAppInfo[]{}));
        }
    }

    private void resetPrivacyGuard() {
        if (mApps == null
                || mApps.size() == 0) {
            return;
        }
        // turn off privacy guard for all apps shown in the current list
        for (PrivacyGuardAppInfo app : mApps) {
            if (app.getPrivacyGuard()) {
                mPm.setPrivacyGuardSetting(app.getPackageName(), false);
                app.setPrivacyGuard(false);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // on click change the privace guard status for this item
        final PrivacyGuardAppInfo app = (PrivacyGuardAppInfo) parent.getItemAtPosition(position);

        final boolean privacyGuard = app.getPrivacyGuard();
        mPm.setPrivacyGuardSetting(app.getPackageName(), !privacyGuard);
        app.setPrivacyGuard(!privacyGuard);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // on longClick open detail app window
        final PrivacyGuardAppInfo app = (PrivacyGuardAppInfo) parent.getItemAtPosition(position);

        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                             Uri.parse("package:" + app.getPackageName())));
        } catch (ActivityNotFoundException err) {
        }

       return true;
    }

    /**
    * Uses the package manager to query for all currently installed apps
    * for the list.
    *
    * @return the complete List off installed applications (@code PrivacyGuardAppInfo)
    */
    private List<PrivacyGuardAppInfo> loadInstalledApps() {
        List<PrivacyGuardAppInfo> apps = new ArrayList<PrivacyGuardAppInfo>();

        // PackageManager.GET_META_DATA
        List<PackageInfo> packages = mPm.getInstalledPackages(PackageManager.GET_PERMISSIONS);

        for(int i=0; i < packages.size(); i++) {
            PackageInfo pInfo = packages.get(i);
            ApplicationInfo appInfo = pInfo.applicationInfo;
            // skip system apps if they shall not be included
            if ((!mShowSystemApps)
                    && ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1)) {
                continue;
            }

            if (mFilterAppPermissions) {
                boolean permissionMatched = false;

                // Get Permissions
                String[] requestedPermissions = pInfo.requestedPermissions;

                if (requestedPermissions != null) {
                    permissionMatched = filterAppPermissions(requestedPermissions);
                }
                if (!permissionMatched
                        || requestedPermissions == null) {
                    continue;
                }
            }

            PrivacyGuardAppInfo app = new PrivacyGuardAppInfo();
            app.setTitle(appInfo.loadLabel(mPm).toString());
            app.setPackageName(pInfo.packageName);
            app.setPrivacyGuard(mPm.getPrivacyGuardSetting(pInfo.packageName));
            apps.add(app);
        }

        // sort the apps by title
        Collections.sort(apps, new Comparator<PrivacyGuardAppInfo>() {
            @Override
            public int compare(PrivacyGuardAppInfo lhs, PrivacyGuardAppInfo rhs) {
                return (lhs.getTitle()).compareToIgnoreCase(rhs.getTitle());
            }
        });

        return apps;
    }

    private boolean filterAppPermissions(String[] requestedPermissions) {
        for (int j = 0; j < requestedPermissions.length; j++) {
            for (int z = 0; z < mPermissionsFilter.length; z++) {
               if (mPermissionsFilter[z].equals(requestedPermissions[j])) {
                    return true;
               }
            }
        }
        return false;
    }

    /**
    * An asynchronous task to load the icons of the installed applications.
    */
    private class LoadIconsTask extends AsyncTask<PrivacyGuardAppInfo, Void, Void> {
        @Override
        protected Void doInBackground(PrivacyGuardAppInfo... apps) {
            Map<String, Drawable> icons = new HashMap<String, Drawable>();

            for (PrivacyGuardAppInfo app : apps) {
                String pkgName = app.getPackageName();
                Drawable icon = null;
                // get the app icon
                try {
                    icon = mPm.getApplicationIcon(pkgName);
                } catch (NameNotFoundException e) {
                }
                icons.put(app.getPackageName(), icon);
            }
            mAdapter.setIcons(icons);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mAdapter.notifyDataSetChanged();
        }
   }

    private void showHelp() {
        /* Display the help dialog */
        AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
        alertDialog.setTitle(R.string.privacy_guard_help_title);
        alertDialog.setMessage(mActivity.getResources().getString(R.string.privacy_guard_help_text));
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                mActivity.getResources().getString(com.android.internal.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setFirstHelpWasShown();
                        return;
                    }
                });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                setFirstHelpWasShown();
            }
        });
        alertDialog.show();
    }

    private void setFirstHelpWasShown() {
        if (!mFirstHelpWasShown) {
            mFirstHelpWasShown = true;
            mEditor = mPreferences.edit();
            mEditor.putBoolean("privacyGuardManagerFirstHelpWasShown", true);
            mEditor.commit();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.privacy_guard_manager, menu);
        menu.findItem(R.id.filterAppPermissions).setChecked(mFilterAppPermissions);
        menu.findItem(R.id.showSystemApps).setChecked(mShowSystemApps);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                showHelp();
                return true;
            case R.id.reset:
                resetPrivacyGuard();
                return true;
            case R.id.filterAppPermissions:
                // set the menu checkbox and save it in
                // shared preference and rebuild the list
                item.setChecked(!item.isChecked());
                mFilterAppPermissions = item.isChecked();
                mEditor = mPreferences.edit();
                mEditor.putBoolean("privacyGuardManagerFilterAppPermissions", item.isChecked());
                mEditor.commit();
                loadApps();
                return true;
            case R.id.showSystemApps:
                // set the menu checkbox and save it in
                // shared preference and rebuild the list
                item.setChecked(!item.isChecked());
                mShowSystemApps = item.isChecked();
                mEditor = mPreferences.edit();
                mEditor.putBoolean("privacyGuardManagerShowSystemApps", item.isChecked());
                mEditor.commit();
                loadApps();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // rebuild the list due that the
        // user maybe changed something
        // on an app
        loadApps();
    }
}
