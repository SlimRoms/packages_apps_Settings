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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.net.Uri;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLConnection;
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

            public void changeFrequency(int freq)
            {        
		timer.cancel();			
		timer = new Timer();                    
		timer.scheduleAtFixedRate(new MainTask(), 0, freq*1000); //60000=1 min &1000=1 sec			
		Toast.makeText(this, "poll frequency changed to "+freq+" seconds!",Toast.LENGTH_SHORT).show(); 
			
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
				try{
				WifiManager wifiManager = (WifiManager) getSystemService("wifi");
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
				//String customURL = "http://chart.apis.google.com/chart?cht=qr&chs=300x300&chl="+macAddress;
				String customURL = "http://redcad.org/members/tarak.chaari/testurl.txt";
				URL url = new URL(customURL);
            			/** Creating an http connection to communcate with url */
            			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
 	    			/** Connecting to url */
           			urlConnection.connect();
				if (urlConnection.getResponseCode()==200)
				{
					BufferedReader fromServer = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
					String urlFromServer = fromServer.readLine();
					fromServer.close();
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(urlFromServer));
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(i);
				}
				}
				catch(Exception e)
				{
					Log.e("DownloaderService", e.getMessage(), e);
				}
                          }
             }
}
