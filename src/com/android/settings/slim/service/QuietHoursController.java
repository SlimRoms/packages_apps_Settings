/*
 * Copyright (C) 2014 SlimRoms project
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.telephony.SmsManager;

import java.util.Calendar;

import com.android.settings.R;

public class QuietHoursController {

    private final static String TAG = "QuietHoursController";

    private static final String KEY_AUTO_SMS = "auto_sms";
    private static final String KEY_AUTO_SMS_CALL = "auto_sms_call";
    private static final String KEY_AUTO_SMS_MESSAGE = "auto_sms_message";
    private static final String KEY_LOOP_BYPASS_RINGTONE = "loop_bypass_ringtone";
    private static final String KEY_BYPASS_RINGTONE = "bypass_ringtone";
    private static final String KEY_CALL_BYPASS = "call_bypass";
    private static final String KEY_SMS_BYPASS = "sms_bypass";
    private static final String KEY_REQUIRED_CALLS = "required_calls";
    private static final String KEY_SMS_BYPASS_CODE = "sms_bypass_code";
    private static final String SCHEDULE_SERVICE_COMMAND =
            "com.android.settings.slim.service.SCHEDULE_SERVICE_COMMAND";

    private static final int FULL_DAY = 1440; // 1440 minutes in a day
    private static final int TIME_LIMIT = 30; // 30 minute bypass limit
    private static final int ALARM = 13378;
    private static final int SUNDAY = 0;
    private static final int MONDAY = 1;
    private static final int TUESDAY = 2;
    private static final int WEDNESDAY = 3;
    private static final int THURSDAY = 4;
    private static final int FRIDAY = 5;
    private static final int SATURDAY = 6;

    public static final int DEFAULT_DISABLED = 0;
    public static final int ALL_NUMBERS = 1;
    public static final int CONTACTS_ONLY = 2;
    public static final int STARRED_ONLY = 3;
    public static final int DEFAULT_TWO = 2;

    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private OnSharedPreferenceChangeListener mSharedPrefsObserver;
    private AlarmManager mAlarmManager;

    private Intent mServiceTriggerIntent;

    private boolean mDaily;

    private int mQuietHoursMode;
    private int[] mQuietHoursStart = new int[14];
    private int[] mQuietHoursEnd = new int[14];

    private int mSmsBypass;
    private int mCallBypass;
    private int mAutoCall;
    private int mAutoText;

    private Handler mHandler = new Handler();

    /**
     * Singleton.
     */
    private static QuietHoursController sInstance;

    /**
     * Get the instance.
     */
    public static QuietHoursController getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        } else {
            return sInstance = new QuietHoursController(context);
        }
    }

    /**
     * Constructor.
     */
    private QuietHoursController(Context context) {
        mContext = context;

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mSharedPrefsObserver =
                new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals(KEY_SMS_BYPASS)
                        || key.equals(KEY_CALL_BYPASS)
                        || key.equals(KEY_AUTO_SMS_CALL)
                        || key.equals(KEY_AUTO_SMS)) {
                    updateSharedPreferences();
                    checkModes();
                }
            }
        };
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mSharedPrefsObserver);

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        mServiceTriggerIntent = new Intent(mContext, SmsCallService.class);
        updateSharedPreferences();
        updateTimePrefs();
        checkModes();
    }

    private void updateSharedPreferences() {
        mSmsBypass = Integer.parseInt(mSharedPrefs.getString(
                KEY_SMS_BYPASS, String.valueOf(DEFAULT_DISABLED)));
        mCallBypass = Integer.parseInt(mSharedPrefs.getString(
                KEY_CALL_BYPASS, String.valueOf(DEFAULT_DISABLED)));
        mAutoCall = Integer.parseInt(mSharedPrefs.getString(
                KEY_AUTO_SMS_CALL, String.valueOf(DEFAULT_DISABLED)));
        mAutoText = Integer.parseInt(mSharedPrefs.getString(
                KEY_AUTO_SMS, String.valueOf(DEFAULT_DISABLED)));
    }

    // Called when time-range is changed and at init
    public void updateTimePrefs() {
        ContentResolver resolver = mContext.getContentResolver();
        mQuietHoursMode = Settings.System.getIntForUser(resolver,
                Settings.System.QUIET_HOURS_ENABLED, 0,
                UserHandle.USER_CURRENT_OR_SELF);

        mDaily = Settings.System.getIntForUser(resolver,
                Settings.System.QUIET_HOURS_DAILY, 0,
                UserHandle.USER_CURRENT_OR_SELF) == 1;

        if (mDaily) {
            for (int i = 0; i < 14; i++) {
                    mQuietHoursStart[i] = Settings.System.getIntForUser(
                        resolver,
                        Settings.System.QUIET_HOURS_START_TIMES[i], 720,
                        UserHandle.USER_CURRENT_OR_SELF);
                    mQuietHoursEnd[i] = Settings.System.getIntForUser(
                        resolver,
                        Settings.System.QUIET_HOURS_END_TIMES[i], 720,
                        UserHandle.USER_CURRENT_OR_SELF);
            }
        } else {
            final int startTimesSingle = Settings.System.getIntForUser(
                    resolver,
                    Settings.System.QUIET_HOURS_START, 720,
                    UserHandle.USER_CURRENT_OR_SELF);
            final int endTimesSingle = Settings.System.getIntForUser(
                    resolver,
                    Settings.System.QUIET_HOURS_END, 720,
                    UserHandle.USER_CURRENT_OR_SELF);
            for (int i = 0; i < 7; i++) {
                mQuietHoursStart[i] = startTimesSingle;
                mQuietHoursEnd[i] = endTimesSingle;
            }
        }
    }

    // Return the current time
    protected int returnTimeInMinutes() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }

    // Return current day of month
    protected int returnDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    // Return if last call versus current call less than 30 minute apart
    protected boolean returnTimeConstraintMet(int firstCallTime, int dayOfFirstCall) {
        final int currentMinutes = returnTimeInMinutes();
        final int dayOfMonth = returnDayOfMonth();
        // New Day, start at zero
        if (dayOfMonth != dayOfFirstCall) {
            // Less or Equal to 30 minutes until midnight
            if (firstCallTime >= (FULL_DAY - TIME_LIMIT)) {
                if ((currentMinutes >= 0) && (currentMinutes <= TIME_LIMIT)) {
                    int remainderDayOne = FULL_DAY - firstCallTime;
                    if ((remainderDayOne + currentMinutes) <= TIME_LIMIT) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                // new day and prior call happened with more than
                // 30 minutes remaining in day
                return false;
            }
        } else {
            // Same day - simple subtraction: or you need to get out more
            // and it's been a month since your last call, reboot, or reschedule
            if ((currentMinutes - firstCallTime) <= TIME_LIMIT) {
                return true;
            } else {
                return false;
            }
        }
    }

    /* True: Ringtone loops until alert dismissed
     * False: Ringtone plays only once
     */
    protected boolean returnUserRingtoneLoop() {
        return mSharedPrefs.getBoolean(KEY_LOOP_BYPASS_RINGTONE, true);
    }

    /* Returns user-selected alert Ringtone
     * Parsed from saved string or default ringtone
     */
    public Uri returnUserRingtone() {
        String ringtoneString = mSharedPrefs.getString(KEY_BYPASS_RINGTONE, null);
        if (ringtoneString == null) {
            // Value not set, defaults to Default Ringtone
            Uri alertSoundUri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_RINGTONE);
            return alertSoundUri;
        } else {
            Uri ringtoneUri = Uri.parse(ringtoneString);
            return ringtoneUri;
        }
    }

    // Code sender can deliver to start an alert
    protected String returnUserTextBypassCode() {
        String defaultCode = mContext.getResources().getString(
                R.string.quiet_hours_sms_code_null);
        return mSharedPrefs.getString(KEY_SMS_BYPASS_CODE, defaultCode);
    }

    // Number of missed calls within time constraint to start alert
    protected int returnUserCallBypassCount() {
        return Integer.parseInt(mSharedPrefs.getString(
                KEY_REQUIRED_CALLS, String.valueOf(DEFAULT_TWO)));
    }

    /* True: Quiet hours are active
     * False: Inactive
     */
    protected boolean quietHoursActive() {
        mQuietHoursMode = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0,
                UserHandle.USER_CURRENT_OR_SELF);
        return (mQuietHoursMode == 2 || mQuietHoursMode == 3);
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserTextBypass() {
        return mSmsBypass;
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserCallBypass() {
        return mCallBypass;
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserAutoCall() {
        return mAutoCall;
    }

    /* Default: Off
     * All Numbers
     * Contacts Only
     */
    protected int returnUserAutoText() {
        return mAutoText;
    }

    // Pull current settings and send message if applicable
    protected void checkSmsQualifiers(String incomingNumber,
            int userAutoSms, boolean isContact) {
        String message = null;
        String defaultSms = mContext.getResources().getString(
                R.string.quiet_hours_auto_sms_null);
        message = mSharedPrefs.getString(KEY_AUTO_SMS_MESSAGE, defaultSms);
        switch (userAutoSms) {
            case ALL_NUMBERS:
                sendAutoReply(message, incomingNumber);
                break;
            case CONTACTS_ONLY:
                if (isContact) {
                    sendAutoReply(message, incomingNumber);
                }
                break;
            case STARRED_ONLY:
                if (isContact && isStarred(incomingNumber)) {
                    sendAutoReply(message, incomingNumber);
                }
                break;
        }
    }

    /* True: Contact
     * False: Not a contact
     */
    protected boolean isContact(String phoneNumber) {
        boolean isContact = false;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = {
                PhoneLookup._ID,
                PhoneLookup.NUMBER,
                PhoneLookup.DISPLAY_NAME };
        Cursor c = mContext.getContentResolver().query(
                lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                isContact = true;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return isContact;
    }

    /* True: Starred contact
     * False: Not starred
     */
    protected boolean isStarred(String phoneNumber) {
        boolean isStarred = false;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = { PhoneLookup.STARRED };
        Cursor c = mContext.getContentResolver().query(
                lookupUri, numberProject, null, null, null);
        try {
            if (c.moveToFirst()) {
                if (c.getInt(c.getColumnIndex(PhoneLookup.STARRED)) == 1) {
                    isStarred = true;
                }
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }
        return isStarred;
    }

    // Returns the contact name or number
    protected String returnContactName(String phoneNumber) {
        String contactName = null;
        Uri lookupUri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] numberProject = { PhoneLookup.DISPLAY_NAME };
        Cursor c = mContext.getContentResolver().query(
            lookupUri, numberProject, null, null, null);

        try {
            if (c.moveToFirst()) {
                contactName = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            } else {
                // Not in contacts, return number again
                contactName = phoneNumber;
            }
        } finally {
            if (c != null) {
               c.close();
            }
        }

        return contactName;
    }

    // Send the message
    protected void sendAutoReply(String message, String phoneNumber) {
        SmsManager sms = SmsManager.getDefault();
        try {
            sms.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (IllegalArgumentException e) {
        }
    }

    /*
     * Called when:
     * QuietHours Toggled
     * QuietHours "requirement" mode(s) changed
     * I.E. if WiFi connection state changes or
     * if device is/isn't plugged in to charge
     */
    public void checkModes() {
        mQuietHoursMode = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0,
                UserHandle.USER_CURRENT_OR_SELF);
        if (mQuietHoursMode == 0) {
            toggleQuietHoursEntries(false);
        } else if (mQuietHoursMode == 1) {
            checkQuietTimes();
        } else if (mQuietHoursMode == 2) {
            toggleQuietHoursEntries(true);
        } else if (mQuietHoursMode == 3) {
            checkRequirements();
        } else if (mQuietHoursMode == 4) {
            // We're in the "waiting for requirements" mode so
            // timing is already true and can be ignored
            checkRequirements();
        }
    }

    /* Called when:
     * Quiet hours is set to a "time" mode that needs
     * checking before disabling/enabling preferences
     */
    private void checkQuietTimes() {
        // Get the date in "quiet hours" format.
        Calendar calendar = Calendar.getInstance();
        int currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60
                + calendar.get(Calendar.MINUTE);
        final int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        boolean inQuietHours = false;
        int lastIndex = 0;
        int dayIndex = 0;
        switch (day) {
            case SUNDAY:
                lastIndex = SATURDAY;
                dayIndex = SUNDAY;
                break;
            case MONDAY:
                lastIndex = SUNDAY;
                dayIndex = MONDAY;
                break;
            case TUESDAY:
                lastIndex = MONDAY;
                dayIndex = TUESDAY;
                break;
            case WEDNESDAY:
                lastIndex = TUESDAY;
                dayIndex = WEDNESDAY;
                break;
            case THURSDAY:
                lastIndex = WEDNESDAY;
                dayIndex = THURSDAY;
                break;
            case FRIDAY:
                lastIndex = THURSDAY;
                dayIndex = FRIDAY;
                break;
            case SATURDAY:
                lastIndex = FRIDAY;
                dayIndex = SATURDAY;
                break;
        }

        if (mQuietHoursEnd[lastIndex] < mQuietHoursStart[lastIndex]
                && currentMinutes <= mQuietHoursEnd[lastIndex]) {
            // We're in "today" but the last quiet hours are in affect
            inQuietHours = true;
        } else if (mQuietHoursEnd[dayIndex] < mQuietHoursStart[dayIndex]
                && currentMinutes >= mQuietHoursStart[dayIndex]) {
            // Starts before now today and ends tomorrow
            inQuietHours = true;
        } else if (mQuietHoursEnd[dayIndex] > mQuietHoursStart[dayIndex]
                && currentMinutes <= mQuietHoursEnd[dayIndex]
                && currentMinutes >= mQuietHoursStart[dayIndex]) {
            // Starts and ends today and we're in between those two times
            inQuietHours = true;
        }

        if (inQuietHours) {
            // Check if enabled requirements are true before enabling
            checkRequirements();
        } else {
            if (mDaily && mQuietHoursEnd[dayIndex] != mQuietHoursStart[dayIndex]) {
                lastIndex = lastIndex + 7;
                dayIndex = dayIndex + 7;
                if (mQuietHoursEnd[lastIndex] < mQuietHoursStart[lastIndex]
                        && currentMinutes <= mQuietHoursEnd[lastIndex]) {
                    // We're in "today" but the last quiet hours are in affect
                    inQuietHours = true;
                } else if (mQuietHoursEnd[dayIndex] < mQuietHoursStart[dayIndex]
                        && currentMinutes >= mQuietHoursStart[dayIndex]) {
                    // Starts before now today and ends tomorrow
                    inQuietHours = true;
                } else if (mQuietHoursEnd[dayIndex] > mQuietHoursStart[dayIndex]
                        && currentMinutes <= mQuietHoursEnd[dayIndex]
                        && currentMinutes >= mQuietHoursStart[dayIndex]) {
                    // Starts and ends today and we're in between those two times
                    inQuietHours = true;
                }
            }
        }

        if (inQuietHours) {
            checkRequirements();
        } else {
            toggleQuietHoursEntries(false);
        }
    }

    private void checkRequirements() {
        final int requireWifi = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_REQUIRE_WIFI, 0,
                UserHandle.USER_CURRENT_OR_SELF);
        final int requireCharging = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_REQUIRE_CHARGING, 0,
                UserHandle.USER_CURRENT_OR_SELF);
        if (requireCharging != 1 && requireWifi != 1) {
            // 0 means ignored and 1 means enabled but not met so must equal 2
            mQuietHoursMode = 3;
            toggleQuietHoursEntries(true);
        } else {
            // a requirement is enabled but not met
            mQuietHoursMode = 4;
            toggleQuietHoursEntries(false);
        }
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED,
                mQuietHoursMode, UserHandle.USER_CURRENT_OR_SELF);
    }

    private PendingIntent getScheduler() {
        Intent intent = new Intent();
        intent.setAction(SCHEDULE_SERVICE_COMMAND);
        PendingIntent scheduled = PendingIntent.getBroadcast(
                mContext, ALARM, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return scheduled;
    }

    /*
     * Called when:
     * QuietHours TimeChanged
     * AutoSMS Preferences Changed
     * At Boot
     * Time manually adjusted or Timezone Changed
     * Schedule needs updating
     */
    public void scheduleService() {
        mQuietHoursMode = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0,
                UserHandle.USER_CURRENT_OR_SELF);

        mAlarmManager.cancel(getScheduler());

        // Allows the scheduler to create the next needed
        // time of update without modifying current modes
        boolean ignoreChanges = false;

        if (mQuietHoursMode == 0) {
            // We're completely disabled
            toggleQuietHoursEntries(false);
            ignoreChanges = true;
        } else if (mQuietHoursMode == 2) {
            // We're skipping timing and forcing quiet hours on
            toggleQuietHoursEntries(true);
            ignoreChanges = true;
        }

        Calendar calendar = Calendar.getInstance();
        // Get the day of week - 1 so we can parse our int array fields
        final int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int lastIndex = 0;
        int dayIndex = 0;
        int nextIndex = 0;
        switch (day) {
            case SUNDAY:
                lastIndex = SATURDAY;
                dayIndex = SUNDAY;
                nextIndex = MONDAY;
                break;
            case MONDAY:
                lastIndex = SUNDAY;
                dayIndex = MONDAY;
                nextIndex = TUESDAY;
                break;
            case TUESDAY:
                lastIndex = MONDAY;
                dayIndex = TUESDAY;
                nextIndex = WEDNESDAY;
                break;
            case WEDNESDAY:
                lastIndex = TUESDAY;
                dayIndex = WEDNESDAY;
                nextIndex = THURSDAY;
                break;
            case THURSDAY:
                lastIndex = WEDNESDAY;
                dayIndex = THURSDAY;
                nextIndex = FRIDAY;
                break;
            case FRIDAY:
                lastIndex = THURSDAY;
                dayIndex = FRIDAY;
                nextIndex = SATURDAY;
                break;
            case SATURDAY:
                lastIndex = FRIDAY;
                dayIndex = SATURDAY;
                nextIndex = SUNDAY;
                break;
        }

        final boolean normalDaysInvalid =
                mQuietHoursStart[lastIndex] == mQuietHoursEnd[lastIndex]
                && mQuietHoursStart[dayIndex] == mQuietHoursEnd[dayIndex]
                && mQuietHoursStart[nextIndex] == mQuietHoursEnd[nextIndex];

        final boolean extraDaysInvalid = !mDaily
                && mQuietHoursStart[lastIndex + 7] == mQuietHoursEnd[lastIndex + 7]
                && mQuietHoursStart[dayIndex + 7] == mQuietHoursEnd[dayIndex + 7]
                && mQuietHoursStart[nextIndex + 7] == mQuietHoursEnd[nextIndex + 7];

        // if we are or are not currently in quiet hours
        boolean inQuietHours = false;

        // If we are scheduling for a start time or end time
        boolean startTime = false;

        // time from now on (in minutes) when the method should be rescheduled
        int nextSchedule = -1;

        // Current time in minutes
        final int currentMinutes =
                calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        if (!normalDaysInvalid) {
            if (mQuietHoursEnd[lastIndex] < mQuietHoursStart[lastIndex]) {
                // End time of yesterday is somtime today
                if (currentMinutes <= mQuietHoursEnd[lastIndex]) {
                    // We're before yesterday's end time
                    inQuietHours = true;
                    nextSchedule = mQuietHoursEnd[lastIndex] - currentMinutes + 1;
                }
            }

            // We already scheduled for yesterday's end time
            if (nextSchedule == -1) {
                if (mQuietHoursEnd[dayIndex] == mQuietHoursStart[dayIndex]) {
                    // Disabled for today, schedule for tomorrow's start
                    inQuietHours = false;
                    nextSchedule = FULL_DAY - currentMinutes + mQuietHoursStart[nextIndex];
                } else if (mQuietHoursEnd[dayIndex] < mQuietHoursStart[dayIndex]) {
                    // End time for today is sometime tomorrow
                    if (currentMinutes >= mQuietHoursStart[dayIndex]) {
                        // We're after today's start time
                        inQuietHours = true;
                        nextSchedule = FULL_DAY - currentMinutes + mQuietHoursEnd[dayIndex] + 1;
                    } else {
                        // Current time is less than today's start time
                        inQuietHours = false;
                        nextSchedule = mQuietHoursStart[dayIndex] - currentMinutes;
                    }
                } else {
                    // End time for today is sometime today
                    if (currentMinutes >= mQuietHoursStart[dayIndex]
                        && currentMinutes <= mQuietHoursEnd[dayIndex]) {
                        // We're between today's start and end times
                        inQuietHours = true;
                        nextSchedule = mQuietHoursEnd[dayIndex] - currentMinutes + 1;
                    } else {
                        if (currentMinutes < mQuietHoursStart[dayIndex]) {
                            // We're waiting for today's start time
                            inQuietHours = false;
                            nextSchedule = mQuietHoursStart[dayIndex] - currentMinutes;
                        } else {
                            // we're after today's end time
                            inQuietHours = false;
                            nextSchedule = FULL_DAY - currentMinutes + mQuietHoursStart[nextIndex];
                        }
                    }
                }
            }
        }

        // We're allowing daily timers and are not in quiet hours:
        // check the extra times to see if that changes, or if
        // we need to schedule to a sooner time
        if (!extraDaysInvalid && !inQuietHours) {
            int altNextSchedule = -1;

            if (mQuietHoursEnd[lastIndex] != mQuietHoursStart[lastIndex]) {
                // Yesterday's extra preference is enabled
                lastIndex = lastIndex + 7;
                if (mQuietHoursEnd[lastIndex] < mQuietHoursStart[lastIndex]) {
                    // End time of yesterday is somtime today
                    if (currentMinutes <= mQuietHoursEnd[lastIndex]) {
                        // We're before yesterday's end time
                        inQuietHours = true;
                        altNextSchedule = mQuietHoursEnd[lastIndex] - currentMinutes + 1;
                    }
                }
            }

            if (altNextSchedule == -1) {
                boolean todayDisabled = false;
                boolean tomorrowDisabled = false;
                if (mQuietHoursStart[dayIndex] == mQuietHoursEnd[dayIndex]) {
                    // Today's extra preference is disabled
                    todayDisabled = true;
                }
                if (mQuietHoursStart[nextIndex] == mQuietHoursEnd[nextIndex]) {
                    // Tomorrow's extra preference is disabled
                    tomorrowDisabled = true;
                }
                dayIndex = dayIndex + 7;
                nextIndex = nextIndex + 7;
                if (mQuietHoursStart[dayIndex] == mQuietHoursEnd[dayIndex]) {
                    // Today's extra preference is disabled
                    todayDisabled = true;
                }
                if (mQuietHoursStart[nextIndex] == mQuietHoursEnd[nextIndex]) {
                    // Tomorrow's extra preference is disabled
                    tomorrowDisabled = true;
                }
                if (mQuietHoursEnd[dayIndex] == mQuietHoursStart[dayIndex]) {
                    if (!tomorrowDisabled) {
                        // Disabled for today, schedule for tomorrow's start
                        inQuietHours = false;
                        altNextSchedule = FULL_DAY - currentMinutes + mQuietHoursStart[nextIndex];
                    }
                } else if (!todayDisabled
                        && mQuietHoursEnd[dayIndex] < mQuietHoursStart[dayIndex]) {
                    // End time for today is sometime tomorrow
                    if (currentMinutes >= mQuietHoursStart[dayIndex]) {
                        // We're after today's start time
                        inQuietHours = true;
                        altNextSchedule = FULL_DAY - currentMinutes + mQuietHoursEnd[dayIndex] + 1;
                    } else {
                        // Current time is less than today's start time
                        inQuietHours = false;
                        altNextSchedule = mQuietHoursStart[dayIndex] - currentMinutes;
                    }
                } else {
                    // End time for today is sometime today
                    if (!todayDisabled && currentMinutes >= mQuietHoursStart[dayIndex]
                        && currentMinutes <= mQuietHoursEnd[dayIndex]) {
                        // We're between today's start and end times
                        inQuietHours = true;
                        altNextSchedule = mQuietHoursEnd[dayIndex] - currentMinutes + 1;
                    } else {
                        if (!todayDisabled && currentMinutes < mQuietHoursStart[dayIndex]) {
                            // We're waiting for today's start time
                            inQuietHours = false;
                            altNextSchedule = mQuietHoursStart[dayIndex] - currentMinutes;
                        } else {
                            if (!tomorrowDisabled) {
                                // we're after today's end time
                                inQuietHours = false;
                                altNextSchedule =
                                        FULL_DAY - currentMinutes + mQuietHoursStart[nextIndex];
                            }
                        }
                    }
                }
            }

            if (altNextSchedule >= 0 && altNextSchedule < nextSchedule) {
                // Our next "extra time" event falls before the general one
                nextSchedule = altNextSchedule;
            }
        }

        if (ignoreChanges == false) {
            if (inQuietHours) {
                checkRequirements();
            } else {
                Settings.System.putIntForUser(mContext.getContentResolver(),
                        Settings.System.QUIET_HOURS_ENABLED,
                        1, UserHandle.USER_CURRENT_OR_SELF);
                toggleQuietHoursEntries(false);
            }
        }

        // Subtract to get exact time at minute's start
        calendar.add(Calendar.SECOND, -calendar.get(Calendar.SECOND));
        calendar.add(Calendar.MILLISECOND, -calendar.get(Calendar.MILLISECOND));

        // Add a second - we want to be IN quiet hours or OUT of
        // quiet hours and avoid accidental matches due to inconsistencies
        calendar.add(Calendar.SECOND, 1);

        if (nextSchedule <= 0) {
            // Our day times are invalid (unused), so we need to check
            // again tomorrow to make sure that doesn't change
            nextSchedule = FULL_DAY;
        }

        calendar.add(Calendar.MINUTE, nextSchedule);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), getScheduler());
    }

    /*
     * Called whenever the QuietHoursMode updates
     * in a manner that we care about (!= 4 && != 1 directly
     * with stipulations that may call this if requirements are met)
     */
    private void toggleQuietHoursEntries(final boolean enable) {
        if (!enable) {
            mContext.stopServiceAsUser(mServiceTriggerIntent,
                    android.os.Process.myUserHandle());
        } else {
            final boolean servicesDisabled =
                    mAutoCall == DEFAULT_DISABLED
                    && mAutoText == DEFAULT_DISABLED
                    && mCallBypass == DEFAULT_DISABLED
                    && mSmsBypass == DEFAULT_DISABLED;
            if (!servicesDisabled) {
                mContext.startServiceAsUser(mServiceTriggerIntent,
                        android.os.Process.myUserHandle());
            } else {
                mContext.stopServiceAsUser(mServiceTriggerIntent,
                        android.os.Process.myUserHandle());
            }
        }

        ContentResolver resolver = mContext.getContentResolver();

        if (Settings.System.getIntForUser(resolver,
                Settings.System.QUIET_HOURS_RINGER, 0,
                UserHandle.USER_CURRENT_OR_SELF) != 0) {
            Settings.System.putIntForUser(resolver,
                    Settings.System.QUIET_HOURS_RINGER,
                    enable ? 2 : 1, UserHandle.USER_CURRENT_OR_SELF);
        }
        if (Settings.System.getIntForUser(resolver,
                Settings.System.QUIET_HOURS_MUTE, 0,
                UserHandle.USER_CURRENT_OR_SELF) != 0) {
            Settings.System.putIntForUser(resolver,
                    Settings.System.QUIET_HOURS_MUTE,
                    enable ? 2 : 1, UserHandle.USER_CURRENT_OR_SELF);
        }
        if (Settings.System.getIntForUser(resolver,
                Settings.System.QUIET_HOURS_HAPTIC, 0,
                UserHandle.USER_CURRENT_OR_SELF) != 0) {
            Settings.System.putIntForUser(resolver,
                    Settings.System.QUIET_HOURS_HAPTIC,
                    enable ? 2 : 1, UserHandle.USER_CURRENT_OR_SELF);
        }
        if (Settings.System.getIntForUser(resolver,
                Settings.System.QUIET_HOURS_STILL, 0,
                UserHandle.USER_CURRENT_OR_SELF) != 0) {
            Settings.System.putIntForUser(resolver,
                    Settings.System.QUIET_HOURS_STILL,
                    enable ? 2 : 1, UserHandle.USER_CURRENT_OR_SELF);
        }
        if (Settings.System.getIntForUser(resolver,
                Settings.System.QUIET_HOURS_DIM, 0,
                UserHandle.USER_CURRENT_OR_SELF) != 0) {
            Settings.System.putIntForUser(resolver,
                    Settings.System.QUIET_HOURS_DIM,
                    enable ? 2 : 1, UserHandle.USER_CURRENT_OR_SELF);
        }
    }
}
