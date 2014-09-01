/*
 * Copyright (C) 2014 Slimroms
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

package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Panel showing storage usage on disk for known {@link StorageVolume} returned
 * by {@link StorageManager}. Calculates and displays usage of data types.
 */
public class AccountsSettings extends SettingsPreferenceFragment
        implements OnAccountsUpdateListener {

    private static final int MENU_ADD = 101;

    private AuthenticatorHelper mAuthenticatorHelper;

    private Context mContext;

     @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();

        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(mContext);
        mAuthenticatorHelper.onAccountsUpdated(mContext, null);

        AccountManager.get(mContext).addOnAccountsUpdatedListener(this, null, true);

        createPreferenceScreen();

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AccountManager.get(mContext).removeOnAccountsUpdatedListener(this);
    }

    private PreferenceScreen createPreferenceScreen() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null)
            root.removeAll();

        addPreferencesFromResource(R.xml.account_settings);

        root = getPreferenceScreen();

        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();

        for (String accountType : accountTypes) {
            CharSequence label = mAuthenticatorHelper.getLabelForType(mContext, accountType);
            if (label == null) {
                continue;
            }

            Account[] accounts = AccountManager.get(mContext).getAccountsByType(accountType);
            PreferenceCategory pc = new PreferenceCategory(mContext);
            pc.setTitle(label);
            root.addPreference(pc);

            for (Account a : accounts) {
                Preference accPref = new Preference(mContext);
                accPref.setTitle(a.name);
                accPref.setIcon(mAuthenticatorHelper.getDrawableForType(mContext, accountType));
                accPref.setFragment(AccountSyncSettings.class.getName());
                accPref.getExtras().putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accPref.getExtras().putParcelable(AccountSyncSettings.ACCOUNT_KEY, a);
                root.addPreference(accPref);
            }
        }
        return root;
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // TODO: watch for package upgrades to invalidate cache; see 7206643
        mAuthenticatorHelper.updateAuthDescriptions(mContext);
        mAuthenticatorHelper.onAccountsUpdated(mContext, accounts);
        createPreferenceScreen();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_ADD, 0, R.string.add_account_label)
                .setIcon(R.drawable.ic_menu_add_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
