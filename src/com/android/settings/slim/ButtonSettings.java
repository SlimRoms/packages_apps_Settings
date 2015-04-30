/*
 * Copyright (C) 2014 TeamEos
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

import com.android.internal.util.actions.ActionUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.settings.R;
import com.android.settings.Utils;

public class ButtonSettings extends ActionFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = ButtonSettings.class.getSimpleName();

    // preference keys
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";
    private static final String KEY_BLUETOOTH_INPUT_SETTINGS = "bluetooth_input_settings";

    // category keys
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_POWER = "power_key";

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mSwapVolumeButtons;
    private SwitchPreference mPowerEndCall;
    private SwitchPreference mHomeAnswerCall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.button_settings);

        final Context context = (Context) getActivity();
        final ContentResolver resolver = getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        // test if device has navbar or hardware keys
        final boolean hasHardKeys = ActionUtils.isCapKeyDevice(getActivity());

        // bits for hardware keys present on device
        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        // bits for hardware keys that are capable of waking device
        final int deviceWakeKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareWakeKeys);

        // check for present hardware keys, all devices have some of these
        // all devices should have a power key, but probably good to be thorough
        // anyways
        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);
        // read bits for present hardware keys
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        final boolean hasVolumeKeys = (deviceKeys & KEY_MASK_VOLUME) != 0;

        // read bits for hardware keys that are capable of waking device
        final boolean showHomeWake = (deviceWakeKeys & KEY_MASK_HOME) != 0;
        final boolean showBackWake = (deviceWakeKeys & KEY_MASK_BACK) != 0;
        final boolean showMenuWake = (deviceWakeKeys & KEY_MASK_MENU) != 0;
        final boolean showVolumeWake = (deviceWakeKeys & KEY_MASK_VOLUME) != 0;

        // load categories and init/remove preferences based on device
        // configuration
        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        // All devices have power button,
        mPowerEndCall = (SwitchPreference) findPreference(KEY_POWER_END_CALL);
        if (hasPowerKey) {
            if (!Utils.isVoiceCapable(getActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
            }
        } else {
            prefScreen.removePreference(powerCategory);
        }

        // back key
        if (hasBackKey) {
            if (!showBackWake) {
                backCategory.removePreference(findPreference(Settings.System.BACK_WAKE_SCREEN));
            }
        } else {
            prefScreen.removePreference(backCategory);
        }

        // home key
        mHomeAnswerCall = (SwitchPreference) findPreference(KEY_HOME_ANSWER_CALL);
        if (hasHomeKey) {
            if (!showHomeWake) {
                homeCategory.removePreference(findPreference(Settings.System.HOME_WAKE_SCREEN));
            }
            if (!Utils.isVoiceCapable(getActivity())) {
                homeCategory.removePreference(mHomeAnswerCall);
                mHomeAnswerCall = null;
            }
        } else {
            prefScreen.removePreference(homeCategory);
        }

        // App switch key (recents)
        if (!hasAppSwitchKey) {
            prefScreen.removePreference(appSwitchCategory);
        }

        // menu key
        if (hasMenuKey) {
            if (!showMenuWake) {
                menuCategory.removePreference(findPreference(Settings.System.MENU_WAKE_SCREEN));
            }
        } else {
            prefScreen.removePreference(menuCategory);
        }

        // search/assist key
        if (!hasAssistKey) {
            prefScreen.removePreference(assistCategory);
        }

        // volume rocker
        // NOTE: we set overlay defaults to 64 on the relatively safe assumption
        // all devices have volume rocker
        if (hasVolumeKeys) {
            if (!showVolumeWake) {
                volumeCategory.removePreference(findPreference(Settings.System.VOLUME_WAKE_SCREEN));
            }

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (SwitchPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);
        } else {
            prefScreen.removePreference(volumeCategory);
        }

        // TODO: bring back when/if cmhw is supported
        // hardware keys backlight
        // final ButtonBacklightBrightness backlight =
        //         (ButtonBacklightBrightness) findPreference(KEY_BUTTON_BACKLIGHT);
        // if (!ButtonBacklightBrightness.isButtonSupported(context) && !ButtonBacklightBrightness.isKeyboardSupported(context)) {
        //     prefScreen.removePreference(backlight);
        // }

        // enable / disable navigation hardware key settings
        // if navigation bar is force showing
        if (hasHardKeys) {
            updateAvailableButtonPreferences(isForcedNavbarEnabled(getActivity()));
        }

        // let super know we can load ActionPreferences
        onPreferenceScreenLoaded();

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_BLUETOOTH_INPUT_SETTINGS);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
        }

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                    (incallHomeBehavior == Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (!ActionUtils.isNormalScreen() ? 2 : 1) : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // if a hardware key device is force showing navigation bar, hide
    // the hardware button preferences
    private void updateAvailableButtonPreferences(boolean barShowing) {
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        //final ButtonBacklightBrightness backlight =
        //        (ButtonBacklightBrightness) prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);

        //if (backlight != null) {
        //    backlight.setEnabled(!barShowing);
        // }

        if (backCategory != null) {
            backCategory.setEnabled(!barShowing);
        }
        if (homeCategory != null) {
            homeCategory.setEnabled(!barShowing);
        }
        if (menuCategory != null) {
            menuCategory.setEnabled(!barShowing);
        }
        if (assistCategory != null) {
            assistCategory.setEnabled(!barShowing);
        }
        if (appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!barShowing);
        }
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }

    // if hardware key disabler is supported, the disabler must only be enabled
    // while the navigation bar is force shown
    public static boolean isForcedNavbarEnabled(Context ctx) {
        return Settings.System.getIntForUser(ctx.getContentResolver(),
                Settings.System.DEV_FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
    }
}
