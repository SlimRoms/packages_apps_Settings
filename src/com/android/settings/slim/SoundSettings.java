/*
 * Copyright (C) 2012 CyanogenMod
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.VolumePanel;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    private static final String KEY_VOLUME_OVERLAY = "volume_overlay";
    private static final String KEY_SAFE_HEADSET_RESTORE = "safe_headset_restore";
    private static final String KEY_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";
    private static final String KEY_CONVERT_SOUND_TO_VIBRATE = "notification_convert_sound_to_vibration";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String PREF_LESS_NOTIFICATION_SOUNDS = "less_notification_sounds";

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mVolumeOverlay;
    private CheckBoxPreference mSafeHeadsetRestore;
    private CheckBoxPreference mVolBtnMusicCtrl;
    private CheckBoxPreference mConvertSoundToVibration;
    private CheckBoxPreference mVolumeAdjustSounds;
    private ListPreference mAnnoyingNotifications;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();

        addPreferencesFromResource(R.xml.sound_settings_rom);

        mVolumeOverlay = (ListPreference) findPreference(KEY_VOLUME_OVERLAY);
        mVolumeOverlay.setOnPreferenceChangeListener(this);
        int volumeOverlay = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_VOLUME_OVERLAY,
                VolumePanel.VOLUME_OVERLAY_EXPANDABLE);
        mVolumeOverlay.setValue(Integer.toString(volumeOverlay));
        mVolumeOverlay.setSummary(mVolumeOverlay.getEntry());

        mSafeHeadsetRestore = (CheckBoxPreference) findPreference(KEY_SAFE_HEADSET_RESTORE);
        mSafeHeadsetRestore.setPersistent(false);
        mSafeHeadsetRestore.setChecked(Settings.System.getInt(resolver,
                Settings.System.SAFE_HEADSET_VOLUME_RESTORE, 1) != 0);

        mVolBtnMusicCtrl = (CheckBoxPreference) findPreference(KEY_VOLBTN_MUSIC_CTRL);
        mVolBtnMusicCtrl.setChecked(Settings.System.getInt(resolver,
                Settings.System.VOLBTN_MUSIC_CONTROLS, 1) != 0);

        mConvertSoundToVibration = (CheckBoxPreference) findPreference(KEY_CONVERT_SOUND_TO_VIBRATE);
        mConvertSoundToVibration.setPersistent(false);
        mConvertSoundToVibration.setChecked(Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_CONVERT_SOUND_TO_VIBRATION, 1) == 1);

        mVolumeAdjustSounds = (CheckBoxPreference) findPreference(KEY_VOLUME_ADJUST_SOUNDS);
        mVolumeAdjustSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED, 1) != 0);

        mAnnoyingNotifications = (ListPreference) findPreference(PREF_LESS_NOTIFICATION_SOUNDS);
        mAnnoyingNotifications.setOnPreferenceChangeListener(this);
        int notificationThreshold = Settings.System.getInt(resolver,
                Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD,
                0);
        mAnnoyingNotifications.setValue(Integer.toString(notificationThreshold));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // updateState in fact updates the UI to reflect the system state
    private void updateState(boolean force) {
        if (getActivity() == null) return;
        ContentResolver resolver = getContentResolver();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mSafeHeadsetRestore) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SAFE_HEADSET_VOLUME_RESTORE,
                    mSafeHeadsetRestore.isChecked() ? 1 : 0);

        } else if (preference == mVolBtnMusicCtrl) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLBTN_MUSIC_CONTROLS,
                    mVolBtnMusicCtrl.isChecked() ? 1 : 0);

        } else if (preference == mConvertSoundToVibration) {
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_CONVERT_SOUND_TO_VIBRATION,
                    mConvertSoundToVibration.isChecked() ? 1 : 0);

        } else if (preference == mVolumeAdjustSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED,
                    mVolumeAdjustSounds.isChecked() ? 1 : 0);

        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        if (preference == mVolumeOverlay) {
            final int value = Integer.valueOf((String) objValue);
            final int index = mVolumeOverlay.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.MODE_VOLUME_OVERLAY, value);
            mVolumeOverlay.setSummary(mVolumeOverlay.getEntries()[index]);
        } else if (preference == mAnnoyingNotifications) {
            final int val = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.MUTE_ANNOYING_NOTIFICATIONS_THRESHOLD, val);
        }

        return true;
    }
}
