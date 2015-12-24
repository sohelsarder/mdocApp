/*
 * Copyright (C) 2011 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mpower.mintel.android.preferences;

import com.mpower.mintel.android.R;

import com.mpower.mintel.android.utilities.UrlUtils;
import com.mpower.mintel.android.utilities.WebUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Toast;

/**
 * @author yanokwa
 */
public class PreferencesActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {


	public static String KEY_LAST_VERSION = "lastVersion";
	public static String KEY_FIRST_RUN = "firstRun";
	public static String KEY_FONT_SIZE = "font_size";

	public static String KEY_SERVER_URL = "server_url";
	public static String KEY_SERVER_PORT = "server_port";
	public static String KEY_NOTIFICATION_PORT = "notif_port";
	public static String KEY_USERNAME = "username";
	public static String KEY_PASSWORD = "password";

	private EditTextPreference mServerUrlPreference;
	//private EditTextPreference mServerPortPreference;
	private EditTextPreference mNotifPortPreference;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.general_preferences));

		updateServerUrl();
		//updateServerPort();
		updateFontSize();
		updateNotificationPort();
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		updateServerUrl();

		//updateServerPort();
		
		updateFontSize();
		
		updateNotificationPort();
		
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED) {
			// request was canceled, so do nothing
			return;
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		if (key.equals(KEY_SERVER_URL)) {
			updateServerUrl();
		} else if (key.equals(KEY_FONT_SIZE)) {
			updateFontSize();
		}else if (key.equals(KEY_NOTIFICATION_PORT)){
			updateNotificationPort();
		}
	}

	private void updateNotificationPort() {
		mNotifPortPreference = (EditTextPreference) findPreference(KEY_NOTIFICATION_PORT);
		mNotifPortPreference.setSummary(mNotifPortPreference.getText());
		updateCredential();
	}

//	private void updateServerPort() {
//		mServerPortPreference = (EditTextPreference) findPreference(KEY_SERVER_PORT);
//		mServerPortPreference.setSummary(mServerPortPreference.getText());
//		updateCredential();
//	}

	private void validateUrl(EditTextPreference preference) {
		if (preference != null) {
			String url = preference.getText();
			if (UrlUtils.isValidUrl(url)) {
				preference.setText(url);
				preference.setSummary(url);
			} else {
				// preference.setText((String) preference.getSummary());
				Toast.makeText(getApplicationContext(),
						getString(R.string.url_error), Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	private void updateServerUrl() {
		mServerUrlPreference = (EditTextPreference) findPreference(KEY_SERVER_URL);

		// remove all trailing "/"s
		while (mServerUrlPreference.getText().endsWith("/")) {
			mServerUrlPreference.setText(mServerUrlPreference.getText()
					.substring(0, mServerUrlPreference.getText().length() - 1));
		}
		validateUrl(mServerUrlPreference);
		mServerUrlPreference.setSummary(mServerUrlPreference.getText());

		mServerUrlPreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });
		
		updateCredential();
	}

	private void updateCredential() {
		WebUtils.refreshCredential();
	}

	private void updateFontSize() {
		ListPreference lp = (ListPreference) findPreference(KEY_FONT_SIZE);
		lp.setSummary(lp.getEntry());
	}

	private InputFilter getReturnFilter() {
		InputFilter returnFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.getType((source.charAt(i))) == Character.CONTROL) {
						return "";
					}
				}
				return null;
			}
		};
		return returnFilter;
	}
}
