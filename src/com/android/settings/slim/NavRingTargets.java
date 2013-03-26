/*
 * Copyright (C) 2012 Slimroms
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

public class NavRingTargets extends SettingsPreferenceFragment implements
        ShortcutPickerHelper.OnPickListener, OnPreferenceChangeListener {

    private static final String TAG = "NavRingTargets";

    private static final String PREF_NAVRING_AMOUNT = "pref_navring_amount";
    private static final String ENABLE_NAVRING_LONG = "enable_navring_long";

    public static final int REQUEST_PICK_CUSTOM_ICON = 200;
    public static final int REQUEST_PICK_LANDSCAPE_ICON = 201;

    private ShortcutPickerHelper mPicker;
    private Preference mPreference;
    private String mString;
    private int mIconIndex = -1;
    private NavRingCustomAction mNavRingCustomAction = null;

    private int mNavRingAmount;
    private int mNavRingLong;

    private File customnavImage;
    private File customnavTemp;

    private boolean mCheckPreferences;

    CheckBoxPreference mEnableNavringLong;
    ListPreference mNavRingButtonQty;

    Resources mSystemUiResources;

    private static class NavRingCustomAction {
        String activitySettingName;
        Preference preference;
        int iconIndex = -1;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createCustomView();
    }

    private PreferenceScreen createCustomView() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.navring_settings);

        prefs = getPreferenceScreen();

        mPicker = new ShortcutPickerHelper(this, this);

        customnavImage = new File(getActivity().getFilesDir()+"navring_icon_" + mIconIndex + ".png");
        customnavTemp = new File(getActivity().getCacheDir()+"/"+"tmp_nvr_icon_" + mIconIndex + ".png");

        mNavRingButtonQty = (ListPreference) findPreference(PREF_NAVRING_AMOUNT);
        mNavRingButtonQty.setOnPreferenceChangeListener(this);
        mNavRingButtonQty.setValue(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEMUI_NAVRING_AMOUNT, 1) + "");

        mEnableNavringLong = (CheckBoxPreference) findPreference("enable_navring_long");
        mEnableNavringLong.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SYSTEMUI_NAVRING_LONG_ENABLE, 0) == 1);

        String target = Settings.System.getString(mContext.getContentResolver(), Settings.System.SYSTEMUI_NAVRING[0]);
        if (target == null || target.equals("")) {
            Settings.System.putString(mContext.getContentResolver(), Settings.System.SYSTEMUI_NAVRING[0], "**assist**");
        }

        int navringQuantity = Settings.System.getInt(getContentResolver(),
                Settings.System.SYSTEMUI_NAVRING_AMOUNT, 1);

        PreferenceGroup targetGroup = (PreferenceGroup) findPreference("targets_cat");
        targetGroup.removeAll();

        PackageManager pm = mContext.getPackageManager();
        Resources res = mContext.getResources();

        for (int i = 0; i < navringQuantity; i++) {
            final int index = i;
            NavBarItemPreference pAction = new NavBarItemPreference(getActivity());
            String dialogTitle = String.format(
                    getResources().getString(R.string.interface_softkeys_pref_default_title), i + 1);
            pAction.setDialogTitle(dialogTitle);
            pAction.setEntries(R.array.navring_dialog_entries);
            pAction.setEntryValues(R.array.navring_dialog_values);
            String title = String.format(getResources().getString(R.string.interface_softkeys_pref_default_title),
                    i + 1);
            pAction.setTitle(title);
            pAction.setKey("interface_navring_release_" + i);
            pAction.setSummary(getProperSummary(i, false));
            pAction.setIcon(resize(getNavRingIconImage(i, false)));
            pAction.setOnPreferenceChangeListener(this);
            targetGroup.addPreference(pAction);

            String uri = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING[index]);

            if (uri == null) {
                pAction.setValue("**null**");
            } else if (uri.startsWith("**")) {
                pAction.setValue(uri);
            } else {
                pAction.setValue("**app**");
            }

            int mNavRingLong = Settings.System.getInt(mContext.getContentResolver(),
                         Settings.System.SYSTEMUI_NAVRING_LONG_ENABLE, 0);

            if (mNavRingLong == 1) {
                ListPreference mLongPress = new ListPreference(getActivity());
                dialogTitle = String.format(
                        getResources().getString(R.string.interface_softkeys_pref_long_title), i + 1);
                mLongPress.setDialogTitle(dialogTitle);
                mLongPress.setEntries(R.array.navring_dialog_entries);
                mLongPress.setEntryValues(R.array.navring_dialog_values);
                mLongPress.setTitle(dialogTitle);
                mLongPress.setKey("interface_navring_long_" + i);
                mLongPress.setSummary(getProperSummary(i, true));
                mLongPress.setOnPreferenceChangeListener(this);
                targetGroup.addPreference(mLongPress);

                String uriLong = Settings.System.getString(getActivity().getContentResolver(),
                        Settings.System.SYSTEMUI_NAVRING_LONG[index]);

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
                        mIconIndex = index;
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
                        customnavTemp.createNewFile();
                        customnavTemp.setWritable(true, false);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(customnavTemp));
                        intent.putExtra("return-data", false);
                        startActivityForResult(intent, REQUEST_PICK_CUSTOM_ICON);
                    } catch (IOException e) {
                    } catch (ActivityNotFoundException e) {
                    }
                    }
                });
            }

            String customIconUri = Settings.System.getString(getContentResolver(),
                    Settings.System.NAVRING_CUSTOM_APP_ICONS[i]);
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
                pAction.setIcon(resize(getNavRingIconImage(i, false)));
            }

        }
        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.nav_ring_targets, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                resetNavRing();
                resetNavRingLong();

                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.SYSTEMUI_NAVRING_AMOUNT, 1);
                Settings.System.putString(getContentResolver(),
                       Settings.System.SYSTEMUI_NAVRING[0], (String) "");
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.SYSTEMUI_NAVRING_LONG_ENABLE, 0);

                createCustomView();

             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        createCustomView();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mEnableNavringLong) {

            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING_LONG_ENABLE,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            createCustomView();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mNavRingButtonQty) {
            int val = Integer.parseInt((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING_AMOUNT, val);
            resetNavRing();
            resetNavRingLong();
            createCustomView();
            return true;
        } else if ((preference.getKey().startsWith("interface_navring_release"))
                || (preference.getKey().startsWith("interface_navring_long"))) {
            boolean longpress = preference.getKey().startsWith("interface_navring_long_");
            int index = Integer.parseInt(preference.getKey().substring(
                    preference.getKey().lastIndexOf("_") + 1));

            if (newValue.equals("**app**")) {
                mNavRingCustomAction = new NavRingCustomAction();
                mNavRingCustomAction.preference = preference;
                if (longpress) {
                    mNavRingCustomAction.activitySettingName = Settings.System.SYSTEMUI_NAVRING_LONG[index];
                    mNavRingCustomAction.iconIndex = -1;
                } else {
                    mNavRingCustomAction.activitySettingName = Settings.System.SYSTEMUI_NAVRING[index];
                    mNavRingCustomAction.iconIndex = index;
                }
                mPicker.pickShortcut();
            } else {
                if (longpress) {
                    Settings.System.putString(getContentResolver(),
                            Settings.System.SYSTEMUI_NAVRING_LONG[index],
                            (String) newValue);
                } else {
                    Settings.System.putString(getContentResolver(),
                            Settings.System.SYSTEMUI_NAVRING[index],
                            (String) newValue);
                    Settings.System.putString(getContentResolver(),
                            Settings.System.NAVRING_CUSTOM_APP_ICONS[index], "");
                }
            }
            createCustomView();
            return true;
        }
        return false;
    }

    public void shortcutPicked(String uri, String friendlyName, Bitmap bmp, boolean isApplication) {
        if (Settings.System.putString(getActivity().getContentResolver(),
                mNavRingCustomAction.activitySettingName, uri)) {
            if (mNavRingCustomAction.iconIndex != -1) {
                if (bmp == null) {
                    Settings.System
                            .putString(
                                    getContentResolver(),
                                    Settings.System.NAVRING_CUSTOM_APP_ICONS[mNavRingCustomAction.iconIndex],
                                    "");
                } else {
                    String iconName = getIconFileName(mNavRingCustomAction.iconIndex);
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
                                    Settings.System.NAVRING_CUSTOM_APP_ICONS[mNavRingCustomAction.iconIndex], "");
                    Settings.System
                            .putString(
                                    getContentResolver(),
                                    Settings.System.NAVRING_CUSTOM_APP_ICONS[mNavRingCustomAction.iconIndex],
                                    Uri.fromFile(mContext.getFileStreamPath(iconName)).toString());
                }
            }
        }
    }

    private String getIconFileName(int index) {
        return "navring_icon_" + index + ".png";
    }

    private String getProperSummary(int i, boolean longpress) {
        String uri = "";
        if (longpress)
            uri = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING_LONG[i]);
        else
            uri = Settings.System.getString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING[i]);

        if (uri == null)
                return getResources().getString(R.string.none);

        if (uri.startsWith("**")) {
            if (uri.equals("**null**"))
                    return getResources().getString(R.string.none);
            else if (uri.equals("**screenshot**"))
                    return getResources().getString(R.string.take_screenshot);
            else if (uri.equals("**ime**"))
                    return getResources().getString(R.string.open_ime_switcher);
            else if (uri.equals("**ring_vib**"))
                    return getResources().getString(R.string.ring_vib);
            else if (uri.equals("**ring_silent**"))
                    return getResources().getString(R.string.ring_silent);
            else if (uri.equals("**ring_vib_silent**"))
                    return getResources().getString(R.string.ring_vib_silent);
            else if (uri.equals("**kill**"))
                    return getResources().getString(R.string.kill_app);
            else if (uri.equals("**widgets**"))
                    return getResources().getString(R.string.widgets);
            else if (uri.equals("**lastapp**"))
                    return getResources().getString(R.string.lastapp);
            else if (uri.equals("**screenoff**"))
                    return getResources().getString(R.string.screen_off);
            else if (uri.equals("**power**"))
                    return getResources().getString(R.string.power);
            else if (uri.equals("**assist**"))
                    return getResources().getString(R.string.google_now);
        } else {
                return mPicker.getFriendlyNameForUri(uri);
        }
        return null;
    }

    private Drawable getNavRingIconImage(int index, boolean landscape) {
        String uri = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.SYSTEMUI_NAVRING[index]);

        int resId = 0;
        PackageManager pm = mContext.getPackageManager();

        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
                Log.e("NavRing targets", "can't access systemui resources",e);
            }
        }

        if (uri == null)
            return getResources().getDrawable(R.drawable.ic_sysbar_null);

        if (uri.equals("**null**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_null", null, null);
        } else if (uri.equals("**screenshot**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_screenshot", null, null);
        } else if (uri.equals("**ime**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_sysbar_ime_switcher", null, null);
        } else if (uri.equals("**ring_vib**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_vib", null, null);
        } else if (uri.equals("**ring_silent**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_silent", null, null);
        } else if (uri.equals("**ring_vib_silent**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_ring_vib_silent", null, null);
        } else if (uri.equals("**kill**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_killtask", null, null);
        } else if (uri.equals("**widgets**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_widgets", null, null);
        } else if (uri.equals("**lastapp**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_lastapp", null, null);
        } else if (uri.equals("**screenoff**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_power", null, null);
        } else if (uri.equals("**power**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_power", null, null);
        } else if (uri.equals("**assist**")) {
                resId = mSystemUiResources.getIdentifier("com.android.systemui:drawable/ic_navbar_googlenow", null, null);
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

    private Drawable resize(Drawable image) {
        int size = 50;
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, getResources()
                .getDisplayMetrics());

        Bitmap d = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapOrig = Bitmap.createScaledBitmap(d, px, px, false);
        return new BitmapDrawable(mContext.getResources(), bitmapOrig);
    }

    public void resetNavRing() {
            for (int i = 0; i < 5; i++) {
               Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING[i], "**null**");

                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.NAVRING_CUSTOM_APP_ICONS[i], "");
            }
    }

    public void resetNavRingLong() {
            for (int i = 0; i < 5; i++) {
               Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.SYSTEMUI_NAVRING_LONG[i], "**null**");
            }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            } else if ((requestCode == REQUEST_PICK_CUSTOM_ICON)
                    || (requestCode == REQUEST_PICK_LANDSCAPE_ICON)) {

                String iconName = getIconFileName(mIconIndex);
                FileOutputStream iconStream = null;
                try {
                    iconStream = getActivity().getApplicationContext().openFileOutput(iconName, Context.MODE_WORLD_READABLE);
                } catch (FileNotFoundException e) {
                    return; // NOOOOO
                }

                if (customnavTemp.exists()) {
                    customnavTemp.renameTo(customnavImage);
                }

                Uri selectedImageUri = Uri.fromFile(customnavImage);
                Log.e(TAG, "Selected image path: " + selectedImageUri.getPath());
                Bitmap bitmap = BitmapFactory.decodeFile(selectedImageUri.getPath());
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, iconStream);

                Settings.System.putString(
                        getContentResolver(),
                        Settings.System.NAVRING_CUSTOM_APP_ICONS[mIconIndex], "");
                Settings.System.putString(
                        getContentResolver(),
                        Settings.System.NAVRING_CUSTOM_APP_ICONS[mIconIndex],
                        Uri.fromFile(
                                new File(getActivity().getApplicationContext().getFilesDir(), iconName)).getPath());

                File f = new File(selectedImageUri.getPath());
                if (f.exists())
                    f.delete();

                Toast.makeText(
                        getActivity(),
                        mIconIndex
                                + getResources().getString(
                                        R.string.custom_app_icon_successfully),
                        Toast.LENGTH_LONG).show();
                createCustomView();
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
