
package com.android.settings.eos;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;

public class NavRingActions extends SettingsPreferenceFragment 
        implements Preference.OnPreferenceChangeListener {
    Context mContext;
    ContentResolver mResolver;
    LayoutInflater mInflate;
    NavRingPreference mRing1;
    NavRingPreference mRing2;
    NavRingPreference mRing3;
    Resources mRes;
    private List<AppPackage> components;
    private AppArrayAdapter mAdapter;
    private ListView mListView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = (Context) getActivity();
        mRes = mContext.getResources();
        mResolver = mContext.getContentResolver();
        populateActionAdapter();

        addPreferencesFromResource(R.xml.navring_settings);

        mRing1 = (NavRingPreference) findPreference("interface_navring_1_release");
        mRing1.setTargetUri(Settings.System.SYSTEMUI_NAVRING_1, new WidgetListener());

        mRing2 = (NavRingPreference) findPreference("interface_navring_2_release");
        mRing2.setTargetUri(Settings.System.SYSTEMUI_NAVRING_2, new WidgetListener());

        mRing3 = (NavRingPreference) findPreference("interface_navring_3_release");
        mRing3.setTargetUri(Settings.System.SYSTEMUI_NAVRING_3, new WidgetListener());

        String target2 = Settings.System.getString(mContext.getContentResolver(), Settings.System.SYSTEMUI_NAVRING_2);
        if (target2 == null || target2.equals("")) {
            Settings.System.putString(mContext.getContentResolver(), Settings.System.SYSTEMUI_NAVRING_2, "assist");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    class WidgetListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            String tag = (String) v.getTag();
            NavRingPreference n = null;
            if (tag != null) {
                if (tag.equals(mRing1.getTargetUri())) {
                    n = mRing1;
                } else if (tag.equals(mRing2.getTargetUri())) {
                    n = mRing2;
                } else if (tag.equals(mRing3.getTargetUri())) {
                    n = mRing3;
                }
                callInitDialog(n);
            }
        }
    }

    public void callInitDialog(final NavRingPreference preference) {
        final NavRingPreference pref = (NavRingPreference) preference;
        final CharSequence[] item_entries = mRes.getStringArray(R.array.navring_dialog_entries);
        final CharSequence[] item_values = mRes.getStringArray(R.array.navring_dialog_values);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mRes.getString(R.string.interface_softkeys_activity_dialog_title))
                .setNegativeButton(mRes.getString(com.android.internal.R.string.cancel),
                        new Dialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                dialog.dismiss();
                            }
                        })
                .setItems(item_entries, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        String pressed = (String) item_values[which];
                        if (pressed.equals(item_values[5])) {
                            callActivityDialog(pref);
                        } else {
                            pref.loadCustomApp(pressed);
                            pref.setTargetValue(pressed);

                        }
                    }
                }).create().show();
    }

    private void populateActionAdapter() {
        components = new ArrayList<AppPackage>();

        PackageManager pm = mContext.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : activities) {
            AppPackage ap = new AppPackage(info, pm);
            components.add(ap);
        }
        mAdapter = new AppArrayAdapter(mContext, components);
        View dialog = View.inflate(mContext, R.layout.activity_dialog, null);
        mListView = (ListView) dialog.findViewById(R.id.dialog_list);
        mListView.setAdapter(mAdapter);
    }

    public void callActivityDialog(final NavRingPreference caller) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setAdapter(mListView.getAdapter(), new Dialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                AppPackage app = (AppPackage) mListView.getAdapter().getItem(which);
                caller.setResourcesFromPackage(app);
            }
        })
                .setTitle(mRes.getString(R.string.interface_softkeys_activity_dialog_title))
                .setNegativeButton(mRes.getString(com.android.internal.R.string.cancel),
                        new Dialog.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                dialog.dismiss();
                            }
                        })
                .create().show();
    }

    public class AppPackage {
        private ComponentName component;
        private String appName;
        private Drawable icon;

        AppPackage(ResolveInfo ri, PackageManager pm) {
            component = new ComponentName(ri.activityInfo.packageName,
                    ri.activityInfo.name);
            appName = ri.activityInfo.loadLabel(pm).toString();
            icon = ri.activityInfo.loadIcon(pm);
        }

        String getComponentName() {
            return component.flattenToString();
        }

        Drawable getIcon() {
            return icon;
        }

        String getName() {
            return appName;
        }
    }

    private class AppArrayAdapter extends ArrayAdapter {
        private final List<AppPackage> apps;
        private final Context mContext;

        public AppArrayAdapter(Context context, List<AppPackage> objects) {
            super(context, R.layout.activity_item, objects);
            this.mContext = context;
            this.apps = objects;
            // TODO Auto-generated constructor stub
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View itemRow = convertView;
            AppPackage ap = (AppPackage) apps.get(position);

            itemRow = ((LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.activity_item, null);

            ((ImageView) itemRow.findViewById(R.id.icon)).setImageDrawable(ap.getIcon());
            ((TextView) itemRow.findViewById(R.id.title)).setText(ap.getName());

            return itemRow;
        }
    }
}
