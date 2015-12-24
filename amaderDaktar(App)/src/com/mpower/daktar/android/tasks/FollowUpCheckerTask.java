package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import android.os.AsyncTask;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.FollowUpListener;
import com.mpower.daktar.android.utilities.WebUtils;

public class FollowUpCheckerTask extends AsyncTask<String, Void, String> {

	FollowUpListener listener;

	public void addListener(final FollowUpListener listener) {
		this.listener = listener;
	}

	@Override
	protected String doInBackground(final String... arg0) {

		final String followUpUrl = WebUtils.URL_PART_FOLLOWUP + arg0[0];
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(followUpUrl, MIntel
					.getInstance().getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));

			final BufferedReader br = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			final StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			System.out.println(sb.toString());
			return sb.toString();
		} catch (final Exception e) {

		}
		return null;
	}

	@Override
	protected void onPostExecute(final String result) {
		listener.onFollowUpReceived(result);
		super.onPostExecute(result);
	}
}
