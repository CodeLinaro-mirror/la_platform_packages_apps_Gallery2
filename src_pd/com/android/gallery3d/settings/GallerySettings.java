/*
 * Copyright (C) 2011 The Android Open Source Project
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
/*
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.gallery3d.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import com.android.gallery3d.R;

import android.util.Log;

public class GallerySettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "GallerySettings";
    private static final String ENABLE_C2PA = "c2pa_option";
    SwitchPreference mC2pa;
    SharedPreferences mSettings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preferenc);
        initPreferences();
        mSettings = getPreferenceScreen().getSharedPreferences();
    }

    private void initPreferences() {
        mC2pa = (SwitchPreference) findPreference(ENABLE_C2PA);
        mC2pa.setOnPreferenceChangeListener(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mC2pa.setChecked(mSettings.getBoolean(ENABLE_C2PA, false));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(mC2pa.equals(preference)){
            Log.i(TAG,"onPreferenceChange,newValue :" + newValue);
            mC2pa.setChecked((Boolean)(newValue));
        }
        return false;
    }
}
