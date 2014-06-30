/*
 * Copyright (C) 2014 SlimRoms - Jubakuba
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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.slim.service.QuietHoursController;
import com.android.settings.SettingsPreferenceFragment;

public class QuietHoursTimes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "QuietHoursTimes";

    private static final String KEY_DAILY_CHECKBOX = "daily_checkbox";
    private static final String KEY_REQUIRE_WIFI = "require_wifi";
    private static final String KEY_REQUIRE_CHARGING = "require_charging";
    private static final String KEY_QUIET_HOURS_TIMERANGE = "quiet_hours_timerange";
    private static final String KEY_QUIET_HOURS_TIMERANGE_SUN = "quiet_hours_timerange_sun";
    private static final String KEY_QUIET_HOURS_TIMERANGE_MON = "quiet_hours_timerange_mon";
    private static final String KEY_QUIET_HOURS_TIMERANGE_TUES = "quiet_hours_timerange_tues";
    private static final String KEY_QUIET_HOURS_TIMERANGE_WED = "quiet_hours_timerange_wed";
    private static final String KEY_QUIET_HOURS_TIMERANGE_THURS = "quiet_hours_timerange_thurs";
    private static final String KEY_QUIET_HOURS_TIMERANGE_FRI = "quiet_hours_timerange_fri";
    private static final String KEY_QUIET_HOURS_TIMERANGE_SAT = "quiet_hours_timerange_sat";

    private int[] mQuietHoursStart = new int[7];
    private int[] mQuietHoursEnd = new int[7];

    private CheckBoxPreference mDailyCheck;
    private CheckBoxPreference mRequireWifi;
    private CheckBoxPreference mRequireCharging;
    private TimeRangePreference mQuietHoursTimeRange;
    private TimeRangePreference mSun;
    private TimeRangePreference mMon;
    private TimeRangePreference mTues;
    private TimeRangePreference mWed;
    private TimeRangePreference mThurs;
    private TimeRangePreference mFri;
    private TimeRangePreference mSat;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_time_settings);

            mContext = getActivity().getApplicationContext();

            ContentResolver resolver = mContext.getContentResolver();

            PreferenceScreen prefSet = getPreferenceScreen();

            final int startTimesSingle = Settings.System.getInt(resolver,
                    Settings.System.QUIET_HOURS_START, 720);
            final int endTimesSingle = Settings.System.getInt(resolver,
                    Settings.System.QUIET_HOURS_END, 720);

            for (int i = 0; i < 7; i++) {
                    mQuietHoursStart[i] = Settings.System.getInt(resolver,
                        Settings.System.QUIET_HOURS_START_TIMES[i], 720);
                    mQuietHoursEnd[i] = Settings.System.getInt(resolver,
                        Settings.System.QUIET_HOURS_END_TIMES[i], 720);
            }

            final boolean useDaily = Settings.System.getInt(
                    resolver, Settings.System.QUIET_HOURS_DAILY, 0) == 1;
            mDailyCheck =
                (CheckBoxPreference) prefSet.findPreference(KEY_DAILY_CHECKBOX);
            mDailyCheck.setOnPreferenceChangeListener(this);
            mDailyCheck.setChecked(useDaily);
            mRequireWifi =
                (CheckBoxPreference) prefSet.findPreference(KEY_REQUIRE_WIFI);
            mRequireWifi.setOnPreferenceChangeListener(this);
            final int requireWifi = Settings.System.getInt(resolver,
                    Settings.System.QUIET_HOURS_REQUIRE_WIFI, 0);
            mRequireWifi.setChecked(requireWifi == 1 || requireWifi == 2);
            mRequireCharging =
                (CheckBoxPreference) prefSet.findPreference(KEY_REQUIRE_CHARGING);
            mRequireCharging.setOnPreferenceChangeListener(this);
            final int requireCharging = Settings.System.getInt(resolver,
                    Settings.System.QUIET_HOURS_REQUIRE_CHARGING, 0);
            mRequireCharging.setChecked(requireCharging == 1 || requireCharging == 2);
            mQuietHoursTimeRange =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE);
            mQuietHoursTimeRange.setOnPreferenceChangeListener(this);
            mSun =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_SUN);
            mSun.setOnPreferenceChangeListener(this);
            mMon =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_MON);
            mMon.setOnPreferenceChangeListener(this);
            mTues =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_TUES);
            mTues.setOnPreferenceChangeListener(this);
            mWed =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_WED);
            mWed.setOnPreferenceChangeListener(this);
            mThurs =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_THURS);
            mThurs.setOnPreferenceChangeListener(this);
            mFri =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_FRI);
            mFri.setOnPreferenceChangeListener(this);
            mSat =
                (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_SAT);
            mSat.setOnPreferenceChangeListener(this);

            mQuietHoursTimeRange.setTimeRange(startTimesSingle, endTimesSingle);
            setDailyRanges();
            setEnabledPrefs();
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = mContext.getContentResolver();
        if (preference == mDailyCheck) {
            final boolean useDaily = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_DAILY,
                    useDaily ? 1 : 0);
            mDailyCheck.setChecked(useDaily);
            setEnabledPrefs();
            updateController();
        } else if (preference == mRequireWifi) {
            final boolean requireWifi = (Boolean) newValue;
            if (!requireWifi) {
                Settings.System.putInt(resolver,
                        Settings.System.QUIET_HOURS_REQUIRE_WIFI, 0);
            } else {
                ConnectivityManager c = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifi = c.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                Settings.System.putInt(resolver,
                        Settings.System.QUIET_HOURS_REQUIRE_WIFI,
                        wifi.isConnected() ? 2 : 1);
            }
            mRequireWifi.setChecked(requireWifi);
            QuietHoursController.getInstance(mContext).checkModes();
        } else if (preference == mRequireCharging) {
            final boolean requireCharging = (Boolean) newValue;
            if (!requireCharging) {
                Settings.System.putInt(resolver,
                        Settings.System.QUIET_HOURS_REQUIRE_CHARGING, 0);
            } else {
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = mContext.registerReceiver(null, ifilter);
                final int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                final boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

                Settings.System.putInt(resolver,
                        Settings.System.QUIET_HOURS_REQUIRE_CHARGING, isCharging ? 2 : 1);
            }
            mRequireCharging.setChecked(requireCharging);
            QuietHoursController.getInstance(mContext).checkModes();
        } else if (preference == mQuietHoursTimeRange) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START,
                    mQuietHoursTimeRange.getStartTime());
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END,
                    mQuietHoursTimeRange.getEndTime());
            updateController();
            return true;
        } else if (preference == mSun) {
            final int start = mSun.getStartTime();
            final int end = mSun.getEndTime();
            updateDailyTime(0, start, end);
            return true;
        } else if (preference == mMon) {
            final int start = mMon.getStartTime();
            final int end = mMon.getEndTime();
            updateDailyTime(1, start, end);
            return true;
        } else if (preference == mTues) {
            final int start = mTues.getStartTime();
            final int end = mTues.getEndTime();
            updateDailyTime(2, start, end);
            return true;
        } else if (preference == mWed) {
            final int start = mWed.getStartTime();
            final int end = mWed.getEndTime();
            updateDailyTime(3, start, end);
            return true;
        } else if (preference == mThurs) {
            final int start = mThurs.getStartTime();
            final int end = mThurs.getEndTime();
            updateDailyTime(4, start, end);
            return true;
        } else if (preference == mFri) {
            final int start = mFri.getStartTime();
            final int end = mFri.getEndTime();
            updateDailyTime(5, start, end);
            return true;
        } else if (preference == mSat) {
            final int start = mSat.getStartTime();
            final int end = mSat.getEndTime();
            updateDailyTime(6, start, end);
            return true;
        }
        return false;
    }

    private void setEnabledPrefs() {
        final boolean enabled = mDailyCheck.isChecked();
        mQuietHoursTimeRange.setEnabled(!enabled);
        mSun.setEnabled(enabled);
        mMon.setEnabled(enabled);
        mTues.setEnabled(enabled);
        mWed.setEnabled(enabled);
        mThurs.setEnabled(enabled);
        mFri.setEnabled(enabled);
        mSat.setEnabled(enabled);
    }

    private void updateController() {
        QuietHoursController.getInstance(mContext).updateTimePrefs();
        QuietHoursController.getInstance(mContext).scheduleService();
    }

    private void updateDailyTime(final int index, int start, int end) {
        ContentResolver resolver = mContext.getContentResolver();

        int lastIndex = index - 1;
        int nextIndex = index + 1;
        if (lastIndex == -1) {
            lastIndex = 6;
        }
        if (nextIndex == 7) {
            nextIndex = 0;
        }
        boolean warning = false;
        if (start > end) {
            // ends the next day
            if (start < mQuietHoursEnd[lastIndex] &&
                    mQuietHoursStart[lastIndex] > mQuietHoursEnd[lastIndex]) {
                // Start is before yesterday's end
                warning = true;
                mQuietHoursEnd[lastIndex] = start;
            }

            if (mQuietHoursStart[nextIndex] < end) {
                warning = true;
                mQuietHoursStart[nextIndex] = end;
            }

            if (mQuietHoursEnd[nextIndex] < end
                    && mQuietHoursStart[nextIndex]
                    <= mQuietHoursEnd[nextIndex]) {
                warning = true;
                mQuietHoursEnd[nextIndex] = end;
            }
        } else {
            if (mQuietHoursStart[lastIndex] > mQuietHoursEnd[lastIndex]) {
                if (mQuietHoursEnd[lastIndex] > start) {
                    warning = true;
                    mQuietHoursEnd[lastIndex] = start;
                }
            }
        }

        if (warning) {
            Toast.makeText(getActivity(), R.string.quiet_hours_times_warning,
                    Toast.LENGTH_LONG).show();
        }

        mQuietHoursStart[index] = start;
        mQuietHoursEnd[index] = end;
        for (int i = 0; i < 7; i++) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START_TIMES[i],
                    mQuietHoursStart[i]);
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END_TIMES[i],
                    mQuietHoursEnd[i]);
        }

        setDailyRanges();
        updateController();
    }

    private void setDailyRanges() {
        mSun.setTimeRange(mQuietHoursStart[0], mQuietHoursEnd[0]);
        mMon.setTimeRange(mQuietHoursStart[1], mQuietHoursEnd[1]);
        mTues.setTimeRange(mQuietHoursStart[2], mQuietHoursEnd[2]);
        mWed.setTimeRange(mQuietHoursStart[3], mQuietHoursEnd[3]);
        mThurs.setTimeRange(mQuietHoursStart[4], mQuietHoursEnd[4]);
        mFri.setTimeRange(mQuietHoursStart[5], mQuietHoursEnd[5]);
        mSat.setTimeRange(mQuietHoursStart[6], mQuietHoursEnd[6]);
        updateSummaries();
    }

    private void updateSummaries() {
        final String sun = mContext.getResources().getString(R.string.sunday);
        final String mon = mContext.getResources().getString(R.string.monday);
        final String tues = mContext.getResources().getString(R.string.tuesday);
        final String wed = mContext.getResources().getString(R.string.wednesday);
        final String thur = mContext.getResources().getString(R.string.thursday);
        final String fri = mContext.getResources().getString(R.string.friday);
        final String sat = mContext.getResources().getString(R.string.saturday);

        if (mQuietHoursEnd[0] < mQuietHoursStart[0]) {
            mSun.setAppendedText(sun, mon);
        } else {
            mSun.setAppendedText(sun, sun);
        }
        if (mQuietHoursEnd[1] < mQuietHoursStart[1]) {
            mMon.setAppendedText(mon, tues);
        } else {
            mMon.setAppendedText(mon, mon);
        }
        if (mQuietHoursEnd[2] < mQuietHoursStart[2]) {
            mTues.setAppendedText(tues, wed);
        } else {
            mTues.setAppendedText(tues, tues);
        }
        if (mQuietHoursEnd[3] < mQuietHoursStart[3]) {
            mWed.setAppendedText(wed, thur);
        } else {
            mWed.setAppendedText(wed, wed);
        }
        if (mQuietHoursEnd[4] < mQuietHoursStart[4]) {
            mThurs.setAppendedText(thur, fri);
        } else {
            mThurs.setAppendedText(thur, thur);
        }
        if (mQuietHoursEnd[5] < mQuietHoursStart[5]) {
            mFri.setAppendedText(fri, sat);
        } else {
            mFri.setAppendedText(fri, fri);
        }
        if (mQuietHoursEnd[6] < mQuietHoursStart[6]) {
            mSat.setAppendedText(sat, sun);
        } else {
            mSat.setAppendedText(sat, sat);
        }
    }
}
