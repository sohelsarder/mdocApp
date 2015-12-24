package com.mpower.mintel.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.listeners.PendingCheckListener;
import com.mpower.mintel.android.preferences.PreferencesActivity;
import com.mpower.mintel.android.utilities.WebUtils;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class CheckNotificationsTask extends AsyncTask<String, Void, String> {

	public PendingCheckListener listener;
	
	@Override
	protected String doInBackground(String... params) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String pendingListUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));
		
		pendingListUrl += WebUtils.URL_PART_PENDINGLIST+ "?rmpId="+params[0];
		HttpResponse response = null;
		try{
			response = WebUtils.stringResponseGet(pendingListUrl, MIntel
					.getInstance().getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			
			String line;
			StringBuilder resultBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null){
				resultBuilder.append(line);
			}
			
			return resultBuilder.toString();
		}catch(Exception e){
			// TODO give notification telling about network failure
			// Silect crash because net not available
		}
		
		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		try{
		JSONObject json = new JSONObject(result);
		JSONArray urls = json.getJSONArray("urls");
		JSONArray names = json.getJSONArray("names");
		if (listener != null)
			listener.pendingListRetrieved(urls, names);
		}catch(Exception e){
			// Silent shutdown. Not valid JSON
		}
	}
	
}
