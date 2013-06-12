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

public class PrivacyGuardAppInfo {

   private String mAppTitle;
   private String mAppPackageName;
   private boolean mAppPrivacyGuard;

   // get and set information off each app object
   // created holded here

   public String getTitle() {
      return mAppTitle;
   }

   public void setTitle(String title) {
      this.mAppTitle = title;
   }

   public String getPackageName() {
      return mAppPackageName;
   }

   public void setPackageName(String packageName) {
      this.mAppPackageName = packageName;
   }

   public boolean getPrivacyGuard() {
      return mAppPrivacyGuard;
   }

   public void setPrivacyGuard(boolean privacyGuard) {
      this.mAppPrivacyGuard = privacyGuard;
   }

}
