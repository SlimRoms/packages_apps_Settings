/*
 * Copyright (C) 2013 Android Open Kang Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.slim.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.provider.Telephony.Sms.Intents;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import com.android.internal.util.slim.QuietHoursHelper;

import com.android.settings.R;

public class SmsCallService extends Service {

    private final static String TAG = "SmsCallService";

    private static TelephonyManager mTelephony;

    private boolean mIncomingCall = false;

    private boolean mKeepCounting = false;

    private String mIncomingNumber;

    private String mNumberSent;

    private int mMinuteSent;

    private int mBypassCallCount;

    private int mMinutes;

    private int mDay;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                mIncomingCall = true;
                mIncomingNumber = incomingNumber;
                final int bypassPreference = SmsCallController.getInstance(
                        SmsCallService.this).returnUserCallBypass();
                final boolean isContact = SmsCallController.getInstance(
                        SmsCallService.this).isContact(mIncomingNumber);
                boolean isStarred = false;

                if (isContact) {
                    isStarred = SmsCallController.getInstance(
                            SmsCallService.this).isStarred(mIncomingNumber);
                }

                if (!mKeepCounting) {
                    mKeepCounting = true;
                    mBypassCallCount = 0;
                    mDay = SmsCallController.getInstance(SmsCallService.this).returnDayOfMonth();
                    mMinutes = SmsCallController.getInstance(SmsCallService.this).returnTimeInMinutes();
                }

                boolean timeConstraintMet = SmsCallController.getInstance(
                        SmsCallService.this).returnTimeConstraintMet(mMinutes, mDay);
                if (timeConstraintMet) {
                    switch (bypassPreference) {
                        case SmsCallController.DEFAULT_DISABLED:
                            break;
                        case SmsCallController.ALL_NUMBERS:
                            mBypassCallCount++;
                            break;
                        case SmsCallController.CONTACTS_ONLY:
                            if (isContact) {
                                mBypassCallCount++;
                            }
                            break;
                        case SmsCallController.STARRED_ONLY:
                            if (isStarred) {
                                mBypassCallCount++;
                            }
                            break;
                    }

                    if (mBypassCallCount == 0) {
                        mKeepCounting = false;
                    }
                } else {
                    switch (bypassPreference) {
                        case SmsCallController.DEFAULT_DISABLED:
                            break;
                        case SmsCallController.ALL_NUMBERS:
                            mBypassCallCount = 1;
                            break;
                        case SmsCallController.CONTACTS_ONLY:
                            if (isContact) {
                                mBypassCallCount = 1;
                            } else {
                                // Reset call count and time at next call
                                mKeepCounting = false;
                            }
                            break;
                        case SmsCallController.STARRED_ONLY:
                            if (isStarred) {
                                mBypassCallCount = 1;
                            } else {
                                // Reset call count and time at next call
                                mKeepCounting = false;
                            }
                            break;
                    }
                    mDay = SmsCallController.getInstance(
                            SmsCallService.this).returnDayOfMonth();
                    mMinutes = SmsCallController.getInstance(
                            SmsCallService.this).returnTimeInMinutes();
                }
                if ((mBypassCallCount
                        == SmsCallController.getInstance(
                                SmsCallService.this).returnUserCallBypassCount())
                        && QuietHoursHelper.inQuietHours(SmsCallService.this, null)
                        && timeConstraintMet) {
                    // Don't auto-respond if alarm fired
                    mIncomingCall = false;
                    mKeepCounting = false;
                    startAlarm(mIncomingNumber);
                }
            }
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // Don't message or alarm if call was answered
                mIncomingCall = false;
                // Call answered, reset Incoming number
                // Stop AlarmSound
                mKeepCounting = false;
                Intent serviceIntent = new Intent(SmsCallService.this, AlarmService.class);
                SmsCallService.this.stopService(serviceIntent);
            }
            if (state == TelephonyManager.CALL_STATE_IDLE && mIncomingCall) {
                // Call Received and now inactive
                mIncomingCall = false;
                final int userAutoSms = SmsCallController.getInstance(
                        SmsCallService.this).returnUserAutoCall();

                if (userAutoSms != SmsCallController.DEFAULT_DISABLED
                        && QuietHoursHelper.inQuietHours(SmsCallService.this, null)) {
                    final boolean isContact =
                            SmsCallController.getInstance(
                                    SmsCallService.this).isContact(mIncomingNumber);
                    checkTimeAndNumber(mIncomingNumber, userAutoSms, isContact);
                }
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };

    private BroadcastReceiver mSmsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            SmsMessage msg = msgs[0];
            String incomingNumber = msg.getOriginatingAddress();
            boolean nawDawg = false;
            final int userAutoSms =
                    SmsCallController.getInstance(SmsCallService.this).returnUserAutoText();
            final int bypassCodePref =
                    SmsCallController.getInstance(SmsCallService.this).returnUserTextBypass();
            final boolean isContact =
                    SmsCallController.getInstance(
                            SmsCallService.this).isContact(incomingNumber);
            boolean isStarred = false;

            if (isContact) {
                isStarred = SmsCallController.getInstance(
                        SmsCallService.this).isStarred(incomingNumber);
            }

            if ((bypassCodePref != SmsCallController.DEFAULT_DISABLED
                   || userAutoSms != SmsCallController.DEFAULT_DISABLED)
                    && QuietHoursHelper.inQuietHours(SmsCallService.this, null)) {
                final String bypassCode =
                        SmsCallController.getInstance(
                                SmsCallService.this).returnUserTextBypassCode();
                final String messageBody = msg.getMessageBody();
                if (messageBody.contains(bypassCode)) {
                   switch (bypassCodePref) {
                       case SmsCallController.DEFAULT_DISABLED:
                           break;
                       case SmsCallController.ALL_NUMBERS:
                           // Sound Alarm && Don't auto-respond
                           nawDawg = true;
                           startAlarm(incomingNumber);
                           break;
                       case SmsCallController.CONTACTS_ONLY:
                           if (isContact) {
                               // Sound Alarm && Don't auto-respond
                               nawDawg = true;
                               startAlarm(incomingNumber);
                           }
                           break;
                       case SmsCallController.STARRED_ONLY:
                           if (isStarred) {
                               // Sound Alarm && Don't auto-respond
                               nawDawg = true;
                               startAlarm(incomingNumber);
                           }
                           break;
                    }
                }
                if (userAutoSms != SmsCallController.DEFAULT_DISABLED && nawDawg == false) {
                    checkTimeAndNumber(incomingNumber, userAutoSms, isContact);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        mTelephony = (TelephonyManager)
                this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.SMS_RECEIVED_ACTION);
        registerReceiver(mSmsReceiver, filter);
    }

    @Override
    public void onDestroy() {
        if (mTelephony != null) {
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        mPhoneStateListener = null;
        unregisterReceiver(mSmsReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * Dont send if alarm fired
     * If in same minute, don't send. This prevents message looping if sent to self
     * or another quiet-hours enabled device with this feature on.
     */
    private void checkTimeAndNumber(String incomingNumber,
            int userSetting, boolean isContact) {
        final int minutesNow = SmsCallController.getInstance(this).returnTimeInMinutes();
        if (minutesNow != mMinuteSent) {
            mNumberSent = incomingNumber;
            mMinuteSent = SmsCallController.getInstance(this).returnTimeInMinutes();
            SmsCallController.getInstance(this).checkSmsQualifiers(
                    incomingNumber, userSetting, isContact);
        } else {
            // Let's try to send if number doesn't match prior
            if (!incomingNumber.equals(mNumberSent)) {
                mNumberSent = incomingNumber;
                mMinuteSent = SmsCallController.getInstance(this).returnTimeInMinutes();
                SmsCallController.getInstance(this).checkSmsQualifiers(
                        incomingNumber, userSetting, isContact);
            }
        }
    }

    private void startAlarm(String phoneNumber) {
        String contactName = SmsCallController.getInstance(this).returnContactName(phoneNumber);
        Intent alarmDialog = new Intent();
        alarmDialog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        alarmDialog.setClass(this, com.android.settings.slim.service.BypassAlarm.class);
        alarmDialog.putExtra("number", contactName);
        startActivity(alarmDialog);
    }
}
