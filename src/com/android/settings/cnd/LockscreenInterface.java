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

package com.android.settings.cnd;

import java.io.File;
import java.io.IOException;

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
import android.preference.PreferenceGroup;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;
import com.android.settings.widgets.SeekBarPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.ChooseLockSettingsHelper;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import net.margaritov.preference.colorpicker.ColorPickerView;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";
    private static final boolean DEBUG = true;
    private static final int LOCKSCREEN_BACKGROUND = 1024;
    public static final String KEY_CLOCK_PREF = "lockscreen_clock";
    public static final String KEY_WEATHER_PREF = "lockscreen_weather";
    public static final String KEY_CALENDAR_PREF = "lockscreen_calendar";
    public static final String KEY_BACKGROUND_PREF = "lockscreen_background";
    public static final String KEY_WIDGETS_PREF = "lockscreen_widgets";
    private static final String PREF_LOCKSCREEN_TEXT_COLOR = "lockscreen_text_color";
    private static final String KEY_ALWAYS_BATTERY_PREF = "lockscreen_battery_status";
    public static final String KEY_VIBRATE_PREF = "lockscreen_vibrate";
    public static final String KEY_BACKGROUND_ALPHA_PREF = "lockscreen_alpha";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String PREF_BLACKBERRY_LOCK_BG_COLOR = "blackberry_lock_bg_color";
    private static final String PREF_BLACKBERRY_LOCK_BG_ALPHA = "blackberry_lock_bg_alpha";
    private static final String PREF_CIRCLES_LOCK_RING_COLOR = "circles_lock_ring_color";
    private static final String PREF_CIRCLES_LOCK_HALO_COLOR = "circles_lock_halo_color";
    private static final String PREF_CIRCLES_LOCK_WAVE_COLOR = "circles_lock_wave_color";
    private static final String PREF_CIRCLES_LOCK_RING_ALPHA = "circles_lock_ring_alpha";
    private static final String PREF_CIRCLES_LOCK_HALO_ALPHA = "circles_lock_halo_alpha";
    private static final String PREF_CIRCLES_LOCK_WAVE_ALPHA = "circles_lock_wave_alpha";

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private CheckBoxPreference mVibratePref;
    private ListPreference mCustomBackground;
    private ListPreference mWidgetsAlignment;
    private Preference mClockPref;
    private Preference mWeatherPref;
    private Preference mCalendarPref;
    private ColorPickerPreference mLockscreenTextColor;
    private ListPreference mBatteryStatus;
    private PreferenceScreen mLockscreenButtons;
    private Activity mActivity;
    ContentResolver mResolver;
    SeekBarPreference mBgAlpha;
    ColorPickerPreference mBlackBerryLockBgColor;
    SeekBarPreference mBlackBerryBgAlpha;
    ColorPickerPreference mCirclesLockRingColor;
    ColorPickerPreference mCirclesLockHaloColor;
    ColorPickerPreference mCirclesLockWaveColor;
    SeekBarPreference mCirclesRingAlpha;
    SeekBarPreference mCirclesHaloAlpha;
    SeekBarPreference mCirclesWaveAlpha;

    private File wallpaperImage;
    private File wallpaperTemporary;
    private boolean mIsScreenLarge;
    private boolean mCirclesLock;
    private boolean mBlackBerryLock;
    private boolean mStockLock;
    private boolean mTransparent;

    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mResolver = mActivity.getContentResolver();

        mIsScreenLarge = Utils.isTablet(getActivity());

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this.getActivity());
        createCustomLockscreenView();

    }


    private PreferenceScreen createCustomLockscreenView() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);
        root = getPreferenceScreen();

        mCirclesLock = Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.USE_CIRCLES_LOCKSCREEN, false);
        mBlackBerryLock = Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.USE_BLACKBERRY_LOCKSCREEN, false);
        mStockLock = Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.USE_STOCK_LOCKSCREEN, true);
        mTransparent = Settings.System.getBoolean(getActivity().getContentResolver(),
                Settings.System.LOCKSCREEN_TRANSPARENT_ENABLED, false);

        boolean mLockscreenDisabled =  mChooseLockSettingsHelper.utils().isLockScreenDisabled();

        mClockPref = (Preference) findPreference(KEY_CLOCK_PREF);
        mWeatherPref = (Preference) findPreference(KEY_WEATHER_PREF);
        mCalendarPref = (Preference) findPreference(KEY_CALENDAR_PREF);

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

        mWidgetsAlignment = (ListPreference) findPreference(KEY_WIDGETS_PREF);
        mWidgetsAlignment.setOnPreferenceChangeListener(this);
        mWidgetsAlignment.setValue(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.LOCKSCREEN_LAYOUT,
                0) + "");

        mBatteryStatus = (ListPreference) findPreference(KEY_ALWAYS_BATTERY_PREF);
        mBatteryStatus.setOnPreferenceChangeListener(this);
        
        mLockscreenTextColor = (ColorPickerPreference) findPreference(PREF_LOCKSCREEN_TEXT_COLOR);
        mLockscreenTextColor.setOnPreferenceChangeListener(this);

        mBlackBerryLockBgColor = (ColorPickerPreference) findPreference(PREF_BLACKBERRY_LOCK_BG_COLOR);
        mBlackBerryLockBgColor.setOnPreferenceChangeListener(this);

        float blackberrybgAlpha;
        try{
            blackberrybgAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.BLACKBERRY_LOCK_BG_ALPHA);
         }catch (Exception e) {
            blackberrybgAlpha = 0;
            blackberrybgAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.BLACKBERRY_LOCK_BG_ALPHA, 0.15f);
         }
        mBlackBerryBgAlpha = (SeekBarPreference) findPreference(PREF_BLACKBERRY_LOCK_BG_ALPHA);
        mBlackBerryBgAlpha.setInitValue((int) (blackberrybgAlpha * 100));
        mBlackBerryBgAlpha.setProperty(Settings.System.BLACKBERRY_LOCK_BG_ALPHA);
        mBlackBerryBgAlpha.setOnPreferenceChangeListener(this);

        mCirclesLockRingColor = (ColorPickerPreference) findPreference(PREF_CIRCLES_LOCK_RING_COLOR);
        mCirclesLockRingColor.setOnPreferenceChangeListener(this);

        mCirclesLockHaloColor = (ColorPickerPreference) findPreference(PREF_CIRCLES_LOCK_HALO_COLOR);
        mCirclesLockHaloColor.setOnPreferenceChangeListener(this);

        mCirclesLockWaveColor = (ColorPickerPreference) findPreference(PREF_CIRCLES_LOCK_WAVE_COLOR);
        mCirclesLockWaveColor.setOnPreferenceChangeListener(this);

        float ringAlpha;
        try{
            ringAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.CIRCLES_LOCK_RING_ALPHA);
         }catch (Exception e) {
            ringAlpha = 0;
            ringAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.CIRCLES_LOCK_RING_ALPHA, 0.0f);
         }
        mCirclesRingAlpha = (SeekBarPreference) findPreference(PREF_CIRCLES_LOCK_RING_ALPHA);
        mCirclesRingAlpha.setInitValue((int) (ringAlpha * 100));
        mCirclesRingAlpha.setProperty(Settings.System.CIRCLES_LOCK_RING_ALPHA);
        mCirclesRingAlpha.setOnPreferenceChangeListener(this);

        float haloAlpha;
        try{
            haloAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.CIRCLES_LOCK_HALO_ALPHA);
         }catch (Exception e) {
            haloAlpha = 0;
            haloAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.CIRCLES_LOCK_HALO_ALPHA, 0.0f);
         }
        mCirclesHaloAlpha = (SeekBarPreference) findPreference(PREF_CIRCLES_LOCK_HALO_ALPHA);
        mCirclesHaloAlpha.setInitValue((int) (haloAlpha * 100));
        mCirclesHaloAlpha.setProperty(Settings.System.CIRCLES_LOCK_HALO_ALPHA);
        mCirclesHaloAlpha.setOnPreferenceChangeListener(this);

        float waveAlpha;
        try{
            waveAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.CIRCLES_LOCK_WAVE_ALPHA);
         }catch (Exception e) {
            waveAlpha = 0;
            waveAlpha = Settings.System.getFloat(getActivity()
                .getContentResolver(),
                Settings.System.CIRCLES_LOCK_WAVE_ALPHA, 0.35f);
         }
        mCirclesWaveAlpha = (SeekBarPreference) findPreference(PREF_CIRCLES_LOCK_WAVE_ALPHA);
        mCirclesWaveAlpha.setInitValue((int) (waveAlpha * 100));
        mCirclesWaveAlpha.setProperty(Settings.System.CIRCLES_LOCK_WAVE_ALPHA);
        mCirclesWaveAlpha.setOnPreferenceChangeListener(this);

        mVibratePref = (CheckBoxPreference) findPreference(KEY_VIBRATE_PREF);
        boolean bVibrate = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_VIBRATE_ENABLED, 1) == 1 ? true : false;
        mVibratePref.setChecked(bVibrate);
        mVibratePref.setOnPreferenceChangeListener(this);

        Preference mPref;
        mLockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
        if (!hasButtons()) {
             PreferenceCategory mCategoryAddOptions = (PreferenceCategory) findPreference("additional_options");
             mPref = (Preference) findPreference("lockscreen_buttons");
             if (mPref != null) mCategoryAddOptions.removePreference(mPref);

            getPreferenceScreen().removePreference(mLockscreenButtons);
        }

        //setup custom lockscreen customize view
        if (mCirclesLock) {
             PreferenceCategory stockCategory = (PreferenceCategory) findPreference("stock_lockscreen");
             getPreferenceScreen().removePreference(stockCategory);

             PreferenceCategory blackberryColorCategory = (PreferenceCategory) findPreference("blackberry_lockscreen");
             getPreferenceScreen().removePreference(blackberryColorCategory);

        }else if (mBlackBerryLock) {
             PreferenceCategory stockCategory = (PreferenceCategory) findPreference("stock_lockscreen");
             getPreferenceScreen().removePreference(stockCategory);

             PreferenceCategory circlesColorCategory = (PreferenceCategory) findPreference("circles_lockscreen");
             getPreferenceScreen().removePreference(circlesColorCategory);

        }else if (mStockLock) {
             PreferenceCategory blackberryColorCategory = (PreferenceCategory) findPreference("blackberry_lockscreen");
             getPreferenceScreen().removePreference(blackberryColorCategory);

             PreferenceCategory circlesColorCategory = (PreferenceCategory) findPreference("circles_lockscreen");
             getPreferenceScreen().removePreference(circlesColorCategory);

        }else if (mLockscreenDisabled) {
             PreferenceCategory styleCategory = (PreferenceCategory) findPreference("lockscreen_style_options");
             getPreferenceScreen().removePreference(styleCategory);

             PreferenceCategory stockCategory = (PreferenceCategory) findPreference("stock_lockscreen");
             getPreferenceScreen().removePreference(stockCategory);

             PreferenceCategory blackberryColorCategory = (PreferenceCategory) findPreference("blackberry_lockscreen");
             getPreferenceScreen().removePreference(blackberryColorCategory);

             PreferenceCategory circlesColorCategory = (PreferenceCategory) findPreference("circles_lockscreen");
             getPreferenceScreen().removePreference(circlesColorCategory);

             PreferenceCategory mCategoryInterf = (PreferenceCategory) findPreference("lockscreen_interface_options");
             getPreferenceScreen().removePreference(mCategoryInterf);

             PreferenceCategory mCategoryAddOptions = (PreferenceCategory) findPreference("additional_options");
             mPref = (Preference) findPreference("lockscreen_buttons");
             if (mPref != null) mCategoryAddOptions.removePreference(mPref);
             mPref = (Preference) findPreference("lockscreen_vibrate");
             if (mPref != null) mCategoryAddOptions.removePreference(mPref);

        }else {
             PreferenceCategory stockCategory = (PreferenceCategory) findPreference("stock_lockscreen");
             getPreferenceScreen().removePreference(stockCategory);

             PreferenceCategory blackberryColorCategory = (PreferenceCategory) findPreference("blackberry_lockscreen");
             getPreferenceScreen().removePreference(blackberryColorCategory);

             PreferenceCategory circlesColorCategory = (PreferenceCategory) findPreference("circles_lockscreen");
             getPreferenceScreen().removePreference(circlesColorCategory);

             PreferenceCategory mCategoryInterfOptions = (PreferenceCategory) findPreference("lockscreen_interface_options");
             mPref = (Preference) findPreference("lockscreen_weather");
             if (mPref != null) mCategoryInterfOptions.removePreference(mPref);
             mPref = (Preference) findPreference("lockscreen_calendar");
             if (mPref != null) mCategoryInterfOptions.removePreference(mPref);

             PreferenceCategory mCategoryAddOptions = (PreferenceCategory) findPreference("additional_options");
             mPref = (Preference) findPreference("lockscreen_buttons");
             if (mPref != null) mCategoryAddOptions.removePreference(mPref);
             mPref = (Preference) findPreference("lockscreen_vibrate");
             if (mPref != null) mCategoryAddOptions.removePreference(mPref);
         }

        updateCustomBackgroundSummary();
        return root;
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

    @Override
    public void onResume() {
        super.onResume();
        createCustomLockscreenView();
        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateState() {
        int resId;

        // Set the clock description text
        if (mClockPref != null) {
            boolean clockEnabled = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_CLOCK, 1) == 1;
            if (clockEnabled) {
                mClockPref.setSummary(R.string.lockscreen_clock_enabled);
            } else {
                mClockPref.setSummary(R.string.lockscreen_clock_summary);
            }
        }

        // Set the weather description text
        if (mWeatherPref != null) {
            boolean weatherEnabled = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_WEATHER, 0) == 1;
            if (weatherEnabled) {
                mWeatherPref.setSummary(R.string.lockscreen_weather_enabled);
            } else {
                mWeatherPref.setSummary(R.string.lockscreen_weather_summary);
            }
        }

        // Set the calendar description text
        if (mCalendarPref != null) {
            boolean weatherEnabled = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_CALENDAR, 0) == 1;
            if (weatherEnabled) {
                mCalendarPref.setSummary(R.string.lockscreen_calendar_enabled);
            } else {
                mCalendarPref.setSummary(R.string.lockscreen_calendar_summary);
            }
        }

        // Set the calendar description text
        if (mBatteryStatus != null) {
            boolean batteryStatusAlwaysOn = Settings.System.getInt(mResolver,
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, 0) == 1;
            if (batteryStatusAlwaysOn) {
                mBatteryStatus.setValueIndex(1);
            } else {
                mBatteryStatus.setValueIndex(0);
            }
            mBatteryStatus.setSummary(mBatteryStatus.getEntry());
            //mCustomBackground.setSummary(getResources().getString(resId));
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        boolean handled = false;
        if (preference == mCustomBackground) {
            int indexOf = mCustomBackground.findIndexOfValue(objValue.toString());
            Settings.System.putBoolean(getContentResolver(),
                    Settings.System.LOCKSCREEN_TRANSPARENT_ENABLED, true);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BACKGROUND_VALUE, indexOf);
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
                Rect rect = new Rect();
                Window window = mActivity.getWindow();
                window.getDecorView().getWindowVisibleDisplayFrame(rect);
                int statusBarHeight = rect.top;
                int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
                int titleBarHeight = contentViewTop - statusBarHeight;
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
                    intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
                    intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
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
                updateCustomBackgroundSummary();
                return false;
            case 3:
                Settings.System.putString(getContentResolver(),
                        Settings.System.LOCKSCREEN_BACKGROUND, null);
                Settings.System.putBoolean(getContentResolver(),
                        Settings.System.LOCKSCREEN_TRANSPARENT_ENABLED, false);
                updateCustomBackgroundSummary();
                break;
            }
            return true;
        } else if (preference == mBgAlpha) {
            float val = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALPHA, val / 100);
            return true;
        } else if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mVibratePref) {
            boolean bValue = Boolean.valueOf((Boolean) objValue);
            int value = 0;
            if (bValue) {
                value = 1;
            }
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.LOCKSCREEN_VIBRATE_ENABLED, value);
            return true;
        } else if (preference == mLockscreenTextColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.LOCKSCREEN_CUSTOM_TEXT_COLOR, intHex);
            if (DEBUG) Log.d(TAG, String.format("new color hex value: %d", intHex));
            return true;
        } else if (preference == mWidgetsAlignment) {
            int value = Integer.valueOf((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                                    Settings.System.LOCKSCREEN_LAYOUT, value);
            return true;
        } else if (preference == mBlackBerryLockBgColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.BLACKBERRY_LOCK_BG_COLOR, intHex);
            return true;
        } else if (preference == mBlackBerryBgAlpha) {
            float val = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.BLACKBERRY_LOCK_BG_ALPHA, val / 100);
            return true;
        } else if (preference == mCirclesLockRingColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CIRCLES_LOCK_RING_COLOR, intHex);
            return true;
        } else if (preference == mCirclesLockHaloColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CIRCLES_LOCK_HALO_COLOR, intHex);
            return true;
        } else if (preference == mCirclesLockWaveColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.CIRCLES_LOCK_WAVE_COLOR, intHex);
            return true;
        } else if (preference == mCirclesRingAlpha) {
            float val = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.CIRCLES_LOCK_RING_ALPHA, val / 100);
            return true;
        } else if (preference == mCirclesHaloAlpha) {
            float val = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.CIRCLES_LOCK_HALO_ALPHA, val / 100);
            return true;
        } else if (preference == mCirclesWaveAlpha) {
            float val = Float.parseFloat((String) objValue);
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.CIRCLES_LOCK_WAVE_ALPHA, val / 100);
            return true;
        }
        return false;
    }

}
