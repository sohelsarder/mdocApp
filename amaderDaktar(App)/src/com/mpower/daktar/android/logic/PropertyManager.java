/*
 * Copyright (C) 2009 University of Washington
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

package com.mpower.daktar.android.logic;

import java.util.HashMap;
import java.util.Vector;

import org.javarosa.core.services.IPropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Used to return device properties to JavaRosa
 *
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */

public class PropertyManager implements IPropertyManager {

	private final String t = "PropertyManager";

	private final Context mContext;

	private final TelephonyManager mTelephonyManager;
	private final HashMap<String, String> mProperties;

	private final static String DEVICE_ID_PROPERTY = "deviceid"; // imei
	private final static String SUBSCRIBER_ID_PROPERTY = "subscriberid"; // imsi
	private final static String SIM_SERIAL_PROPERTY = "simserial";
	private final static String PHONE_NUMBER_PROPERTY = "phonenumber";

	public String getName() {
		return "Property Manager";
	}

	public PropertyManager(final Context context) {
		Log.i(t, "calling constructor");

		mContext = context;

		mProperties = new HashMap<String, String>();
		mTelephonyManager = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);

		String deviceId = mTelephonyManager.getDeviceId();
		if (deviceId != null
				&& (deviceId.contains("*") || deviceId
						.contains("000000000000000"))) {
			deviceId = Settings.Secure.getString(mContext.getContentResolver(),
					Settings.Secure.ANDROID_ID);
		}
		mProperties.put(DEVICE_ID_PROPERTY, deviceId);
		mProperties.put(SUBSCRIBER_ID_PROPERTY,
				mTelephonyManager.getSubscriberId());
		mProperties.put(SIM_SERIAL_PROPERTY,
				mTelephonyManager.getSimSerialNumber());
		mProperties.put(PHONE_NUMBER_PROPERTY,
				mTelephonyManager.getLine1Number());
	}

	@Override
	public Vector<String> getProperty(final String propertyName) {
		return null;
	}

	@Override
	public String getSingularProperty(final String propertyName) {
		return mProperties.get(propertyName.toLowerCase());
	}

	@Override
	public void setProperty(final String propertyName,
			final String propertyValue) {
	}

	@Override
	public void setProperty(final String propertyName,
			@SuppressWarnings("rawtypes") final Vector propertyValue) {

	}

	@Override
	public void addRules(final IPropertyRules rules) {

	}

	@Override
	public Vector<IPropertyRules> getRules() {
		return null;
	}

}
