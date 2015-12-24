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
package com.mpower.mintel.android.application;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;
import org.apache.http.util.ByteArrayBuffer;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.services.PushService;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * Extends the Application class to implement
 * 
 * @author carlhartung
 * 
 */
public class MIntel extends Application {

	// Storage paths
	public static final String MINTEL_ROOT = Environment
			.getExternalStorageDirectory() + "/amaderDaktar";
	public static final String FORMS_PATH = MINTEL_ROOT + "/forms";
	public static final String INSTANCES_PATH = MINTEL_ROOT + "/instances";
	public static final String CACHE_PATH = MINTEL_ROOT + "/.cache";
	public static final String METADATA_PATH = MINTEL_ROOT + "/metadata";
	public static final String PRESCRIPTION_PATH = MINTEL_ROOT
			+ "/prescriptions";
	public static final String TMPFILE_PATH = CACHE_PATH + "/tmp.jpg";

	public static final String DEFAULT_FONTSIZE = "21";

	private HttpContext localContext = null;
	private static MIntel singleton = null;

	private static Context context;

	public static MIntel getInstance() {
		return singleton;
	}

	/**
	 * Creates required directories on the SDCard (or other external storage)
	 * 
	 * @throws RuntimeException
	 *             if there is no SDCard or the directory exists as a non
	 *             directory
	 */
	public static void createMIntelDirs() throws RuntimeException {
		String cardstatus = Environment.getExternalStorageState();
		if (cardstatus.equals(Environment.MEDIA_REMOVED)
				|| cardstatus.equals(Environment.MEDIA_UNMOUNTABLE)
				|| cardstatus.equals(Environment.MEDIA_UNMOUNTED)
				|| cardstatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY)
				|| cardstatus.equals(Environment.MEDIA_SHARED)) {
			RuntimeException e = new RuntimeException(
					"mIntel reports :: SDCard error: "
							+ Environment.getExternalStorageState());
			throw e;
		}

		String[] dirs = { MINTEL_ROOT, FORMS_PATH, INSTANCES_PATH, CACHE_PATH,
				METADATA_PATH, PRESCRIPTION_PATH };

		for (String dirName : dirs) {
			File dir = new File(dirName);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					RuntimeException e = new RuntimeException(
							"mIntel reports :: Cannot create directory: "
									+ dirName);
					throw e;
				}
			} else {
				if (!dir.isDirectory()) {
					RuntimeException e = new RuntimeException(
							"mIntel reports :: " + dirName
									+ " exists, but is not a directory");
					throw e;
				}
			}
		}

		String[] fileNames = { "pres_n.ogg", "call_n.ogg" };
		copyAudioFiles(fileNames);
	}

	private static void copyAudioFiles(String[] assetName) {
		for (int i = 0; i < assetName.length; i++) {
			File file = new File(METADATA_PATH, assetName[i]);
			if (!file.exists()) {
				AssetManager mngr = getAppContext().getAssets();
				ByteArrayBuffer baf = new ByteArrayBuffer(2048);
				try {
					InputStream path = mngr.open(assetName[i]);
					BufferedInputStream bis = new BufferedInputStream(path,
							1024);
					int current = 0;
					while ((current = bis.read()) != -1) {
						baf.append((byte) current);
					}
					byte[] bitmapdata = baf.toByteArray();

					FileOutputStream fos;
					fos = new FileOutputStream(file);
					fos.write(bitmapdata);
					fos.flush();
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Shared HttpContext so a user doesn't have to re-enter login information
	 * 
	 * @return
	 */
	public synchronized HttpContext getHttpContext() {
		if (localContext == null) {
			// set up one context for all HTTP requests so that authentication
			// and cookies can be retained.
			localContext = new SyncBasicHttpContext(new BasicHttpContext());

			// establish a local cookie store for this attempt at downloading...
			CookieStore cookieStore = new BasicCookieStore();
			localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

			// and establish a credentials provider. Default is 7 minutes.
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			localContext.setAttribute(ClientContext.CREDS_PROVIDER,
					credsProvider);
		}
		return localContext;
	}

	@Override
	public void onCreate() {
		singleton = this;
		MIntel.context = getApplicationContext();
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		super.onCreate();
		// PushService.actionStart(getAppContext());
	}

	public static Context getAppContext() {
		return MIntel.context;
	}

	public static String getDeviceId() {
		TelephonyManager tm = (TelephonyManager) getAppContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

}
