package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.MissedNotificationListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

public class MissedNotificationTask extends AsyncTask<String, Void, String> {

	private MissedNotificationListener listener;
	private String type;

	public void setListener(final MissedNotificationListener listener) {
		this.listener = listener;
	}

	@Override
	protected String doInBackground(final String... params) {
		final String ret = null;
		Log.w("PENDING", "Entered Task");
		if (params[1] != null) {
			type = params[1];
			if (params[1].equals("pres")) {

				final SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(MIntel.getInstance()
								.getBaseContext());
				String prescListUrl = settings.getString(
						PreferencesActivity.KEY_SERVER_URL,
						MIntel.getInstance().getString(
								R.string.default_server_url));

				prescListUrl += WebUtils.URL_PART_PENDINGLIST + params[0];

				// String missedCallUrl = WebUtils.URL_PART_MISSEDCALLS +
				// params[0];
				HttpResponse response = null;
				try {
					response = WebUtils.stringResponseGet(prescListUrl, MIntel
							.getInstance().getHttpContext(), WebUtils
							.createHttpClient(WebUtils.CONNECTION_TIMEOUT));

					final BufferedReader reader = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));

					String line;
					final StringBuilder resultBuilder = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						resultBuilder.append(line);
					}

					return resultBuilder.toString();
				} catch (final Exception e) {
				}
			} else if (params[1].equals("call")) {
				final String missedCallUrl = WebUtils.URL_PART_MISSEDCALLS + 5;
				Log.w("MISSED CALL", missedCallUrl);
				HttpResponse response = null;
				try {
					response = WebUtils.stringResponseGet(missedCallUrl, MIntel
							.getInstance().getHttpContext(), WebUtils
							.createHttpClient(WebUtils.CONNECTION_TIMEOUT));

					final BufferedReader reader = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));

					String line;
					final StringBuilder resultBuilder = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						resultBuilder.append(line);
					}

					return resultBuilder.toString();
				} catch (final Exception e) {
					Toast.makeText(MIntel.getAppContext(),
							"Web Response Error", Toast.LENGTH_SHORT).show();
				}
			}
		}
		return ret;
	}

	@Override
	protected void onPostExecute(final String result) {
		Log.w("PENDING", "WENT TO LISTENER WITH THIS:->" + result);
		if (listener != null) {
			listener.onMissedNotificationRetrieved(result, type);
		}
		super.onPostExecute(result);
	}

}
