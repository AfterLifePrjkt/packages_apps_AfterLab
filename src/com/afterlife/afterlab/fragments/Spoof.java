/*
 * Copyright (C) 2023-2024 the risingOS Android Project
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
package com.afterlife.afterlab.fragments;

import android.app.Activity;
import android.app.AlertDialog; 
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.afterlife.SystemRestartUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

@SearchIndexable
public class Spoof extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "Spoof";
    private static final String SYS_GMS_SPOOF = "persist.sys.pixelprops.gms";
    private static final String SYS_KEYBOX_SPOOF = "persist.sys.entryhooks_enabled";
    private static final String SYS_GOOGLE_SPOOF = "persist.sys.pixelprops.google";
    private static final String SYS_PROP_OPTIONS = "persist.sys.pixelprops.all";
    private static final String SYS_GAMEPROP_ENABLED = "persist.sys.gameprops.enabled";
    private static final String SYS_GPHOTOS_SPOOF = "persist.sys.pixelprops.gphotos";
    private static final String KEY_PIF_JSON_FILE_PREFERENCE = "pif_json_file_preference";
    private static final String KEY_GAME_PROPS_JSON_FILE_PREFERENCE = "game_props_json_file_preference";
    private static final String KEY_UPDATE_JSON_BUTTON = "update_pif_json";

    private boolean isPixelDevice;

    private Preference mGmsSpoof;
    private Preference mKeyBoxSpoof;
    private Preference mGoogleSpoof;
    private Preference mGphotosSpoof;
    private Preference mPropOptions;
    private Preference mPifJsonFilePreference;
    private Preference mGamePropsJsonFilePreference;
    private Preference mGamePropsSpoof;
    private Preference mWikiLink;
    private Preference mUpdateJsonButton;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        addPreferencesFromResource(R.xml.afterlife_settings_spoof);

        mGamePropsSpoof = findPreference(SYS_GAMEPROP_ENABLED);
        mGphotosSpoof = findPreference(SYS_GPHOTOS_SPOOF);
        mGmsSpoof = findPreference(SYS_GMS_SPOOF);
        mKeyBoxSpoof = findPreference(SYS_KEYBOX_SPOOF);
        mGoogleSpoof = findPreference(SYS_GOOGLE_SPOOF);
        mPropOptions = findPreference(SYS_PROP_OPTIONS);
        mPifJsonFilePreference = findPreference(KEY_PIF_JSON_FILE_PREFERENCE);
        mGamePropsJsonFilePreference = findPreference(KEY_GAME_PROPS_JSON_FILE_PREFERENCE);
        mUpdateJsonButton = findPreference(KEY_UPDATE_JSON_BUTTON);

        String model = SystemProperties.get("ro.product.model");
        isPixelDevice = SystemProperties.get("ro.soc.manufacturer").equals("Google");

        mGmsSpoof.setDependency(SYS_PROP_OPTIONS);
        mGphotosSpoof.setDependency(SYS_PROP_OPTIONS);
        
        if (isPixelDevice) {
            mGoogleSpoof.setDefaultValue(false);
            if (isMainlineTensorModel(model)) {
                mGoogleSpoof.setEnabled(false);
                mGoogleSpoof.setSummary(R.string.google_spoof_option_disabled);
            }
        }

        mGmsSpoof.setOnPreferenceChangeListener(this);
        mKeyBoxSpoof.setOnPreferenceChangeListener(this);
        mPropOptions.setOnPreferenceChangeListener(this);
        mGoogleSpoof.setOnPreferenceChangeListener(this);
        mGphotosSpoof.setOnPreferenceChangeListener(this);
        mGamePropsSpoof.setOnPreferenceChangeListener(this);

        mPifJsonFilePreference.setOnPreferenceClickListener(preference -> {
            openFileSelector(10001);
            return true;
        });

        mGamePropsJsonFilePreference.setOnPreferenceClickListener(preference -> {
            openFileSelector(10002);
            return true;
        });
        
        mWikiLink = findPreference("wiki_link");
        if (mWikiLink != null) {
            mWikiLink.setOnPreferenceClickListener(preference -> {
                Uri uri = Uri.parse("https://github.com/AfterlifeOS/afterlifeOS_wiki");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            });
        }
        
        mUpdateJsonButton.setOnPreferenceClickListener(preference -> {
            updatePropertiesFromUrl("https://raw.githubusercontent.com/AfterlifeOS/afterlifeOS_wiki/refs/heads/14/spoofing/PlayIntergrity/pif.json");
            return true;
        });
        
        Preference showPropertiesPref = findPreference("show_pif_properties");
        if (showPropertiesPref != null) {
            showPropertiesPref.setOnPreferenceClickListener(preference -> {
                showPropertiesDialog();
                return true;
            });
        }
    }
    
    private boolean isMainlineTensorModel(String model) {
        return model.matches("Pixel [8-9][a-zA-Z ]*");
    }

    private void openFileSelector(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == 10001) {
                    loadPifJson(uri);
                } else if (requestCode == 10002) {
                    loadGameSpoofingJson(uri);
                }
            }
        }
    }
    
    private void showPropertiesDialog() {
        StringBuilder properties = new StringBuilder();
        try {
            JSONObject jsonObject = new JSONObject();
            String[] keys = {
                "persist.sys.pihooks_ID",
                "persist.sys.pihooks_BRAND",
                "persist.sys.pihooks_DEVICE",
                "persist.sys.pihooks_FINGERPRINT",
                "persist.sys.pihooks_MANUFACTURER",
                "persist.sys.pihooks_MODEL",
                "persist.sys.pihooks_PRODUCT",
                "persist.sys.pihooks_SECURITY_PATCH",
                "persist.sys.pihooks_DEVICE_INITIAL_SDK_INT"
            };
            for (String key : keys) {
                String value = SystemProperties.get(key, null);
                if (value != null) {
                    String buildKey = key.replace("persist.sys.pihooks_", "");
                    jsonObject.put(buildKey, value);
                }
            }
            properties.append(jsonObject.toString(4));
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON from properties", e);
            properties.append(getString(R.string.error_loading_properties));
        }
        new AlertDialog.Builder(getContext())
            .setTitle(R.string.show_pif_properties_title)
            .setMessage(properties.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void updatePropertiesFromUrl(String urlString) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try (InputStream inputStream = urlConnection.getInputStream()) {
                    String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    Log.d(TAG, "Downloaded JSON data: " + json);
                    JSONObject jsonObject = new JSONObject(json);
                    String spoofedModel = jsonObject.optString("MODEL", "Unknown model");
                    for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                        String key = it.next();
                        String value = jsonObject.getString(key);
                        Log.d(TAG, "Setting property: persist.sys.pihooks_" + key + " = " + value);
                        SystemProperties.set("persist.sys.pihooks_" + key, value);
                    }
                    mHandler.post(() -> {
                        String toastMessage = getString(R.string.toast_spoofing_success, spoofedModel);
                        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();
                    });

                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading JSON or setting properties", e);
                mHandler.post(() -> {
                    Toast.makeText(getContext(), R.string.toast_spoofing_failure, Toast.LENGTH_LONG).show();
                });
            }
            mHandler.postDelayed(() -> {
                SystemRestartUtils.showSystemRestartDialog(getContext());
            }, 1250);
        }).start();
    }

    private void loadPifJson(Uri uri) {
        Log.d(TAG, "Loading PIF JSON from URI: " + uri.toString());
        try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Log.d(TAG, "PIF JSON data: " + json);
                JSONObject jsonObject = new JSONObject(json);
                for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                    String key = it.next();
                    String value = jsonObject.getString(key);
                    Log.d(TAG, "Setting PIF property: persist.sys.pihooks_" + key + " = " + value);
                    SystemProperties.set("persist.sys.pihooks_" + key, value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading PIF JSON or setting properties", e);
        }
        mHandler.postDelayed(() -> {
            SystemRestartUtils.showSystemRestartDialog(getContext());
        }, 1250);
    }

    private void loadGameSpoofingJson(Uri uri) {
        Log.d(TAG, "Loading Game Props JSON from URI: " + uri.toString());
        try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Log.d(TAG, "Game Props JSON data: " + json);
                JSONObject jsonObject = new JSONObject(json);
                for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                    String key = it.next();
                    if (key.startsWith("PACKAGES_") && !key.endsWith("_DEVICE")) {
                        String deviceKey = key + "_DEVICE";
                        if (jsonObject.has(deviceKey)) {
                            JSONObject deviceProps = jsonObject.getJSONObject(deviceKey);
                            JSONArray packages = jsonObject.getJSONArray(key);
                            for (int i = 0; i < packages.length(); i++) {
                                String packageName = packages.getString(i);
                                Log.d(TAG, "Spoofing package: " + packageName);
                                setGameProps(packageName, deviceProps);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading Game Props JSON or setting properties", e);
        }
        mHandler.postDelayed(() -> {
            SystemRestartUtils.showSystemRestartDialog(getContext());
        }, 1250);
    }

    private void setGameProps(String packageName, JSONObject deviceProps) {
        try {
            for (Iterator<String> it = deviceProps.keys(); it.hasNext(); ) {
                String key = it.next();
                String value = deviceProps.getString(key);
                String systemPropertyKey = "persist.sys.gameprops." + packageName + "." + key;
                SystemProperties.set(systemPropertyKey, value);
                Log.d(TAG, "Set system property: " + systemPropertyKey + " = " + value);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing device properties", e);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mGmsSpoof 
            || preference == mKeyBoxSpoof
            || preference == mPropOptions
            || preference == mGoogleSpoof
            || preference == mGphotosSpoof
            || preference == mGamePropsSpoof) {
            SystemRestartUtils.showSystemRestartDialog(getContext());
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.afterlife_settings_spoof) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
