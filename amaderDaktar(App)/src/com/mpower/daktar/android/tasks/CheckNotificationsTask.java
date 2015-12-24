package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.PendingCheckListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

public class CheckNotificationsTask extends AsyncTask<String, Void, String> {

	public PendingCheckListener listener;

	@Override
	protected String doInBackground(final String... params) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String pendingListUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		pendingListUrl += WebUtils.URL_PART_PENDINGLIST + "?rmpId=" + params[0];
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(pendingListUrl, MIntel
					.getInstance().getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));

			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));

			String line;
			final StringBuilder resultBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				resultBuilder.append(line);
			}

			return resultBuilder.toString();
		} catch (final Exception e) {
			// TODO give notification telling about network failure
			// Silect crash because net not available
		}

		return null;
	}

	@Override
	protected void onPostExecute(final String result) {
		super.onPostExecute(result);
		try {
			final JSONObject json = new JSONObject(result);
			final JSONArray urls = json.getJSONArray("urls");
			final JSONArray names = json.getJSONArray("names");
			if (listener != null) {
				listener.pendingListRetrieved(urls, names);
			}
		} catch (final Exception e) {
			// Silent shutdown. Not valid JSON
		}
	}

}
