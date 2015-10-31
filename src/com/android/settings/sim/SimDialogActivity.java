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

package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.IExtTelephony;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimDialogActivity extends Activity {
    private static String TAG = "SimDialogActivity";

    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    public static final int INVALID_PICK = -1;
    public static final int DATA_PICK = 0;
    public static final int CALLS_PICK = 1;
    public static final int SMS_PICK = 2;
    public static final int PREFERRED_PICK = 3;

    private IExtTelephony mExtTelephony = IExtTelephony.Stub.
            asInterface(ServiceManager.getService("extphone"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle extras = getIntent().getExtras();
        final int dialogType = extras.getInt(DIALOG_TYPE_KEY, INVALID_PICK);

        switch (dialogType) {
            case DATA_PICK:
            case CALLS_PICK:
            case SMS_PICK:
                createDialog(this, dialogType).show();
                break;
            case PREFERRED_PICK:
                displayPreferredDialog(extras.getInt(PREFERRED_SIM));
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }

    }

    private void displayPreferredDialog(final int slotId) {
        final Resources res = getResources();
        final Context context = getApplicationContext();
        final SubscriptionInfo sir = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoForSimSlotIndex(slotId);

        if (sir != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.sim_preferred_title);
            alertDialogBuilder.setMessage(res.getString(
                        R.string.sim_preferred_message, sir.getDisplayName()));

            alertDialogBuilder.setPositiveButton(R.string.yes, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    final int subId = sir.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandle =
                            subscriptionIdToPhoneAccountHandle(subId);
                    setDefaultDataSubId(context, subId);
                    setDefaultSmsSubId(context, subId);
                    setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                    finish();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.no, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    finish();
                }
            });

            alertDialogBuilder.create().show();
        } else {
            finish();
        }
    }

    private static void setDefaultDataSubId(final Context context, final int subId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultDataSubId(subId);
        Toast.makeText(context, R.string.data_switch_started, Toast.LENGTH_LONG).show();
    }

    private static void setDefaultSmsSubId(final Context context, final int subId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultSmsSubId(subId);
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        final TelephonyManager telephonyManager = TelephonyManager.from(this);
        final Iterator<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == telephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }

        return null;
    }

    public Dialog createDialog(final Context context, final int id) {
        final ArrayList<String> list = new ArrayList<String>();
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final ArrayList<SubscriptionInfo> smsSubInfoList = new ArrayList<SubscriptionInfo>();
        final List<SubscriptionInfo> subInfoList =
            subscriptionManager.getActiveSubscriptionInfoList();
        final int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {

                        final SubscriptionInfo sir;
                        boolean ddsalertDisplayed = false;
                        switch (id) {
                            case DATA_PICK:
                                sir = subInfoList.get(value);
                                final int preferredSubID = sir.getSubscriptionId();
                                int defaultDataSubId = SubscriptionManager.getDefaultDataSubId();
                                if (defaultDataSubId != preferredSubID) {
                                    ddsalertDisplayed = displayDdsAlertIfNeeded(context,
                                           preferredSubID, defaultDataSubId);
                                    if (ddsalertDisplayed == false) {
                                        setDefaultDataSubId(context, preferredSubID);
                                    }
                                }
                                break;
                            case CALLS_PICK:
                                final TelecomManager telecomManager =
                                        TelecomManager.from(context);
                                final List<PhoneAccountHandle> phoneAccountsList =
                                        telecomManager.getCallCapablePhoneAccounts();
                                setUserSelectedOutgoingPhoneAccount(
                                        value < 1 ? null : phoneAccountsList.get(value - 1));
                                break;
                            case SMS_PICK:
                                boolean isSmsPrompt = false;
                                if (value < 1) {
                                    isSmsPrompt = true;
                                } else {
                                    sir = smsSubInfoList.get(value);
                                    if ( sir != null) {
                                        setDefaultSmsSubId(context, sir.getSubscriptionId());
                                    } else {
                                        isSmsPrompt = true;
                                    }
                                    Log.d(TAG, "SubscriptionInfo:" + sir);
                                }
                                Log.d(TAG, "isSmsPrompt: " + isSmsPrompt);
                                try {
                                    mExtTelephony.setSMSPromptEnabled(isSmsPrompt);
                                } catch (RemoteException ex) {
                                    Log.e(TAG, "RemoteException @setSMSPromptEnabled" + ex);
                                } catch (NullPointerException ex) {
                                    Log.e(TAG, "NullPointerException @setSMSPromptEnabled" + ex);
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid dialog type "
                                        + id + " in SIM dialog.");
                        }
                        if (id != DATA_PICK || ddsalertDisplayed == false) {
                            finish();
                        }
                    }
                };

        Dialog.OnKeyListener keyListener = new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        finish();
                    }
                    return true;
                }
            };

        ArrayList<SubscriptionInfo> callsSubInfoList = new ArrayList<SubscriptionInfo>();
        if (id == CALLS_PICK) {
            final TelecomManager telecomManager = TelecomManager.from(context);
            final TelephonyManager telephonyManager = TelephonyManager.from(context);
            final Iterator<PhoneAccountHandle> phoneAccounts =
                    telecomManager.getCallCapablePhoneAccounts().listIterator();

            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            callsSubInfoList.add(null);
            while (phoneAccounts.hasNext()) {
                final PhoneAccount phoneAccount =
                        telecomManager.getPhoneAccount(phoneAccounts.next());
                list.add((String)phoneAccount.getLabel());
                int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    final SubscriptionInfo sir = SubscriptionManager.from(context)
                            .getActiveSubscriptionInfo(subId);
                    callsSubInfoList.add(sir);
                } else {
                    callsSubInfoList.add(null);
                }
            }
        } else if ((id == SMS_PICK)){
            list.add(getResources().getString(R.string.sim_sms_ask_first_prefs_title));
            smsSubInfoList.add(null);
            for (int i = 0; i < selectableSubInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                smsSubInfoList.add(sir);
                CharSequence displayName = sir.getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        } else {
            for (int i = 0; i < selectableSubInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                CharSequence displayName = sir.getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        }

        String[] arr = list.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ListAdapter adapter = new SelectAccountListAdapter(
                id == CALLS_PICK ? callsSubInfoList :
                (id == SMS_PICK ? smsSubInfoList: subInfoList),
                builder.getContext(),
                R.layout.select_account_list_item,
                arr, id);

        switch (id) {
            case DATA_PICK:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case CALLS_PICK:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case SMS_PICK:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type "
                        + id + " in SIM dialog.");
        }

        Dialog dialog = builder.setAdapter(adapter, selectionListener).create();
        dialog.setOnKeyListener(keyListener);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });

        return dialog;

    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private int mResId;
        private int mDialogId;
        private final float OPACITY = 0.54f;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList,
                Context context, int resource, String[] arr, int dialogId) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
            mDialogId = dialogId;
            mSubInfoList = subInfoList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView;
            final ViewHolder holder;

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            final SubscriptionInfo sir = mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText(getItem(position));
                holder.summary.setText("");
                holder.icon.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_live_help));
                holder.icon.setAlpha(OPACITY);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(mContext));
            }
            return rowView;
        }

        private class ViewHolder {
            TextView title;
            TextView summary;
            ImageView icon;
        }
    }

    private static boolean isDdsSwitchAlertDialogSupported(Context context, int subId) {
        Resources res = getResourcesForSubId(context, subId);
        return res.getBoolean(R.bool.config_dds_switch_alert_dialog_supported);
    }

    /**
     * Returns the resources related to Subscription.
     * @param Context object
     * @param Subscription Id of Subscription who's resources are required
     * @return Resources of the Sub.
     * @hide
     */
    private static Resources getResourcesForSubId(Context context, int subId) {
        String operatorNumeric = TelephonyManager.getDefault().getIccOperatorNumericForData(subId);
        Configuration config = context.getResources().getConfiguration();
        Configuration newConfig = new Configuration();
        newConfig.setTo(config);

        if (!TextUtils.isEmpty(operatorNumeric)) {
            newConfig.mcc = Integer.parseInt(operatorNumeric.substring(0,3));
            newConfig.mnc = Integer.parseInt(operatorNumeric.substring(3));
        }
        Log.d(TAG, "getResourcesForSubId: " + subId +
                ", mccmnc = " + newConfig.mcc + newConfig.mnc);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        DisplayMetrics newMetrics = new DisplayMetrics();
        newMetrics.setTo(metrics);

        return new Resources(context.getResources().getAssets(), newMetrics, newConfig);
    }

    private boolean displayDdsAlertIfNeeded(
            final Context context, final int subId, final int defaultDataSubId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final TelephonyManager telephonymanager = TelephonyManager.from(context);
        Log.d(TAG, "Default Data SubId [" + defaultDataSubId + "]");

        if (isDdsSwitchAlertDialogSupported(context, defaultDataSubId) &&
               ((telephonymanager.getVoiceNetworkType(defaultDataSubId) ==
                       TelephonyManager.NETWORK_TYPE_LTE) ||
                (telephonymanager.getVoiceNetworkType(defaultDataSubId) ==
                       TelephonyManager.NETWORK_TYPE_LTE_CA) ||
                (telephonymanager.getDataNetworkType(defaultDataSubId) ==
                       TelephonyManager.NETWORK_TYPE_LTE) ||
                (telephonymanager.getDataNetworkType(defaultDataSubId) ==
                       TelephonyManager.NETWORK_TYPE_LTE_CA))) {
            Log.d(TAG, "DDS switch request from LTE sub");

            AlertDialog alertDlg = new AlertDialog.Builder(context).create();
            String title = context.getResources().getString(
                    R.string.data_switch_warning_title,
                    SubscriptionManager.getSlotId(subId) + 1);
            alertDlg.setTitle(title);
            String warningString = context.getResources().getString(
                    R.string.data_switch_warning_text);
            alertDlg.setMessage(warningString);
            alertDlg.setCancelable(false);

            String yes = context.getResources().getString(
                    R.string.yes);
            alertDlg.setButton(yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "Switch DDS to subId: " + subId );
                    subscriptionManager.setDefaultDataSubId(subId);
                    Toast.makeText(context, R.string.data_switch_started, Toast.LENGTH_LONG)
                            .show();
                    finish();
                }
            });

            String no = context.getResources().getString(
                    R.string.no);
            alertDlg.setButton2(no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "Cancelled switch DDS to subId: " + subId);
                    dialog.cancel();
                    finish();
                    return;
                }
            });

            alertDlg.show();
            return true;
        }
        return false;
    }
}
