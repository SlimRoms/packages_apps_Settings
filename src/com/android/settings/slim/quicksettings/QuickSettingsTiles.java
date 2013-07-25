/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.slim.quicksettings;

import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slim.quicksettings.QuickSettingsUtil;
import com.android.settings.slim.quicksettings.QuickSettingsUtil.TileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class QuickSettingsTiles extends Fragment {
    private static final String TAG = "QuickSettingsTiles";

    private static final int MENU_RESET = Menu.FIRST;

    DraggableGridView mDragView;
    private ViewGroup mContainer;
    LayoutInflater mInflater;
    Resources mSystemUiResources;
    Resources res;
    TileAdapter mTileAdapter;
    static ArrayList<String> curr;
    Context mContext;

    private int mTileTextSize;
    public HashMap<String, String> tilesContentMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDragView = new DraggableGridView(getActivity(), null);
        mContainer = container;
        mInflater = inflater;
        mContext = getActivity();
        PackageManager pm = mContext.getPackageManager();
        res = mContext.getResources();
        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
            }
        }
        int colCount = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3);
        updateTileTextSize(colCount);
        return mDragView;
    }

    void loadTilesContent(ArrayList<String> tiles){
        if (tilesContentMap != null) tilesContentMap.clear();
        tilesContentMap = new HashMap<String, String>();
        String tilesContentString = Settings.System.getString(
                    mContext.getContentResolver(), Settings.System.QUICK_SETTINGS_TILE_CONTENT);
        if (tilesContentString == null || tilesContentString.equals("")) return;
        for (String tileContentString : tilesContentString.split("\\|")) {
            StringTokenizer st = new StringTokenizer(tileContentString,"=");
            String tileName = st.nextToken();
            if (tiles.contains(tileName)) {
                tilesContentMap.put(tileName, st.nextToken());
            }
        }
    }

    void genTiles() {
        mDragView.removeAllViews();
        String allTilesString = QuickSettingsUtil.getCurrentTiles(mContext);
        if (!allTilesString.equals("")){
            int customShortcutCount = 0;
            ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(allTilesString);
            loadTilesContent(tiles);
            for (String tileindex : tiles) {
                StringTokenizer st = new StringTokenizer(tileindex, "+");
                QuickSettingsUtil.TileInfo tile = QuickSettingsUtil.TILES.get(st.nextToken());
                if (tile != null) {
                    String tileString = res.getString(tile.getTitleResId());
                    if (st.hasMoreTokens())
                    if (tileindex.startsWith(QuickSettingsUtil.TILE_CUSTOMSHORTCUT)) {
                            customShortcutCount++;
                            String newTileString = tilesContentMap.get(tileindex);
                            if (newTileString != null) tileString = newTileString;
                            else tileString += " " + customShortcutCount;
                    }
                    addTile(tileString, tile.getIcon(), 0, false);
                }
            }
        }
        addTile(res.getString(R.string.profiles_add), null, R.drawable.ic_menu_add, false);
        removeUnsupportedTiles();
    }

    /**
     * Adds a tile to the dragview
     * @param titleId - string id for tile text in systemui
     * @param iconSysId - resource id for icon in systemui
     * @param iconRegId - resource id for icon in local package
     * @param newTile - whether a new tile is being added by user
     */
    void addTile(String titleId, String iconSysId, int iconRegId, boolean newTile) {
        View v = (View) mInflater.inflate(R.layout.qs_tile, null, false);
        TextView name = (TextView) v.findViewById(R.id.qs_text);
        name.setText(titleId);
        name.setTextSize(1, mTileTextSize);
        if (mSystemUiResources != null && iconSysId != null) {
            int resId = mSystemUiResources.getIdentifier(iconSysId, null, null);
            if (resId > 0) {
                try {
                    Drawable d = mSystemUiResources.getDrawable(resId);
                    name.setCompoundDrawablesRelativeWithIntrinsicBounds(null, d, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            name.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRegId, 0, 0);
        }
        mDragView.addView(v, newTile ? mDragView.getChildCount() - 1 : mDragView.getChildCount());
    }

    public void removeUnsupportedTiles() {
        PackageManager pm = mContext.getPackageManager();
        ContentResolver resolver = mContext.getContentResolver();
        // Don't show mobile data options if not supported
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (!isMobileData) {
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_MOBILEDATA);
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_WIFIAP);
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_NETWORKMODE);
            QuickSettingsUtil.TILES_DEFAULT.remove(QuickSettingsUtil.TILE_WIFIAP);
            QuickSettingsUtil.TILES_DEFAULT.remove(QuickSettingsUtil.TILE_MOBILEDATA);
            QuickSettingsUtil.TILES_DEFAULT.remove(QuickSettingsUtil.TILE_NETWORKMODE);
        } else {
            // We have telephony support however, some phones run on networks not supported
            // by the networkmode tile so remove both it and the associated options list
            int network_state = -99;
            try {
                network_state = Settings.Global.getInt(resolver,
                        Settings.Global.PREFERRED_NETWORK_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.e(TAG, "Unable to retrieve PREFERRED_NETWORK_MODE", e);
            }

            switch (network_state) {
                // list of supported network modes
                case Phone.NT_MODE_WCDMA_PREF:
                case Phone.NT_MODE_WCDMA_ONLY:
                case Phone.NT_MODE_GSM_UMTS:
                case Phone.NT_MODE_GSM_ONLY:
                    break;
                default:
                    QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_NETWORKMODE);
                    break;
            }
        }

        // Don't show the bluetooth options if not supported
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            QuickSettingsUtil.TILES_DEFAULT.remove(QuickSettingsUtil.TILE_BLUETOOTH);
        }

        // Dont show the profiles tile if profiles are disabled
        if (Settings.System.getInt(resolver, Settings.System.SYSTEM_PROFILES_ENABLED, 1) != 1) {
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_PROFILE);
        }

        // Dont show the NFC tile if not supported
        if (NfcAdapter.getDefaultAdapter(mContext) == null) {
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_NFC);
        }

        // Dont show the LTE tile if not supported
        if (!deviceSupportsLte()) {
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_LTE);
        }

        // Dont show the torch tile if not supported
        if (!mContext.getResources().getBoolean(R.bool.has_led_flash)) {
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_TORCH);
        }

        // Dont show fast charge tile if not supported
        boolean fchargeIsPossible = false;
        String fchargePath = mContext.getResources()
                .getString(com.android.internal.R.string.config_fastChargePath);
        if (!fchargePath.isEmpty()) {
            File fastcharge = new File(fchargePath);
            if (fastcharge.exists()) {
                fchargeIsPossible = true;
            }
        }
        if (!fchargeIsPossible) {
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_FCHARGE);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        genTiles();
        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
        mDragView.setOnRearrangeListener(new OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                curr = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil.getCurrentTiles(mContext));
                String oldTile = curr.get(oldIndex);
                curr.remove(oldIndex);
                curr.add(newIndex, oldTile);
                QuickSettingsUtil.saveCurrentTiles(mContext, QuickSettingsUtil.getTileStringFromList(curr));
            }
            @Override
            public void onDelete(int index) {
                curr = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil.getCurrentTiles(mContext));
                curr.remove(index);
                QuickSettingsUtil.saveCurrentTiles(mContext, QuickSettingsUtil.getTileStringFromList(curr));
            }
        });
        mDragView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 != mDragView.getChildCount() - 1) return;
                curr = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil.getCurrentTiles(mContext));
                mTileAdapter = null;
                mTileAdapter = new TileAdapter(mContext, 0);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.tile_choose_title)
                .setAdapter(mTileAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int position) {
                        TileInfo info = QuickSettingsUtil.TILES.get(mTileAdapter.getTileId(position));
                        int tileOccurencesCount=1;
                        for (int i=0; i<curr.size();i++)
                            if (curr.get(i).startsWith(info.getId())) tileOccurencesCount++;
                        info.setOccurences(tileOccurencesCount);
                        while (curr.contains(info.getId()+"+"+tileOccurencesCount)) tileOccurencesCount++;
                        if (!info.isSingleton()) curr.add(info.getId()+"+"+tileOccurencesCount);
                        else curr.add(info.getId());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                QuickSettingsUtil.saveCurrentTiles(mContext, QuickSettingsUtil.getTileStringFromList(curr));
                            }
                        }).start();
                        String tileNameDisplay = res.getString(info.getTitleResId());
                        if (!info.isSingleton()) tileNameDisplay += " "+info.getOccurences();
                        addTile(tileNameDisplay, info.getIcon(), 0, true);
                    }
                });
                builder.create().show();
            }
        });
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isPhone(mContext)) {
            mContainer.setPadding(20, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetTiles();
                return true;
            default:
                return false;
        }
    }

    private void resetTiles() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.tiles_reset_title);
        alert.setMessage(R.string.tiles_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                QuickSettingsUtil.resetTiles(mContext);
                genTiles();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.create().show();
    }

    private void updateTileTextSize(int column) {
        // adjust the tile text size based on column count
        switch (column) {
            case 5:
                mTileTextSize = 7;
                break;
            case 4:
                mTileTextSize = 10;
                break;
            case 3:
            default:
                mTileTextSize = 12;
                break;
        }
    }

    @SuppressWarnings("rawtypes")
    static class TileAdapter extends ArrayAdapter {

        ArrayList<String> mTileKeys;
        Resources mResources;

        public TileAdapter(Context context, int textViewResourceId) {
            super(context, android.R.layout.simple_list_item_1);
            getItemsToDisplay();
            mResources = context.getResources();
        }

        private void getItemsToDisplay() {
            mTileKeys = new ArrayList(QuickSettingsUtil.TILES.keySet());
            for (int i=0; i<curr.size(); i++)
                if (mTileKeys.contains(curr.get(i)) && QuickSettingsUtil.TILES.get(curr.get(i)).isSingleton()) mTileKeys.remove(curr.get(i));
        }

        @Override
        public int getCount() {
            return mTileKeys.size();
        }

        @Override
        public Object getItem(int position) {
            int resid = QuickSettingsUtil.TILES.get(mTileKeys.get(position))
                    .getTitleResId();
            return mResources.getString(resid);
        }

        public String getTileId(int position) {
            return QuickSettingsUtil.TILES.get(mTileKeys.get(position))
                    .getId();
        }

    }

    public interface OnRearrangeListener {
        public abstract void onRearrange(int oldIndex, int newIndex);
        public abstract void onDelete(int index);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QUICK_SETTINGS_TILE_CONTENT), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            genTiles();
        }
    }

    private boolean deviceSupportsLte() {
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return (tm.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) || tm.getLteOnGsmMode() != 0;
    }

}
