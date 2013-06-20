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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.net.URISyntaxException;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.ShortcutPickerHelper;
import com.android.settings.widget.NavBarItemPreference;

public class PieButtonSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, ShortcutPickerHelper.OnPickListener {

    private static final String TAG = "PieButton";
    private static final String PREF_PIE_QTY = "pie_qty";
    private static final String PREF_PIE_BUTTONS = "pie_buttons_cat";
    private static final String PREF_PIE_ENABLE_LONG = "pie_enable_long";

    public static final int REQUEST_PICK_CUSTOM_ICON = 200;
    public static final int REQUEST_PICK_LANDSCAPE_ICON = 201;

    private String[] mClickActions = new String[5];
    private String[] mLongpressActions = new String[5];
    private String[] mPortraitIcons = new String[5];

    private final static String
            mPieConfigDefault = "**back**|**null**|empty|"
                              + "**home**|**null**|empty|"
                              + "**recents**|**null**|empty";
    ListPreference mPieButtonQty;
    CheckBoxPreference mEnablePieLong;

    Preference mPendingPreference;
    Resources mSystemUiResources;

    private ShortcutPickerHelper mPicker;
    private int mPendingIconIndex = -1;
    private PieCustomAction mPendingPieCustomAction = null;
    private int mPieQnty;

    private File customPieImage;
    private File customPieTemp;

    private boolean mCheckPreferences;

    private static class PieCustomAction {
        String activitySettingName;
        Preference preference;
        int index;
        boolean longpress = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        refreshSettings();
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.pie_button, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.PIE_LONG_PRESS_ENABLE, 0);
                resetPie(3);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetPie (int qnty) {
       if (qnty != 5) {
            mClickActions[0] = "**back**";
            mClickActions[1] = "**home**";
            mClickActions[2] = "**recents**";
            mClickActions[3] = "**null**";
            mClickActions[4] = "**null**";
        } else {
            mClickActions[0] = "**null**";
            mClickActions[1] = "**back**";
            mClickActions[2] = "**home**";
            mClickActions[3] = "**recents**";
            mClickActions[4] = "**null**";
        }

        for (int i = 0; i < 5; i++) {
            mLongpressActions[i] = "**null**";
            mPortraitIcons[i] = "empty";
        }
        mPieQnty = qnty;
        setPieConfig();
        refreshSettings();
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mEnablePieLong) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_LONG_PRESS_ENABLE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);

            refreshSettings();
            setHasOptionsMenu(true);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mPieButtonQty) {
            int val = Integer.parseInt((String) newValue);
            resetPie(val);
            refreshSettings();
            return true;
        } else if ((preference.getKey().startsWith("pie_action"))
                || (preference.getKey().startsWith("pie_longpress"))) {
            boolean longpress = preference.getKey().startsWith("pie_longpress_");
            int index = Integer.parseInt(preference.getKey().substring(
                    preference.getKey().lastIndexOf("_") + 1));

            if (newValue.equals("**app**")) {
                mPendingPieCustomAction = new PieCustomAction();
                mPendingPieCustomAction.preference = preference;
                mPendingPieCustomAction.index = index;
                if (longpress) {
                    mPendingPieCustomAction.activitySettingName = mLongpressActions[index];
                    mPendingPieCustomAction.longpress = true;
                } else {
                    mPendingPieCustomAction.activitySettingName = mClickActions[index];
                }
                mPicker.pickShortcut();
            } else {
                if (longpress) {
                    mLongpressActions[index] = (String) newValue;
                } else {
                    mClickActions[index] = (String) newValue;
                    mPortraitIcons[index] = "empty";
                }
                setPieConfig();
            }
            refreshSettings();
            return true;
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            } else if ((requestCode == REQUEST_PICK_CUSTOM_ICON)
                    || (requestCode == REQUEST_PICK_LANDSCAPE_ICON)) {

                String iconName = getIconFileName(mPendingIconIndex);
                FileOutputStream iconStream = null;
                try {
                    iconStream = getActivity().getApplicationContext().openFileOutput(iconName, Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    return; // NOOOOO
                }

                if (customPieTemp.exists()) {
                    customPieTemp.renameTo(customPieImage);
                }

                Uri selectedImageUri = Uri.fromFile(customPieImage);
                Log.e(TAG, "Selected image path: " + selectedImageUri.getPath());
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath());
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, iconStream);

                mPortraitIcons[mPendingIconIndex] =
                        Uri.fromFile(
                                new File(getActivity().getApplicationContext().getFilesDir(), iconName)).getPath();

                File f = new File(selectedImageUri.getPath());
                if (f.exists())
                    f.delete();

                Toast.makeText(
                        getActivity(),
                        mPendingIconIndex
                                + getResources().getString(
                                        R.string.custom_app_icon_successfully),
                        Toast.LENGTH_LONG).show();
                setPieConfig();
                refreshSettings();
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pie_button_settings);

        prefs = getPreferenceScreen();

        mPicker = new ShortcutPickerHelper(this, this);

        customPieImage = new File(getActivity().getFilesDir()+"pie_icon_" + mPendingIconIndex + ".png");
        customPieTemp = new File(getActivity().getCacheDir()+"/"+"tmp_pie_icon_" + mPendingIconIndex + ".png");

        getPieConfig();

        mPieButtonQty = (ListPreference) findPreference(PREF_PIE_QTY);
        mPieButtonQty.setOnPreferenceChangeListener(this);
        mPieButtonQty.setValue(mPieQnty + "");

        int pieLong = Settings.System.getInt(mContext.getContentResolver(),
                     Settings.System.PIE_LONG_PRESS_ENABLE, 0);

        mEnablePieLong = (CheckBoxPreference) findPreference(PREF_PIE_ENABLE_LONG);
        mEnablePieLong.setChecked(pieLong == 1);

        PreferenceGroup targetGroup = (PreferenceGroup) findPreference(PREF_PIE_BUTTONS);
        targetGroup.removeAll();

        PackageManager pm = mContext.getPackageManager();
        Resources res = mContext.getResources();

        for (int i = 0; i < mPieQnty; i++) {
            final int index = i;
            //we reuse the NavBarItemPreference class here
            NavBarItemPreference pAction = new NavBarItemPreference(getActivity());
            String dialogTitle = String.format(
                    getResources().getString(R.string.pie_action_title), i + 1);
            pAction.setDialogTitle(dialogTitle);
            pAction.setEntries(R.array.pie_button_entries);
            pAction.setEntryValues(R.array.pie_button_values);
            pAction.setTitle(dialogTitle);
            pAction.setKey("pie_action_" + i);
            pAction.setSummary(getProperSummary(i, false));
            pAction.setOnPreferenceChangeListener(this);
            targetGroup.addPreference(pAction);

            String uri = mClickActions[index];

            if (uri == null) {
                pAction.setValue("**null**");
            } else if (uri.startsWith("**")) {
                pAction.setValue(uri);
            } else {
                pAction.setValue("**app**");
            }

            if (pieLong == 1) {
                ListPreference mLongPress = new ListPreference(getActivity());
                dialogTitle = String.format(
                        getResources().getString(R.string.pie_longpress_title), i + 1);
                mLongPress.setDialogTitle(dialogTitle);
                mLongPress.setEntries(R.array.pie_button_entries);
                mLongPress.setEntryValues(R.array.pie_button_values);
                mLongPress.setTitle(dialogTitle);
                mLongPress.setKey("pie_longpress_" + i);
                mLongPress.setSummary(getProperSummary(i, true));
                mLongPress.setOnPreferenceChangeListener(this);
                targetGroup.addPreference(mLongPress);

                String uriLong = mLongpressActions[index];

               if (uriLong == null) {
                    mLongPress.setValue("**null**");
               } else if (uriLong.startsWith("**")) {
                    mLongPress.setValue(uriLong);
               } else {
                    mLongPress.setValue("**app**");
               }
            }

            if (uri != null && !uri.equals("**null**")) {
             pAction.setImageListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPendingIconIndex = index;
                        int width = 100;
                        int height = width;
                        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/*");
                        intent.putExtra("crop", "true");
                        intent.putExtra("scale", true);
                        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
                        intent.putExtra("aspectX", width);
                        intent.putExtra("aspectY", height);
                        intent.putExtra("outputX", width);
                        intent.putExtra("outputY", height);
                    try {
                        customPieTemp.createNewFile();
                        customPieTemp.setWritable(true, false);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(customPieTemp));
                        intent.putExtra("return-data", false);
                        startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON);
                    } catch (IOException e) {
                    } catch (ActivityNotFoundException e) {
                    }
                    }
                });
            }

            String customIconUri = mPortraitIcons[i];
            if (customIconUri != null && !customIconUri.equals("empty")) {
                File f = new File(Uri.parse(customIconUri).getPath());
                if (f.exists())
                    pAction.setIcon(resize(new BitmapDrawable(res, f.getAbsolutePath())));
            } else if (customIconUri != null && uri.equals("**app**")) {
                // here they chose another app icon
                try {
                    pAction.setIcon(resize(pm.getActivityIcon(Intent.parseUri(uri, 0))));
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            } else {
                // ok use default icons here
                pAction.setIcon(resize(getPieIconImage(i, false)));
            }
        }
        mCheckPreferences = true;
        return prefs;
    }

    private Drawable resize(Drawable image) {
        int size = 50;
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, getResources()
                .getDisplayMetrics());

        Bitmap d = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, px, px, false);
        return new BitmapDrawable(mContext.getResources(), bitmapOrig);
    }

    private Drawable getPieIconImage(int index, boolean landscape) {
        String uri =  mClickActions[index];

        int resId = 0;
        PackageManager pm = mContext.getPackageManager();

        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
                Log.e("PieButtons:", "can't access systemui resources",e);
            }
        }

        if (uri.startsWith("**")) {
            if (uri.equals("**home**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_home", null, null);
            } else if (uri.equals("**back**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_back", null, null);
            } else if (uri.equals("**recents**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_recent", null, null);
            } else if (uri.equals("**search**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_search", null, null);
            } else if (uri.equals("**screenshot**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_screenshot", null, null);
            } else if (uri.equals("**menu**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_menu_big", null, null);
             } else if (uri.equals("**ime**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_ime_switcher", null, null);
            } else if (uri.equals("**kill**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_killtask", null, null);
            } else if (uri.equals("**widgets**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_widget", null, null);
            } else if (uri.equals("**power**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_power", null, null);
            } else if (uri.equals("**notifications**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_notifications", null, null);
            } else if (uri.equals("**lastapp**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_lastapp", null, null);
            } else if (uri.equals("**quicksettings**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_qs", null, null);
            }
        } else {
            try {
                return mContext.getPackageManager().getActivityIcon(Intent.parseUri(uri, 0));
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (resId > 0) {
            try {
                return mSystemUiResources.getDrawable(resId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return getResources().getDrawable(R.drawable.ic_sysbar_null);
    }

    private String getProperSummary(int i, boolean longpress) {
        String uri = "";
        if (longpress)
            uri = mLongpressActions[i];
        else
            uri = mClickActions[i];

        if (uri == null)
            return getResources().getString(R.string.pie_action_none);

        if (uri.startsWith("**")) {
            if (uri.equals("**home**"))
                return getResources().getString(R.string.pie_action_home);
            else if (uri.equals("**back**"))
                return getResources().getString(R.string.pie_action_back);
            else if (uri.equals("**recents**"))
                return getResources().getString(R.string.pie_action_recents);
            else if (uri.equals("**search**"))
                return getResources().getString(R.string.pie_action_search);
            else if (uri.equals("**screenshot**"))
                return getResources().getString(R.string.pie_action_screenshot);
            else if (uri.equals("**menu**"))
                return getResources().getString(R.string.pie_action_menu);
            else if (uri.equals("**ime**"))
                return getResources().getString(R.string.pie_action_ime);
            else if (uri.equals("**kill**"))
                return getResources().getString(R.string.pie_action_kill);
            else if (uri.equals("**widgets**"))
                return getResources().getString(R.string.pie_action_widgets);
            else if (uri.equals("**power**"))
                return getResources().getString(R.string.pie_action_power);
            else if (uri.equals("**notifications**"))
                return getResources().getString(R.string.pie_action_notifications);
            else if (uri.equals("**lastapp**"))
                return getResources().getString(R.string.pie_action_lastapp);
            else if (uri.equals("**quicksettings**"))
                return getResources().getString(R.string.pie_action_quicksettings);
            else if (uri.equals("**null**"))
                return getResources().getString(R.string.pie_action_none);
        } else {
            return mPicker.getFriendlyNameForUri(uri);
        }
        return null;
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
        if (!mPendingPieCustomAction.longpress) {
            if (bmp == null) {
                mPortraitIcons[mPendingPieCustomAction.index] = "empty";
            } else {
                String iconName = getIconFileName(mPendingPieCustomAction.index);
                FileOutputStream iconStream = null;
                try {
                    iconStream = mContext.openFileOutput(iconName, Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    return; // NOOOOO
                }
                bmp.compress(Bitmap.CompressFormat.PNG, 100, iconStream);
                mPortraitIcons[mPendingPieCustomAction.index] =
                                Uri.fromFile(mContext.getFileStreamPath(iconName)).toString();
            }
            mClickActions[mPendingPieCustomAction.index] = uri;
        } else {
            mLongpressActions[mPendingPieCustomAction.index] = uri;
        }

        setPieConfig();
        mPendingPieCustomAction.preference.setSummary(friendlyName);
    }

    private String getIconFileName(int index) {
        return "pie_icon_" + index + ".png";
    }

    private void getPieConfig() {
        // init vars to fill with them later the pie config values
        int counter = 0;
        int buttonNumber = 0;
        String pieConfig = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTONS_CONFIG);

        if (pieConfig == null) {
            pieConfig = mPieConfigDefault;
        }

        // Split out the navbar config to work with and add to the list
        for (String configValue : pieConfig.split("\\|")) {
            counter++;
            if (counter == 1) {
                mClickActions[buttonNumber] = configValue;
            }
            if (counter == 2) {
                mLongpressActions[buttonNumber] = configValue;
            }
            if (counter == 3) {
                mPortraitIcons[buttonNumber] = configValue;
                buttonNumber++;
                //reset counter due that iteration of one button is finished
                counter = 0;
            }
        }

        // set overall counted number off buttons
        mPieQnty = buttonNumber;
    }

    private void setPieConfig() {
        String finalPieConfig = "";

        for (int i = 0; i < mPieQnty; i++) {
            if (i != 0) {
                finalPieConfig += "|";
            }
            finalPieConfig += mClickActions[i] + "|"
                               + mLongpressActions[i] + "|"
                               + mPortraitIcons[i];
        }
        Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTONS_CONFIG, finalPieConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSettings();
    }

    public static class PieLayout extends ListFragment {
        private static final String TAG = "PieLayout";

        Context mContext;

        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);

            mContext = getActivity().getBaseContext();
        }

        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        };

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onResume() {
            super.onResume();
        }
    }
}
