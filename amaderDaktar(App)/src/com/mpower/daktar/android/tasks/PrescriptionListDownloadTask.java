package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.PrescriptionListDownloadListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

public class PrescriptionListDownloadTask extends
AsyncTask<String, String, String> {

	private final PrescriptionListDownloadListener listener;

	public PrescriptionListDownloadTask(
			final PrescriptionListDownloadListener listener) {
		this.listener = listener;
	}

	@Override
	protected String doInBackground(final String... params) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String downloadListUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		downloadListUrl += WebUtils.URL_PART_PRESCRIPTIONLIST + "?patId="
				+ params[0];
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(downloadListUrl, MIntel
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
			Toast.makeText(MIntel.getAppContext(), "Web Response Error",
					Toast.LENGTH_SHORT).show();
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(final String... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(final String result) {
		super.onPostExecute(result);
		listener.onPrescriptionListDownloaded(result);
	}

}
