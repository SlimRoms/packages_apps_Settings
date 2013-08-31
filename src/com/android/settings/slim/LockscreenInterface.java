/*
 * Copyright (C) 2013 Slimroms
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.internal.view.RotationPolicy;
import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.widget.SeekBarPreference;

import java.io.File;
import java.io.IOException;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import net.margaritov.preference.colorpicker.ColorPickerView;

public class LockscreenInterface extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";

    private static final int LOCKSCREEN_BACKGROUND = 1024;

    private static final String KEY_ADDITIONAL_OPTIONS = "options_group";
    private static final String KEY_SLIDER_OPTIONS = "slider_group";
    private static final String KEY_WIDGET_OPTIONS = "lockscreen_widgets_group";
    private static final String KEY_ALWAYS_BATTERY_PREF = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String PREF_LOCKSCREEN_AUTO_ROTATE = "lockscreen_auto_rotate";
    private static final String PREF_LOCKSCREEN_EIGHT_TARGETS = "lockscreen_eight_targets";
    private static final String PREF_LOCKSCREEN_SHORTCUTS = "lockscreen_shortcuts";

    private static final String KEY_BACKGROUND_PREF = "lockscreen_background";
    private static final String KEY_BACKGROUND_ALPHA_PREF = "lockscreen_alpha";

    private ListPreference mBatteryStatus;
    private PreferenceScreen mLockscreenButtons;
    private PreferenceCategory mAdditionalOptions;
    private ListPreference mCustomBackground;
    private SeekBarPreference mBgAlpha;
    private CheckBoxPreference mLockscreenAutoRotate;
    private CheckBoxPreference mLockscreenEightTargets;
    private Preference mShortcuts;

    private boolean mIsScreenLarge;

    private boolean mCheckPreferences;

    private Activity mActivity;
    private ContentResolver mResolver;
    private File wallpaperImage;
    private File wallpaperTemporary;

    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();

        mIsScreenLarge = Utils.isTablet(getActivity());

        createCustomLockscreenView();
    }

    private PreferenceScreen createCustomLockscreenView() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        prefs = getPreferenceScreen();

        mAdditionalOptions = (PreferenceCategory) prefs.findPreference(KEY_ADDITIONAL_OPTIONS);

        mCustomBackground = (ListPreference) findPreference(KEY_BACKGROUND_PREF);
        mCustomBackground.setOnPreferenceChangeListener(this);
        wallpaperImage = new File(mActivity.getFilesDir()+"/lockwallpaper");
        wallpaperTemporary = new File(mActivity.getCacheDir()+"/lockwallpaper.tmp");

        float bgAlpha;
        try{
            bgAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.LOCKSCREEN_ALPHA);
         }catch (Exception e) {
            bgAlpha = 0;
            bgAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.LOCKSCREEN_ALPHA, 0.0f);
         }
        mBgAlpha = (SeekBarPreference) findPreference(KEY_BACKGROUND_ALPHA_PREF);
        mBgAlpha.setInitValue((int) (bgAlpha * 100));
        mBgAlpha.setProperty(Settings.System.LOCKSCREEN_ALPHA);
        mBgAlpha.setOnPreferenceChangeListener(this);

        mBatteryStatus = (ListPreference) findPreference(KEY_ALWAYS_BATTERY_PREF);
        mBatteryStatus.setOnPreferenceChangeListener(this);
        setBatteryStatusSummary();

        mLockscreenAutoRotate = (CheckBoxPreference) findPreference(PREF_LOCKSCREEN_AUTO_ROTATE);
        int defaultValue = getResources().getBoolean(com.android.internal.R.bool.config_enableLockScreenRotation) ? 1 : 0;
        mLockscreenAutoRotate.setChecked(Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_AUTO_ROTATE, defaultValue) == 1);
        mLockscreenAutoRotate.setOnPreferenceChangeListener(this);

        mLockscreenEightTargets = (CheckBoxPreference) findPreference(PREF_LOCKSCREEN_EIGHT_TARGETS);
        mLockscreenEightTargets.setChecked(Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_EIGHT_TARGETS, 0) == 1);
        mLockscreenEightTargets.setOnPreferenceChangeListener(this);

        mShortcuts = (Preference) findPreference(PREF_LOCKSCREEN_SHORTCUTS);
        mShortcuts.setEnabled(!mLockscreenEightTargets.isChecked());

        if (RotationPolicy.isRotationLocked(getActivity())) {
            mLockscreenAutoRotate.setEnabled(false);
            mLockscreenAutoRotate.setSummary(getResources().getString(R.string.lockscreen_no_rotate_summary));
        }

        mLockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
        if (!hasButtons()) {
            mAdditionalOptions.removePreference(mLockscreenButtons);
        }

        final int unsecureUnlockMethod = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.LOCKSCREEN_UNSECURE_USED, 1);
        final int lockBeforeUnlock = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.LOCK_BEFORE_UNLOCK, 0);

        PreferenceCategory sliderCategory = (PreferenceCategory) findPreference(KEY_SLIDER_OPTIONS);

        //setup custom lockscreen customize view
        if ((unsecureUnlockMethod != 1 && lockBeforeUnlock == 0)
                 || unsecureUnlockMethod == -1) {
             getPreferenceScreen().removePreference(sliderCategory);
        } else if (!Utils.isPhone(getActivity())) {
             // Nothing for tablets and large screen devices
             sliderCategory.removePreference(mShortcuts);
             sliderCategory.removePreference(mLockscreenEightTargets);
        }

        setBatteryStatusSummary();
        updateCustomBackgroundSummary();
        mCheckPreferences = true;
        return prefs;
    }


    @Override
    public void onResume() {
        super.onResume();
        createCustomLockscreenView();
    }

    @Override
    public void onPause() {
        super.onPause();
        setBatteryStatusSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mLockscreenAutoRotate) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_AUTO_ROTATE, (Boolean) objValue ? 1 : 0);
            return true;
        } else if (preference == mLockscreenEightTargets) {
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_EIGHT_TARGETS, (Boolean) objValue ? 1 : 0);
            mShortcuts.setEnabled(!((Boolean) objValue));
            Settings.System.putString(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_TARGETS, GlowPadView.EMPTY_TARGET);
            for (File pic : mActivity.getFilesDir().listFiles()) {
                if (pic.getName().startsWith("lockscreen_")) {
                    pic.delete();
                }
            }
            return true;
        } else if (preference == mCustomBackground) {
            int indexOf = mCustomBackground.findIndexOfValue(objValue.toString());
            switch (indexOf) {
            //Displays color dialog when user has chosen color fill
            case 0:
                final ColorPickerView colorView = new ColorPickerView(mActivity);
                int currentColor = Settings.System.getInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, -1);
                if (currentColor != -1) {
                    colorView.setColor(currentColor);
                }
                colorView.setAlphaSliderVisible(false);
                new AlertDialog.Builder(mActivity)
                .setTitle(R.string.lockscreen_custom_background_dialog_title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_BACKGROUND, colorView.getColor());
                        Settings.System.putInt(getContentResolver(),
                                Settings.System.LOCKSCREEN_BACKGROUND_VALUE, 0);
                        updateCustomBackgroundSummary();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setView(colorView).show();
                return false;
            //Launches intent for user to select an image/crop it to set as background
            case 1:
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("scale", true);
                intent.putExtra("scaleUpIfNeeded", false);
                intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
                Display display = mActivity.getWindowManager().getDefaultDisplay();
                int width = display.getWidth();
                int height = display.getHeight();
                // Lock screen for tablets visible section are different in landscape/portrait,
                // image need to be cropped correctly, like wallpaper setup for scrolling in background in home screen
                // other wise it does not scale correctly
                if (mIsScreenLarge) {
                    width = mActivity.getWallpaperDesiredMinimumWidth();
                    height = mActivity.getWallpaperDesiredMinimumHeight();
                    float spotlightX = (float) display.getWidth() / width;
                    float spotlightY = (float) display.getHeight() / height;
                    intent.putExtra("aspectX", width);
                    intent.putExtra("aspectY", height);
                    intent.putExtra("outputX", width);
                    intent.putExtra("outputY", height);
                    intent.putExtra("spotlightX", spotlightX);
                    intent.putExtra("spotlightY", spotlightY);

                } else {
                    boolean isPortrait = getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_PORTRAIT;
                    intent.putExtra("aspectX", isPortrait ? width : height);
                    intent.putExtra("aspectY", isPortrait ? height : width);
                }
                try {
                    wallpaperTemporary.createNewFile();
                    wallpaperTemporary.setWritable(true, false);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(wallpaperTemporary));
                    intent.putExtra("return-data", false);
                    mActivity.startActivityFromFragment(this, intent, LOCKSCREEN_BACKGROUND);
                } catch (IOException e) {
                } catch (ActivityNotFoundException e) {
                }
                return false;
            //Sets background color to default
            case 2:
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, null);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND_VALUE, indexOf);
                updateCustomBackgroundSummary();
                return false;
            case 3:
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, null);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND_VALUE, indexOf);
                updateCustomBackgroundSummary();
                break;
            }
            return true;
        } else if (preference == mBgAlpha) {
            float val = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALPHA, val / 100);
            return true;
        }
        return false;
    }

    private void updateCustomBackgroundSummary() {
        int resId;
            int customBackground = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_BACKGROUND_VALUE, 3);
        if (customBackground == 3) {
            resId = R.string.lockscreen_background_default_wallpaper;
            mCustomBackground.setValueIndex(3);
            mBgAlpha.setEnabled(false);
        } else if (customBackground == 2) {
            resId = R.string.lockscreen_background_full_transparent;
            mCustomBackground.setValueIndex(2);
            mBgAlpha.setEnabled(false);
        } else if (customBackground == 1) {
            resId = R.string.lockscreen_background_custom_image;
            mCustomBackground.setValueIndex(1);
            mBgAlpha.setEnabled(true);
        } else {
            resId = R.string.lockscreen_background_color_fill;
            mCustomBackground.setValueIndex(0);
            mBgAlpha.setEnabled(true);
        }
        mCustomBackground.setSummary(getResources().getString(resId));
    }

    private void setBatteryStatusSummary() {
        // Set the battery status description text
        if (mBatteryStatus != null) {
            int batteryStatus = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, 0);
            mBatteryStatus.setValueIndex(batteryStatus);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOCKSCREEN_BACKGROUND) {
            if (resultCode == Activity.RESULT_OK) {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.renameTo(wallpaperImage);
                }
                wallpaperImage.setReadOnly();
                Toast.makeText(mActivity, getResources().getString(R.string.
                        lockscreen_background_result_successful), Toast.LENGTH_LONG).show();
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND,"");
                Settings.System.putInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND_VALUE, 1);
                updateCustomBackgroundSummary();
            } else {
                if (wallpaperTemporary.exists()) {
                    wallpaperTemporary.delete();
                }
                Toast.makeText(mActivity, getResources().getString(R.string.
                        lockscreen_background_result_not_successful), Toast.LENGTH_LONG).show();
            }
        }
    }

}
