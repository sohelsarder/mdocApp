package com.mpower.daktar.android.fingerprint;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Tristram + Ben on 5/21/2014.
 */

public class ServerConnect {
	private static final String EnrollURL = "http://afis.simprints.com/api/person/enroll";
	private static final String IdentifyURL = "http://afis.simprints.com/api/person/identify";
	private static final String key = "AJK89S2X0CRY638Q8";
	private static final String Name = "Sadat";
	
	public String Enroll(String template, int finger, String name) {
		return QueryServer(key, template, finger, EnrollURL, name);
	}

	public String Identify(String template, String name) {
		return QueryServer(key, template, 0, IdentifyURL, name);
	}

	// send template to given url - need to have active session
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private static String QueryServer(final String key, final String temp,
			final int finger, final String url, final String name) {

		InputStream inputStream;
		String result = "";

		HttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);

		// load pieces of template into JSON object
		JSONObject jsonObject = new JSONObject();
		
		try {
			jsonObject.put("Key", key);
			jsonObject.put("Name", name);
			jsonObject.put("Template", temp);
			jsonObject.put("Finger", finger);

			Log.w("json", jsonObject.toString(4));
			
			StringEntity se = new StringEntity(jsonObject.toString());

			// set HTTP params
			httpPost.setEntity(se);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");

			// Need to make thread
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);

			HttpResponse httpResponse;
			httpResponse = httpClient.execute(httpPost);

			// get hold of response entity data
			inputStream = httpResponse.getEntity().getContent();

			// convert to string
			if (inputStream != null)
				result = convertInputStreamToString(inputStream);
			else{
				Log.w("Server Error", result);
				result = "Error: No server response";
				}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Server Exception", e.toString());
		}
		return result;
	}

	// to convert input stream from server
	private static String convertInputStreamToString(InputStream inputStream)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream));
		String line;
		String result = "";
		while ((line = bufferedReader.readLine()) != null)
			result += line;

		inputStream.close();
		Log.w("Server message", result);
		return result;
	}
}