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

public class PieButtonSecondLayerSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, ShortcutPickerHelper.OnPickListener {

    private static final String TAG = "PieButton";
    private static final String PREF_PIE_QTY = "pie_qty";
    private static final String PREF_PIE_BUTTONS = "pie_buttons_cat";
    private static final String PREF_PIE_ENABLE_LONG = "pie_enable_long";

    public static final int REQUEST_PICK_CUSTOM_ICON = 200;
    public static final int REQUEST_PICK_LANDSCAPE_ICON = 201;

    ListPreference mPieButtonQty;
    CheckBoxPreference mEnablePieLong;

    Preference mPendingPreference;

    Resources mSystemUiResources;

    private ShortcutPickerHelper mPicker;
    private int mPendingIconIndex = -1;
    private PieCustomAction mPendingPieCustomAction = null;

    private File customPieImage;
    private File customPieTemp;

    private boolean mCheckPreferences;

    private static class PieCustomAction {
        String activitySettingName;
        Preference preference;
        int iconIndex = -1;
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
                        Settings.System.PIE_LONG_PRESS_ENABLE_SECOND_LAYER, 0);
                resetPie(5);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetPie (int qnty) {

                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.PIE_BUTTONS_QTY_SECOND_LAYER, qnty);

                if (qnty != 7) {
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[0], "**menu**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[1], "**notifications**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[2], "**search**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[3], "**screenshot**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[4], "**ime**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[5], "**null**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[6], "**null**");
                 } else {
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[0], "**null**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[1], "**menu**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[2], "**notifications**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[3], "**search**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[4], "**screenshot**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[5], "**ime**");
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[6], "**null**");
                 }

                for (int i = 0; i < 7; i++) {
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.PIE_LONGPRESS_ACTIVITIES_SECOND_LAYER[i], null);

                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[i], "");
                }
                refreshSettings();
                setHasOptionsMenu(true);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mEnablePieLong) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_LONG_PRESS_ENABLE_SECOND_LAYER,
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
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PIE_BUTTONS_QTY_SECOND_LAYER, val);
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
                if (longpress) {
                    mPendingPieCustomAction.activitySettingName = Settings.System.PIE_LONGPRESS_ACTIVITIES_SECOND_LAYER[index];
                    mPendingPieCustomAction.iconIndex = -1;
                } else {
                    mPendingPieCustomAction.activitySettingName = Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[index];
                    mPendingPieCustomAction.iconIndex = index;
                }
                mPicker.pickShortcut();
            } else {
                if (longpress) {
                    Settings.System.putString(getContentResolver(),
                            Settings.System.PIE_LONGPRESS_ACTIVITIES_SECOND_LAYER[index],
                            (String) newValue);
                } else {
                    Settings.System.putString(getContentResolver(),
                            Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[index],
                            (String) newValue);
                    Settings.System.putString(getContentResolver(),
                            Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[index], "");
                }
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

                Settings.System.putString(
                        getContentResolver(),
                        Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[mPendingIconIndex], "");
                Settings.System.putString(
                        getContentResolver(),
                        Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[mPendingIconIndex],
                        Uri.fromFile(
                                new File(getActivity().getApplicationContext().getFilesDir(), iconName)).getPath());

                File f = new File(selectedImageUri.getPath());
                if (f.exists())
                    f.delete();

                Toast.makeText(
                        getActivity(),
                        mPendingIconIndex
                                + getResources().getString(
                                        R.string.custom_app_icon_successfully),
                        Toast.LENGTH_LONG).show();
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
        addPreferencesFromResource(R.xml.pie_button_second_layer_settings);

        prefs = getPreferenceScreen();

        mPicker = new ShortcutPickerHelper(this, this);

        customPieImage = new File(getActivity().getFilesDir()+"pie_icon_second_layer" + mPendingIconIndex + ".png");
        customPieTemp = new File(getActivity().getCacheDir()+"/"+"tmp_pie_icon_second_layer" + mPendingIconIndex + ".png");

        mPieButtonQty = (ListPreference) findPreference(PREF_PIE_QTY);
        mPieButtonQty.setOnPreferenceChangeListener(this);
        mPieButtonQty.setValue(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.PIE_BUTTONS_QTY_SECOND_LAYER, 5) + "");

        int pieLong = Settings.System.getInt(mContext.getContentResolver(),
                     Settings.System.PIE_LONG_PRESS_ENABLE_SECOND_LAYER, 0);

        mEnablePieLong = (CheckBoxPreference) findPreference(PREF_PIE_ENABLE_LONG);
        mEnablePieLong.setChecked(pieLong == 1);


        int pieQuantity = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_BUTTONS_QTY_SECOND_LAYER, 5);

        PreferenceGroup targetGroup = (PreferenceGroup) findPreference(PREF_PIE_BUTTONS);
        targetGroup.removeAll();

        PackageManager pm = mContext.getPackageManager();
        Resources res = mContext.getResources();

        for (int i = 0; i < pieQuantity; i++) {
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

            String uri = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[index]);

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

                String uriLong = Settings.System.getString(getActivity().getContentResolver(),
                        Settings.System.PIE_LONGPRESS_ACTIVITIES_SECOND_LAYER[index]);

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

            String customIconUri = Settings.System.getString(getContentResolver(),
                    Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[i]);
            if (customIconUri != null && customIconUri.length() > 0) {
                File f = new File(Uri.parse(customIconUri).getPath());
                if (f.exists())
                    pAction.setIcon(resize(new BitmapDrawable(res, f.getAbsolutePath())));
            } else if (customIconUri != null && !customIconUri.equals("")) {
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
        String uri = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[index]);

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

        if (uri == null)
            return getResources().getDrawable(R.drawable.ic_sysbar_null);

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
            uri = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.PIE_LONGPRESS_ACTIVITIES_SECOND_LAYER[i]);
        else
            uri = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.PIE_CUSTOM_ACTIVITIES_SECOND_LAYER[i]);
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
            else if (uri.equals("**null**"))
                return getResources().getString(R.string.pie_action_none);
        } else {
            return mPicker.getFriendlyNameForUri(uri);
        }
        return null;
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
        if (Settings.System.putString(getActivity().getContentResolver(),
                mPendingPieCustomAction.activitySettingName, uri)) {
            if (mPendingPieCustomAction.iconIndex != -1) {
                if (bmp == null) {
                    Settings.System
                            .putString(
                                    getContentResolver(),
                                    Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[mPendingPieCustomAction.iconIndex],
                                    "");
                } else {
                    String iconName = getIconFileName(mPendingPieCustomAction.iconIndex);
                    FileOutputStream iconStream = null;
                    try {
                        iconStream = mContext.openFileOutput(iconName, Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                        return; // NOOOOO
                    }
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, iconStream);
                    Settings.System
                            .putString(
                                    getContentResolver(),
                                    Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[mPendingPieCustomAction.iconIndex], "");
                    Settings.System
                            .putString(
                                    getContentResolver(),
                                    Settings.System.PIE_CUSTOM_ICONS_SECOND_LAYER[mPendingPieCustomAction.iconIndex],
                                    Uri.fromFile(mContext.getFileStreamPath(iconName)).toString());
                }
            }
            mPendingPieCustomAction.preference.setSummary(friendlyName);
        }
    }

    private String getIconFileName(int index) {
        return "pie_icon_second_layer" + index + ".png";
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSettings();
    }

    public static class PieLayout extends ListFragment {
        private static final String TAG = "PieLayoutSecondLayer";

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
