/*
 * Copyright (C) 2014 SlimRoms
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

package com.android.settings.slim;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.android.internal.util.slim.ButtonsConstants;
import com.android.internal.util.slim.SlimActions;

public class SlimActionShortcut extends Activity  {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        String value = getIntent().getStringExtra("value");
        if (ButtonsConstants.ACTION_RECENTS.equals(value)) {
            SlimActions.triggerVirtualKeypress(
                    KeyEvent.KEYCODE_APP_SWITCH, false);
        } else {
            SlimActions.processAction(this, value, false);
        }
        this.finish();
    }
}