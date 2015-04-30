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

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference.BaseSavedState;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class LongSwipePreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {
	private static final String TAG = LongSwipePreference.class.getSimpleName();

	private static final int DEVICE_NORMAL_SCREEN = 1;
	private static final int DEVICE_LARGE_SCREEN = 2;
	private static final int DEVICE_XLARGE_SCREEN = 3;

	// same for all devices
	private SeekBar mRightPort;
	private TextView mRightPortVal;
	private SeekBar mLeftPort;
	private TextView mLeftPortVal;

	// this group will double as right/left or up/down
	// depending on screen size
	private SeekBar mRightLand;
	private TextView mRightLandVal;
	private SeekBar mLeftLand;
	private TextView mLeftLandVal;

	// long swipe thresholds user set values
	// as read from settings or being committed
	private int leftLandSetting;
	private int rightLandSetting;
	private int leftPortSetting;
	private int rightPortSetting;

	// hold default values for reset
	private int leftLandDef;
	private int rightLandDef;
	private int leftPortDef;
	private int rightPortDef;

	// min/max values for seekbar math
	private int leftLandMin;
	private int rightLandMin;
	private int leftPortMin;
	private int rightPortMin;
	private int leftLandMax;
	private int rightLandMax;
	private int leftPortMax;
	private int rightPortMax;

	// track the instance state of the seekbars
	private int leftLandTemp;
	private int rightLandTemp;
	private int leftPortTemp;
	private int rightPortTemp;

	private Context mContext;
	private ContentResolver mResolver;

	private int mScreenSize;

	public LongSwipePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mResolver = context.getContentResolver();

		if (ActionUtils.isNormalScreen()) {
			mScreenSize = DEVICE_NORMAL_SCREEN;
		} else if (ActionUtils.isLargeScreen()) {
			mScreenSize = DEVICE_LARGE_SCREEN;
		} else if (ActionUtils.isXLargeScreen()) {
			mScreenSize = DEVICE_XLARGE_SCREEN;
		} else {
			mScreenSize = DEVICE_NORMAL_SCREEN;
		}

		// sane default values to match framework
		// expressed as int for seekbars
		// TODO: add to dimens, use density bucket, kinda ugly here
		if (DEVICE_NORMAL_SCREEN == mScreenSize) {
			leftLandDef = 40;
			rightLandDef = 40;
			leftPortDef = 35;
			rightPortDef = 35;
			leftLandMax = 65;
			rightLandMax = 65;
			leftPortMax = 65;
			rightPortMax = 65;
			leftLandMin = 25;
			rightLandMin = 25;
			leftPortMin = 25;
			rightPortMin = 25;
		} else if (DEVICE_LARGE_SCREEN == mScreenSize) {
			leftLandDef = 30;
			rightLandDef = 30;
			leftPortDef = 40;
			rightPortDef = 40;
			leftLandMax = 75;
			rightLandMax = 75;
			leftPortMax = 85;
			rightPortMax = 85;
			leftLandMin = 20;
			rightLandMin = 20;
			leftPortMin = 25;
			rightPortMin = 25;
		} else if (DEVICE_XLARGE_SCREEN == mScreenSize) {
			leftLandDef = 25;
			rightLandDef = 25;
			leftPortDef = 30;
			rightPortDef = 30;
			leftLandMax = 50;
			rightLandMax = 50;
			leftPortMax = 55;
			rightPortMax = 55;
			leftLandMin = 15;
			rightLandMin = 15;
			leftPortMin = 20;
			rightPortMin = 20;
		} else {
			leftLandDef = 40;
			rightLandDef = 40;
			leftPortDef = 40;
			rightPortDef = 40;
			leftLandMax = 65;
			rightLandMax = 65;
			leftPortMax = 65;
			rightPortMax = 65;
			leftLandMin = 25;
			rightLandMin = 25;
			leftPortMin = 25;
			rightPortMin = 25;
		}
		updateValues();
		setDialogLayoutResource(R.layout.long_swipe_threshold);
	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		builder.setNeutralButton(R.string.long_swipe_reset,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		ViewGroup portRight = (ViewGroup) view
				.findViewById(R.id.right_swipe_port_container);
		mRightPortVal = (TextView) portRight.findViewById(R.id.value);
		mRightPortVal.setText(getPercentString(rightPortTemp));
		mRightPort = (SeekBar) portRight.findViewById(R.id.seekbar);
		mRightPort.setMax(rightPortMax - rightPortMin);
		mRightPort.setProgress(rightPortTemp - rightPortMin);
		mRightPort.setOnSeekBarChangeListener(this);

		ViewGroup portLeft = (ViewGroup) view
				.findViewById(R.id.left_swipe_port_container);
		mLeftPortVal = (TextView) portLeft.findViewById(R.id.value);
		mLeftPortVal.setText(getPercentString(leftPortTemp));
		mLeftPort = (SeekBar) portLeft.findViewById(R.id.seekbar);
		mLeftPort.setMax(leftPortMax - leftPortMin);
		mLeftPort.setProgress(leftPortTemp - leftPortMin);
		mLeftPort.setOnSeekBarChangeListener(this);

		ViewGroup landRight = (ViewGroup) view
				.findViewById(R.id.right_swipe_land_container);
		TextView labelRight = (TextView) landRight.findViewById(R.id.text);
		if (DEVICE_NORMAL_SCREEN == mScreenSize) {
			labelRight.setText(mContext.getString(R.string.up_swipe_title));
		}
		mRightLandVal = (TextView) landRight.findViewById(R.id.value);
		mRightLandVal.setText(getPercentString(rightLandTemp));
		mRightLand = (SeekBar) landRight.findViewById(R.id.seekbar);
		mRightLand.setMax(rightLandMax - rightLandMin);
		mRightLand.setProgress(rightLandTemp - rightLandMin);
		mRightLand.setOnSeekBarChangeListener(this);

		ViewGroup landLeft = (ViewGroup) view
				.findViewById(R.id.left_swipe_land_container);
		TextView labelLeft = (TextView) landLeft.findViewById(R.id.text);
		if (DEVICE_NORMAL_SCREEN == mScreenSize) {
			labelLeft.setText(mContext.getString(R.string.down_swipe_title));
		}
		mLeftLandVal = (TextView) landLeft.findViewById(R.id.value);
		mLeftLandVal.setText(getPercentString(leftLandTemp));
		mLeftLand = (SeekBar) landLeft.findViewById(R.id.seekbar);
		mLeftLand.setMax(leftLandMax - leftLandMin);
		mLeftLand.setProgress(leftLandTemp - leftLandMin);
		mLeftLand.setOnSeekBarChangeListener(this);

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (seekBar.equals(mRightPort)) {
			rightPortTemp = progress + rightPortMin;
			mRightPortVal.setText(getPercentString(rightPortTemp));
		} else if (seekBar.equals(mLeftPort)) {
			leftPortTemp = progress + leftPortMin;
			mLeftPortVal.setText(getPercentString(leftPortTemp));
		} else if (seekBar.equals(mRightLand)) {
			rightLandTemp = progress + rightLandMin;
			mRightLandVal.setText(getPercentString(rightLandTemp));
		} else if (seekBar.equals(mLeftLand)) {
			leftLandTemp = progress + leftLandMin;
			mLeftLandVal.setText(getPercentString(leftLandTemp));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void showDialog(Bundle state) {
		super.showDialog(state);
		AlertDialog d = (AlertDialog) getDialog();
		Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
		defaultsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRightPort.setProgress(rightPortDef - rightPortMin);
				mLeftPort.setProgress(leftPortDef - leftPortMin);
				mRightLand.setProgress(rightLandDef - rightLandMin);
				mLeftLand.setProgress(leftLandDef - leftLandMin);
			}
		});
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (!positiveResult) {
			// user canceled, set Temp values back to
			// last saved settings vals
			leftPortTemp = leftPortSetting;
			rightPortTemp = rightPortSetting;
			leftLandTemp = leftLandSetting;
			rightLandTemp = rightLandSetting;
			return;
		}
		String rightUri;
		String leftUri;

		if (mScreenSize == DEVICE_NORMAL_SCREEN) {
			rightUri = Settings.System.NX_LONGSWIPE_THRESHOLD_UP_LAND;
			leftUri = Settings.System.NX_LONGSWIPE_THRESHOLD_DOWN_LAND;
		} else {
			rightUri = Settings.System.NX_LONGSWIPE_THRESHOLD_RIGHT_LAND;
			leftUri = Settings.System.NX_LONGSWIPE_THRESHOLD_LEFT_LAND;
		}

		commitValue(Settings.System.NX_LONGSWIPE_THRESHOLD_LEFT_PORT, leftPortTemp);
		commitValue(Settings.System.NX_LONGSWIPE_THRESHOLD_RIGHT_PORT, rightPortTemp);
		commitValue(leftUri, leftLandTemp);
		commitValue(rightUri, rightLandTemp);

		// all the settings are equal since we committed
		leftPortSetting = leftPortTemp;
		rightPortSetting = rightPortTemp;
		leftLandSetting = leftLandTemp;
		rightLandSetting = rightLandTemp;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (getDialog() == null || !getDialog().isShowing()) {
			return superState;
		}

		// Save the dialog state
		final SavedState myState = new SavedState(superState);
		myState.mLSTleftPort = leftPortTemp;
		myState.mLSTrightPort = rightPortTemp;
		myState.mLSTleftLand = leftLandTemp;
		myState.mLSTrightLand = rightLandTemp;

		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());

		mLeftPort.setProgress(myState.mLSTleftPort - leftPortMin);
		mRightPort.setProgress(myState.mLSTrightPort - rightPortMin);
		mLeftLand.setProgress(myState.mLSTleftLand - leftLandMin);
		mRightLand.setProgress(myState.mLSTrightLand - rightLandMin);
	}

	private void updateValues() {
		String rightUri;
		String leftUri;

		if (mScreenSize == DEVICE_NORMAL_SCREEN) {
            rightUri = Settings.System.NX_LONGSWIPE_THRESHOLD_UP_LAND;
            leftUri = Settings.System.NX_LONGSWIPE_THRESHOLD_DOWN_LAND;
		} else {
			rightUri = Settings.System.NX_LONGSWIPE_THRESHOLD_RIGHT_LAND;
			leftUri = Settings.System.NX_LONGSWIPE_THRESHOLD_LEFT_LAND;
		}

		leftLandSetting = float2int(Settings.System.getFloat(mResolver,
				leftUri, int2float(leftLandDef)));
		leftLandTemp = leftLandSetting;

		rightLandSetting = float2int(Settings.System.getFloat(mResolver,
				rightUri, int2float(rightLandDef)));
		rightLandTemp = rightLandSetting;

		leftPortSetting = float2int(Settings.System.getFloat(mResolver,
		        Settings.System.NX_LONGSWIPE_THRESHOLD_LEFT_PORT, int2float(leftPortDef)));
		leftPortTemp = leftPortSetting;

		rightPortSetting = float2int(Settings.System.getFloat(mResolver,
				Settings.System.NX_LONGSWIPE_THRESHOLD_RIGHT_PORT, int2float(rightPortDef)));
		rightPortTemp = rightPortSetting;

	}

	private void commitValue(String uri, int val) {
		Settings.System.putFloat(mResolver, uri, int2float(val));
	}

	private String getPercentString(int val) {
		return String.format("%d%%", val);
	}

	private int float2int(float f) {
		return Math.round(f * 100);
	}

	private float int2float(int i) {
		return ((float) (i * 100) / 10000);
	}

	private static class SavedState extends BaseSavedState {
		int mLSTleftPort;
		int mLSTrightPort;
		int mLSTleftLand;
		int mLSTrightLand;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		public SavedState(Parcel source) {
			super(source);
			mLSTleftPort = source.readInt();
			mLSTrightPort = source.readInt();
			mLSTleftLand = source.readInt();
			mLSTrightLand = source.readInt();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(mLSTleftPort);
			dest.writeInt(mLSTrightPort);
			dest.writeInt(mLSTleftLand);
			dest.writeInt(mLSTrightLand);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
