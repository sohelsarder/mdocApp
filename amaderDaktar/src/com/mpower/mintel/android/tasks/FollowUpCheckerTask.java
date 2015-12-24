package com.mpower.mintel.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.listeners.FollowUpListener;
import com.mpower.mintel.android.preferences.PreferencesActivity;
import com.mpower.mintel.android.utilities.WebUtils;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class FollowUpCheckerTask extends AsyncTask<String, Void, String>{

	FollowUpListener listener;
	
	public void addListener(FollowUpListener listener){
		this.listener = listener;
	}
	
	@Override
	protected String doInBackground(String... arg0) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		
		String followUpUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		followUpUrl += WebUtils.URL_PART_FOLLOWUP+arg0[0];
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(followUpUrl, MIntel
					.getInstance().getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));
			
		
		BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine())!= null){
			sb.append(line);
		}
		return sb.toString();
		}catch (Exception e){
			
		}
		return null;
	}

	
	@Override
	protected void onPostExecute(String result) {
		listener.onFollowUpReceived(result);
		super.onPostExecute(result);
	}
}
