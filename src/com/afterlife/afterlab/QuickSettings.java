/*
 * Copyright (C) 2020 Project-Awaken
 * Copyright (C) 2023-2024 AfterLife Project
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
package com.afterlife.afterlab;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.afterlife.afterlab.fragments.QsHeaderImageSettings;
import com.afterlife.support.preference.SystemSettingListPreference;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.display.DisplayDensityConfiguration;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment 
            implements Preference.OnPreferenceChangeListener {

    private static final String KEY_AFL_QS_CUSTOM  = "qs_custom_header_style";
    private SystemSettingListPreference mAflQsCustom;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.category_quicksettings);
        PreferenceScreen prefSet = getPreferenceScreen();

        mAflQsCustom = findPreference(KEY_AFL_QS_CUSTOM);
        mAflQsCustom.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context mContext = getActivity().getApplicationContext();
        ContentResolver resolver = mContext.getContentResolver();

    if (preference == mAflQsCustom) {
        int value = Integer.parseInt((String) newValue);

        final int defaultSwDp = 450;

        try {
            final Resources res = mContext.getResources();
            final DisplayMetrics metrics = res.getDisplayMetrics();
            final int minDimensionPx = Math.min(metrics.widthPixels, metrics.heightPixels);

            if (value == 1 || value == 2) {
                final int newSwDp = defaultSwDp;
                final int newDensity = DisplayMetrics.DENSITY_MEDIUM * minDimensionPx / newSwDp;
                final int densityDpi = Math.max(newDensity, 120);

                DisplayDensityConfiguration.setForcedDisplayDensity(
                    Display.DEFAULT_DISPLAY,
                    densityDpi
                );

                Settings.System.putInt(resolver, "density", densityDpi);

                Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER,
                    0,
                    UserHandle.USER_CURRENT
                );
            } 

            else if (value == 0) {
                final int defaultDensity = 400;

                DisplayDensityConfiguration.setForcedDisplayDensity(
                    Display.DEFAULT_DISPLAY,
                    defaultDensity
                );

                Settings.System.putInt(resolver, "density", defaultDensity);

                Settings.Secure.putIntForUser(
                    resolver,
                    Settings.Secure.QS_SHOW_BRIGHTNESS_SLIDER,
                    1,
                    UserHandle.USER_CURRENT
                );
            }
        } catch (Exception e) {
            Slog.e("DensityPreference", "Error changing DPI", e);
            return false;
        }

        return true;
    }

    return false;
}

    
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.AFTERLIFE;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.category_quicksettings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}