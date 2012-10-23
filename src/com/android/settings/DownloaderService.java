/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.os.IBinder;
import android.util.Log;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;

/**
 * Activity with the accessibility settings.
 */
public class DownloaderService extends Service {
              private static Timer timer = new Timer(); 
	   public static Context mContext;
           @Override
            public IBinder onBind(Intent intent)
           {
                    // TODO Auto-generated method stub
                    return null;
             }

            @Override
             public void onCreate()
           {

                     // Creating service
                      super.onCreate();
			mContext = this;
                     // Starting service
                      startService();
             }

	     @Override 
    	     public void onDestroy() 
	    {
	      super.onDestroy();
	      stopService();
	    }

            private void startService()
            {        
			Toast.makeText(this, "Downloader Service Started!",Toast.LENGTH_SHORT).show(); 
			timer = new Timer();                    
			timer.scheduleAtFixedRate(new MainTask(), 0, 60000); //60000=1 min &1000=1 sec
             }
	    
	     private void stopService()
	    {
	      if (timer != null) timer.cancel();
	      Toast.makeText(this, "Downloader Service Stopped!",Toast.LENGTH_SHORT).show();    
	    }

            private class MainTask extends TimerTask
            { 
			public String filePath="";
			private Handler updateUI = new Handler(){
				@Override
				public void dispatchMessage(Message msg) {
				    super.dispatchMessage(msg);
				    Toast.makeText(mContext, "File sucessfully downloaded to:\r\n"+filePath, Toast.LENGTH_LONG).show();
				}
			};                        
 
			public void run() 
                         {
				//Toast.makeText(this, "Now 1 minute finished",Toast.LENGTH_LONG).show();
				WifiManager wifiManager = (WifiManager) getSystemService("wifi");
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
				String customURL = "http://chart.apis.google.com/chart?cht=qr&chs=300x300&chl="+macAddress;
				final DownloadManager downloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
				DownloadManager.Request request = new DownloadManager.Request(Uri.parse(customURL));
				//request.setDestinationInExternalPublicDir("download2", "test.png");
				final long id = downloadManager.enqueue(request);
				BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent arg1) {
					   // TODO Auto-generated method stub
					   DownloadManager.Query query = new DownloadManager.Query();
					   query.setFilterById(id);
					   Cursor cursor = downloadManager.query(query);
					   if(cursor.moveToFirst()){
					    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
					    int status = cursor.getInt(columnIndex);
					    if(status == DownloadManager.STATUS_SUCCESSFUL){    
						Uri fileUri = downloadManager.getUriForDownloadedFile(id);
						filePath = fileUri.getPath();
						String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
						Toast.makeText(mContext, "File sucessfully downloaded to:\r\n"+uriString, Toast.LENGTH_LONG).show();
						/*try {
							updateUI.sendEmptyMessage(0);
						} catch (Exception e) {e.printStackTrace(); }*/
					    	}
				   	    }
					  } 
	 			};
				registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    				//Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(customURL));
				//startActivity(i);
                          }
             }
}
