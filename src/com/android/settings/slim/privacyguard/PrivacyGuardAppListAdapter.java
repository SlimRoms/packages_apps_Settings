/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.slim.privacyguard;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

public class PrivacyGuardAppListAdapter extends BaseAdapter {

    private LayoutInflater mInflater;

    private List<PrivacyGuardAppInfo> mApps;
    private Map<String, Drawable> mIcons;
    private Drawable mDefaultImg;

    private Context mContext;

    //constructor
    public PrivacyGuardAppListAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        // set the default icon till the actual app icon is loaded in async task
        mDefaultImg = mContext.getResources().getDrawable(R.drawable.ic_launcher);
    }

    @Override
    public int getCount() {
        return mApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        PrivacyGuardAppViewHolder appHolder;
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.privacy_guard_manager_list_row, null);

            // creates a ViewHolder and children references
            appHolder = new PrivacyGuardAppViewHolder();
            appHolder.mTitle = (TextView) convertView.findViewById(R.id.appTitle);
            appHolder.mIcon = (ImageView) convertView.findViewById(R.id.appIcon);
            appHolder.mPrivacyGuardIcon = (ImageView) convertView.findViewById(R.id.appPrivacyGuardIcon);
            convertView.setTag(appHolder);
        } else {
            appHolder = (PrivacyGuardAppViewHolder) convertView.getTag();
        }

        PrivacyGuardAppInfo app = mApps.get(position);

        appHolder.setTitle(app.getTitle());
        if (mIcons == null
            || mIcons.get(app.getPackageName()) == null) {
            appHolder.setIcon(mDefaultImg);
        } else {
            appHolder.setIcon(mIcons.get(app.getPackageName()));
        }

        appHolder.setPrivacyGuardIcon(app.getPrivacyGuard());

        return convertView;
    }

    public void setListItems(List<PrivacyGuardAppInfo> list) {
        mApps = list;
    }

    public void setIcons(Map<String, Drawable> icons) {
        this.mIcons = icons;
    }

    public Map<String, Drawable> getIcons() {
        return mIcons;
    }

    /**
     * App view holder used to reuse the views inside the list.
     */
    public class PrivacyGuardAppViewHolder {

        private TextView mTitle;
        private ImageView mIcon;
        private ImageView mPrivacyGuardIcon;

        // set the title
        public void setTitle(String title) {
            mTitle.setText(title);
        }

        // set the app icon
        public void setIcon(Drawable img) {
            if (img != null) {
                mIcon.setImageDrawable(img);
            }
        }

        // set the privacy guard icon
        public void setPrivacyGuardIcon(boolean privacyEnabled) {
            if (privacyEnabled) {
                Drawable img = mContext.getResources().getDrawable(R.drawable.ic_privacy_guard);
                mPrivacyGuardIcon.setImageDrawable(img);
            } else {
                mPrivacyGuardIcon.setImageDrawable(null);
            }
        }
    }
}
