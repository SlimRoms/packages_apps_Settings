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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.slim.service.QuietHoursController;
import com.android.settings.SettingsPreferenceFragment;

public class QuietHoursTimes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "QuietHoursTimes";

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DLG_RESET = 0;

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
    private static final String KEY_QUIET_HOURS_TIMERANGE_SUN_EXTRA =
            "quiet_hours_timerange_sun_extra";
    private static final String KEY_QUIET_HOURS_TIMERANGE_MON_EXTRA =
            "quiet_hours_timerange_mon_extra";
    private static final String KEY_QUIET_HOURS_TIMERANGE_TUES_EXTRA =
            "quiet_hours_timerange_tues_extra";
    private static final String KEY_QUIET_HOURS_TIMERANGE_WED_EXTRA =
            "quiet_hours_timerange_wed_extra";
    private static final String KEY_QUIET_HOURS_TIMERANGE_THURS_EXTRA =
            "quiet_hours_timerange_thurs_extra";
    private static final String KEY_QUIET_HOURS_TIMERANGE_FRI_EXTRA =
            "quiet_hours_timerange_fri_extra";
    private static final String KEY_QUIET_HOURS_TIMERANGE_SAT_EXTRA =
            "quiet_hours_timerange_sat_extra";

    private int[] mQuietHoursStart = new int[14];
    private int[] mQuietHoursEnd = new int[14];

    private CheckBoxPreference mDailyCheck;
    private CheckBoxPreference mRequireWifi;
    private CheckBoxPreference mRequireCharging;
    private TimeRangePreference mQuietHoursTimeRange;
    private TimeRangePreference mSun;
    private TimeRangePreference mSunExtra;
    private TimeRangePreference mMon;
    private TimeRangePreference mMonExtra;
    private TimeRangePreference mTues;
    private TimeRangePreference mTuesExtra;
    private TimeRangePreference mWed;
    private TimeRangePreference mWedExtra;
    private TimeRangePreference mThurs;
    private TimeRangePreference mThursExtra;
    private TimeRangePreference mFri;
    private TimeRangePreference mFriExtra;
    private TimeRangePreference mSat;
    private TimeRangePreference mSatExtra;

    private PreferenceCategory mSunCat;
    private PreferenceCategory mMonCat;
    private PreferenceCategory mTueCat;
    private PreferenceCategory mWedCat;
    private PreferenceCategory mThuCat;
    private PreferenceCategory mFriCat;
    private PreferenceCategory mSatCat;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_time_settings);
            prefSet = getPreferenceScreen();

            mContext = getActivity().getApplicationContext();

            ContentResolver resolver = mContext.getContentResolver();

            final int startTimesSingle = Settings.System.getInt(resolver,
                    Settings.System.QUIET_HOURS_START, 720);
            final int endTimesSingle = Settings.System.getInt(resolver,
                    Settings.System.QUIET_HOURS_END, 720);

            for (int i = 0; i < 14; i++) {
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
            mSunExtra =
                    (TimeRangePreference) prefSet.findPreference(
                    KEY_QUIET_HOURS_TIMERANGE_SUN_EXTRA);
            mSunExtra.setOnPreferenceChangeListener(this);
            mMon =
                    (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_MON);
            mMon.setOnPreferenceChangeListener(this);
            mMonExtra =
                    (TimeRangePreference) prefSet.findPreference(
                    KEY_QUIET_HOURS_TIMERANGE_MON_EXTRA);
            mMonExtra.setOnPreferenceChangeListener(this);
            mTues =
                    (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_TUES);
            mTues.setOnPreferenceChangeListener(this);
            mTuesExtra =
                    (TimeRangePreference) prefSet.findPreference(
                    KEY_QUIET_HOURS_TIMERANGE_TUES_EXTRA);
            mTuesExtra.setOnPreferenceChangeListener(this);
            mWed =
                    (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_WED);
            mWed.setOnPreferenceChangeListener(this);
            mWedExtra =
                    (TimeRangePreference) prefSet.findPreference(
                    KEY_QUIET_HOURS_TIMERANGE_WED_EXTRA);
            mWedExtra.setOnPreferenceChangeListener(this);
            mThurs =
                    (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_THURS);
            mThurs.setOnPreferenceChangeListener(this);
            mThursExtra =
                    (TimeRangePreference) prefSet.findPreference(
                    KEY_QUIET_HOURS_TIMERANGE_THURS_EXTRA);
            mThursExtra.setOnPreferenceChangeListener(this);
            mFri =
                    (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_FRI);
            mFri.setOnPreferenceChangeListener(this);
            mFriExtra =
                    (TimeRangePreference) prefSet.findPreference(
                    KEY_QUIET_HOURS_TIMERANGE_FRI_EXTRA);
            mFriExtra.setOnPreferenceChangeListener(this);
            mSat =
                    (TimeRangePreference) prefSet.findPreference(KEY_QUIET_HOURS_TIMERANGE_SAT);
            mSat.setOnPreferenceChangeListener(this);
            mSatExtra =
                    (TimeRangePreference) prefSet.findPreference(
                    KEY_QUIET_HOURS_TIMERANGE_SAT_EXTRA);
            mSatExtra.setOnPreferenceChangeListener(this);

            mQuietHoursTimeRange.setTimeRange(startTimesSingle, endTimesSingle);

            mSunCat = (PreferenceCategory) findPreference("sunday");
            mMonCat = (PreferenceCategory) findPreference("monday");
            mTueCat = (PreferenceCategory) findPreference("tuesday");
            mWedCat = (PreferenceCategory) findPreference("wednesday");
            mThuCat = (PreferenceCategory) findPreference("thursday");
            mFriCat = (PreferenceCategory) findPreference("friday");
            mSatCat = (PreferenceCategory) findPreference("saturday");

            setHasOptionsMenu(true);

            setDailyRanges();
            setEnabledPrefs();
        }
        return prefSet;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
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
        } else if (preference == mSunExtra) {
            final int start = mSunExtra.getStartTime();
            final int end = mSunExtra.getEndTime();
            updateDailyTime(7, start, end);
            return true;
        } else if (preference == mMonExtra) {
            final int start = mMonExtra.getStartTime();
            final int end = mMonExtra.getEndTime();
            updateDailyTime(8, start, end);
            return true;
        } else if (preference == mTuesExtra) {
            final int start = mTuesExtra.getStartTime();
            final int end = mTuesExtra.getEndTime();
            updateDailyTime(9, start, end);
            return true;
        } else if (preference == mWedExtra) {
            final int start = mWedExtra.getStartTime();
            final int end = mWedExtra.getEndTime();
            updateDailyTime(10, start, end);
            return true;
        } else if (preference == mThursExtra) {
            final int start = mThursExtra.getStartTime();
            final int end = mThursExtra.getEndTime();
            updateDailyTime(11, start, end);
            return true;
        } else if (preference == mFriExtra) {
            final int start = mFriExtra.getStartTime();
            final int end = mFriExtra.getEndTime();
            updateDailyTime(12, start, end);
            return true;
        } else if (preference == mSatExtra) {
            final int start = mSatExtra.getStartTime();
            final int end = mSatExtra.getEndTime();
            updateDailyTime(13, start, end);
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
        mSunExtra.setEnabled(enabled && mQuietHoursStart[0] != mQuietHoursEnd[0]);
        mMonExtra.setEnabled(enabled && mQuietHoursStart[1] != mQuietHoursEnd[1]);
        mTuesExtra.setEnabled(enabled && mQuietHoursStart[2] != mQuietHoursEnd[2]);
        mWedExtra.setEnabled(enabled && mQuietHoursStart[3] != mQuietHoursEnd[3]);
        mThursExtra.setEnabled(enabled && mQuietHoursStart[4] != mQuietHoursEnd[4]);
        mFriExtra.setEnabled(enabled && mQuietHoursStart[5] != mQuietHoursEnd[5]);
        mSatExtra.setEnabled(enabled && mQuietHoursStart[6] != mQuietHoursEnd[6]);
    }

    private void updateController() {
        QuietHoursController.getInstance(mContext).updateTimePrefs();
        QuietHoursController.getInstance(mContext).scheduleService();
    }

    private void updateDailyTime(final int index, int start, int end) {
        final boolean extraTime = index > 6;
        ContentResolver resolver = mContext.getContentResolver();
        boolean warning = false;
        int lastIndex = index - 1;
        int nextIndex = index + 1;
        if (extraTime) {
            if (lastIndex == 6) {
                lastIndex = 13;
            }
            if (nextIndex == 14) {
                nextIndex = 7;
            }
        } else {
            if (lastIndex == -1) {
                lastIndex = 6;
            }
            if (nextIndex == 7) {
                nextIndex = 0;
            }
        }

        int alternateIndex = extraTime ? index - 7 : index + 7;
        int altLastIndex = extraTime ? index - 8 : index + 6;
        int altNextIndex = extraTime ? index - 6 : index + 8;
        if (altLastIndex == -1) {
            altLastIndex = 6;
        } else if (altLastIndex == 6) {
            altLastIndex = 13;
        }
        if (altNextIndex == 7) {
            altNextIndex = 0;
        } else if (altNextIndex == 14) {
            altNextIndex = 7;
        }

        final boolean ignoreAlt = !extraTime &&
                mQuietHoursEnd[alternateIndex] == mQuietHoursStart[alternateIndex];

        final boolean ignoreAltNext = !extraTime &&
                mQuietHoursEnd[altNextIndex] == mQuietHoursStart[altNextIndex];

        if (start > end) {
            // Starts today, ends tomorrow
            if (mQuietHoursStart[alternateIndex] > mQuietHoursEnd[alternateIndex]) {
                // Today's alternate time starts and ends before/after midnight as well
                warning = true;
                mQuietHoursEnd[alternateIndex] = start;
            }

            if (start < mQuietHoursStart[alternateIndex]) {
                // Starts before today's set start time - reset
                if (!ignoreAlt) {
                    warning = true;
                }
                mQuietHoursStart[alternateIndex] = start;
            }

            if (start < mQuietHoursEnd[alternateIndex]) {
                // Starts before today's set end time - reset
                if (!ignoreAlt) {
                    warning = true;
                }
                mQuietHoursEnd[alternateIndex] = start;
            }

            if (mQuietHoursStart[lastIndex] > mQuietHoursEnd[lastIndex]) {
                if (start < mQuietHoursEnd[lastIndex]) {
                    // Starts before yesterday's set end time - reset
                    warning = true;
                    mQuietHoursEnd[lastIndex] = start;
                }
            }

            if (mQuietHoursStart[altLastIndex] > mQuietHoursEnd[altLastIndex]) {
                if (start < mQuietHoursEnd[altLastIndex]) {
                    // Starts before yesterday's extra end time - reset
                    warning = true;
                    mQuietHoursEnd[lastIndex] = start;
                }
            }

            if (mQuietHoursStart[altNextIndex] == mQuietHoursEnd[altNextIndex]) {
                if (end > mQuietHoursStart[altNextIndex]) {
                    // No warning, pref is currently not in use anyway
                    mQuietHoursEnd[altNextIndex] = end;
                    mQuietHoursStart[altNextIndex] = end;
                }
            }

            if (mQuietHoursStart[altNextIndex] < mQuietHoursEnd[altNextIndex]) {
                // Tomorrow starts/stops same day
                if (end > mQuietHoursEnd[altNextIndex]) {
                    warning = true;
                    mQuietHoursEnd[altNextIndex] = end;
                }
            }
            if (end > mQuietHoursStart[altNextIndex]) {
                // Ends after tomorrow's set extra start time - reset
                if (!ignoreAltNext) {
                    warning = true;
                }
                mQuietHoursStart[altNextIndex] = end;
            }

            if (mQuietHoursStart[nextIndex] == mQuietHoursEnd[nextIndex]) {
                if (end > mQuietHoursStart[nextIndex]) {
                    // No warning, pref is currently not in use anyway
                    mQuietHoursEnd[nextIndex] = end;
                    mQuietHoursStart[nextIndex] = end;
                }
            }
            if (mQuietHoursStart[nextIndex] < mQuietHoursEnd[nextIndex]) {
                // Tomorrow starts/stops same day
                if (end > mQuietHoursEnd[nextIndex]) {
                    warning = true;
                    mQuietHoursEnd[nextIndex] = end;
                }
            }
            if (end > mQuietHoursStart[nextIndex]) {
                // Ends after tomorrow's set extra start time - reset
                mQuietHoursStart[nextIndex] = end;
                if (mQuietHoursStart[nextIndex] != mQuietHoursEnd[nextIndex]) {
                    warning = true;
                } else {
                    mQuietHoursEnd[nextIndex] = end;
                }
            }
        } else {
            // Starts today, ends today
            if (mQuietHoursStart[lastIndex] > mQuietHoursEnd[lastIndex]) {
                if (start < mQuietHoursEnd[lastIndex]) {
                    // Last end time is greater than our start
                    warning = true;
                    mQuietHoursEnd[lastIndex] = start;
                }
            }
            if (mQuietHoursStart[altLastIndex] > mQuietHoursEnd[altLastIndex]) {
                if (start < mQuietHoursEnd[altLastIndex]) {
                    // Alternate index last end time is greater than our start
                    warning = true;
                    mQuietHoursEnd[altLastIndex] = start;
                }
            }
            if (mQuietHoursStart[alternateIndex] > mQuietHoursEnd[alternateIndex]) {
                if (end > mQuietHoursStart[alternateIndex]) {
                    // Alternate start is before our new end time
                    warning = true;
                    mQuietHoursStart[alternateIndex] = end;
                }
            } else {
                boolean fixed = false;
                if (mQuietHoursEnd[alternateIndex] == mQuietHoursStart[alternateIndex]) {
                    // Will only fire for alternate times
                    fixed = true;
                    mQuietHoursStart[alternateIndex] = start;
                    mQuietHoursEnd[alternateIndex] = start;
                }
                if (!fixed && end >= mQuietHoursEnd[alternateIndex]
                        && start <= mQuietHoursStart[alternateIndex]) {
                    warning = true;
                    fixed = true;
                    if (extraTime) {
                        mQuietHoursStart[alternateIndex] = start;
                        mQuietHoursEnd[alternateIndex] = end;
                        start = mQuietHoursStart[alternateIndex];
                        end = start;
                    } else {
                        mQuietHoursStart[alternateIndex] = start;
                        mQuietHoursEnd[alternateIndex] = start;
                    }
                }
                if (!fixed && end > mQuietHoursEnd[alternateIndex]
                        && start <= mQuietHoursEnd[alternateIndex]) {
                        // In between times
                        fixed = true;
                        warning = true;
                        mQuietHoursEnd[alternateIndex] = start;
                }
                if (!fixed && end > mQuietHoursStart[alternateIndex]
                        && start <= mQuietHoursStart[alternateIndex]) {
                        // In between times
                        warning = true;
                        mQuietHoursStart[alternateIndex] = end;
                }
                if (mQuietHoursStart[altLastIndex] > mQuietHoursEnd[altLastIndex]) {
                    if (mQuietHoursEnd[altLastIndex] > start) {
                        warning = true;
                        mQuietHoursEnd[altLastIndex] = start;
                    }
                }
                if (mQuietHoursStart[lastIndex] > mQuietHoursEnd[lastIndex]) {
                    if (mQuietHoursEnd[lastIndex] > start) {
                        warning = true;
                        mQuietHoursEnd[lastIndex] = start;
                    }
                }
            }
        }

        if (ignoreAltNext) {
            mQuietHoursEnd[altNextIndex] = mQuietHoursStart[altNextIndex];
        }

        if (ignoreAlt) {
            mQuietHoursStart[alternateIndex] = start;
            mQuietHoursEnd[alternateIndex] = mQuietHoursStart[alternateIndex];
        }

        mQuietHoursStart[index] = start;
        mQuietHoursEnd[index] = end;

        if (warning) {
            Toast.makeText(getActivity(), R.string.quiet_hours_times_warning,
                    Toast.LENGTH_LONG).show();
        }

        for (int i = 0; i < 14; i++) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START_TIMES[i],
                    mQuietHoursStart[i]);
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END_TIMES[i],
                    mQuietHoursEnd[i]);
        }

        setDailyRanges();
        updateController();
        setEnabledPrefs();
    }

    private void setDailyRanges() {
        mSun.setTimeRange(mQuietHoursStart[0], mQuietHoursEnd[0]);
        mMon.setTimeRange(mQuietHoursStart[1], mQuietHoursEnd[1]);
        mTues.setTimeRange(mQuietHoursStart[2], mQuietHoursEnd[2]);
        mWed.setTimeRange(mQuietHoursStart[3], mQuietHoursEnd[3]);
        mThurs.setTimeRange(mQuietHoursStart[4], mQuietHoursEnd[4]);
        mFri.setTimeRange(mQuietHoursStart[5], mQuietHoursEnd[5]);
        mSat.setTimeRange(mQuietHoursStart[6], mQuietHoursEnd[6]);
        mSunExtra.setTimeRange(mQuietHoursStart[7], mQuietHoursEnd[7]);
        mMonExtra.setTimeRange(mQuietHoursStart[8], mQuietHoursEnd[8]);
        mTuesExtra.setTimeRange(mQuietHoursStart[9], mQuietHoursEnd[9]);
        mWedExtra.setTimeRange(mQuietHoursStart[10], mQuietHoursEnd[10]);
        mThursExtra.setTimeRange(mQuietHoursStart[11], mQuietHoursEnd[11]);
        mFriExtra.setTimeRange(mQuietHoursStart[12], mQuietHoursEnd[12]);
        mSatExtra.setTimeRange(mQuietHoursStart[13], mQuietHoursEnd[13]);
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
        final String through = mContext.getResources().getString(R.string.arrow_right);
        boolean twoDay = false;

        if (mQuietHoursEnd[0] < mQuietHoursStart[0]) {
            twoDay = true;
            mSun.setAppendedText(sun, mon);
        } else {
            mSun.setAppendedText(sun, sun);
        }
        if (mQuietHoursEnd[7] < mQuietHoursStart[7]) {
            twoDay = true;
            mSunExtra.setAppendedText(sun, mon);
        } else {
            mSunExtra.setAppendedText(sun, sun);
        }
        if (twoDay) {
            mSunCat.setTitle(sun + through + mon);
        } else {
            mSunCat.setTitle(sun);
        }
        twoDay = false;
        if (mQuietHoursEnd[1] < mQuietHoursStart[1]) {
            twoDay = true;
            mMon.setAppendedText(mon, tues);
        } else {
            mMon.setAppendedText(mon, mon);
        }
        if (mQuietHoursEnd[8] < mQuietHoursStart[8]) {
            twoDay = true;
            mMonExtra.setAppendedText(mon, tues);
        } else {
            mMonExtra.setAppendedText(mon, mon);
        }
        if (twoDay) {
            mMonCat.setTitle(mon + through + tues);
        } else {
            mMonCat.setTitle(mon);
        }
        twoDay = false;
        if (mQuietHoursEnd[2] < mQuietHoursStart[2]) {
            twoDay = true;
            mTues.setAppendedText(tues, wed);
        } else {
            mTues.setAppendedText(tues, tues);
        }
        if (mQuietHoursEnd[9] < mQuietHoursStart[9]) {
            twoDay = true;
            mTuesExtra.setAppendedText(tues, wed);
        } else {
            mTuesExtra.setAppendedText(tues, tues);
        }
        if (twoDay) {
            mTueCat.setTitle(tues + through + wed);
        } else {
            mTueCat.setTitle(tues);
        }
        twoDay = false;
        if (mQuietHoursEnd[3] < mQuietHoursStart[3]) {
            twoDay = true;
            mWed.setAppendedText(wed, thur);
        } else {
            mWed.setAppendedText(wed, wed);
        }
        if (mQuietHoursEnd[10] < mQuietHoursStart[10]) {
            twoDay = true;
            mWedExtra.setAppendedText(wed, thur);
        } else {
            mWedExtra.setAppendedText(wed, wed);
        }
        if (twoDay) {
            mWedCat.setTitle(wed + through + thur);
        } else {
            mWedCat.setTitle(wed);
        }
        twoDay = false;
        if (mQuietHoursEnd[4] < mQuietHoursStart[4]) {
            twoDay = true;
            mThurs.setAppendedText(thur, fri);
        } else {
            mThurs.setAppendedText(thur, thur);
        }
        if (mQuietHoursEnd[11] < mQuietHoursStart[11]) {
            twoDay = true;
            mThursExtra.setAppendedText(thur, fri);
        } else {
            mThursExtra.setAppendedText(thur, thur);
        }
        if (twoDay) {
            mThuCat.setTitle(thur + through + fri);
        } else {
            mThuCat.setTitle(thur);
        }
        twoDay = false;
        if (mQuietHoursEnd[5] < mQuietHoursStart[5]) {
            twoDay = true;
            mFri.setAppendedText(fri, sat);
        } else {
            mFri.setAppendedText(fri, fri);
        }
        if (mQuietHoursEnd[12] < mQuietHoursStart[12]) {
            twoDay = true;
            mFriExtra.setAppendedText(fri, sat);
        } else {
            mFriExtra.setAppendedText(fri, fri);
        }
        if (twoDay) {
            mFriCat.setTitle(fri + through + sat);
        } else {
            mFriCat.setTitle(fri);
        }
        twoDay = false;
        if (mQuietHoursEnd[6] < mQuietHoursStart[6]) {
            twoDay = true;
            mSat.setAppendedText(sat, sun);
        } else {
            mSat.setAppendedText(sat, sat);
        }
        if (mQuietHoursEnd[13] < mQuietHoursStart[13]) {
            twoDay = true;
            mSatExtra.setAppendedText(sat, sun);
        } else {
            mSatExtra.setAppendedText(sat, sat);
        }
        if (twoDay) {
            mSatCat.setTitle(sat + through + sun);
        } else {
            mSatCat.setTitle(sat);
        }
        twoDay = false;
    }

    private void resetTimes() {
        ContentResolver resolver = mContext.getContentResolver();
        for (int i = 0; i < 14; i++) {
            Settings.System.putInt(resolver,
                    Settings.System.QUIET_HOURS_START_TIMES[i],
                    720);
            Settings.System.putInt(resolver,
                    Settings.System.QUIET_HOURS_END_TIMES[i],
                    720);
        }
        Settings.System.putInt(resolver,
                Settings.System.QUIET_HOURS_START, 720);
        Settings.System.putInt(resolver,
                Settings.System.QUIET_HOURS_END, 720);
        updateController();
        createCustomView();
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        QuietHoursTimes getOwner() {
            return (QuietHoursTimes) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.quiet_hours_reset_times)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetTimes();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }
}
