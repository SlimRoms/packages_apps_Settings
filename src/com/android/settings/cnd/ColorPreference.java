
package com.android.settings.cnd;

import android.graphics.*;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;

public class ColorPreference extends Preference {
    private Context mContext;
    private int mDefaultColor;
    private int mColorHolder;
    private int mCurrentColor;
    private String mSettingsProviderTarget = null;
    private boolean isTargetSet = false;

    public ColorPreference(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
        // set a generic, common android color to safely create the dialog
        // if we were unable to grab the target objects current color value
        // default color only refers to the dialog color selector circle
        mDefaultColor = mContext.getResources().getColor(com.android.internal.R.color.holo_blue_light);
    }

    public ColorPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onClick() {
        if (isTargetSet) {
            mCurrentColor = getColorSetting();
            mColorHolder = mCurrentColor;
            ColorPickerDialog cpd = new ColorPickerDialog(mContext,
                    new ColorPickerDialog.OnColorChangedListener() {

                        @Override
                        public void colorUpdate(int color) {
                            mCurrentColor = color;
                            writeColorSetting(color);
                        }

                        @Override
                        public void colorChanged(int color) {
                        }
                    }, mCurrentColor);

            cpd.setOnCancelListener(new Dialog.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    mCurrentColor = mColorHolder;
                    writeColorSetting(mCurrentColor);
                }
            });
            cpd.setCanceledOnTouchOutside(true);
            cpd.show();
        }
    }

    public void setProviderTarget(String target) {
        setProviderTarget(target, mDefaultColor);
    }

    public void setProviderTarget(String target, int defColor) {
        mSettingsProviderTarget = target;
        mDefaultColor = defColor;
        // hold the previously set color in the event of cancel or dismiss to
        // restore
        mColorHolder = getColorSetting();
        mCurrentColor = mColorHolder;
        isTargetSet = true;
    }

    private void writeColorSetting(int color) {
        Settings.System.putInt(mContext.getContentResolver(), mSettingsProviderTarget, color);
    }

    private int getColorSetting() {
        int color = Settings.System.getInt(mContext.getContentResolver(), mSettingsProviderTarget,
                mDefaultColor);
        if (color == -1) color = mDefaultColor;
        return color;
    }
}
