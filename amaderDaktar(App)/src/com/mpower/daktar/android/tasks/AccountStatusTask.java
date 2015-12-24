package com.mpower.daktar.android.tasks;

import org.apache.http.HttpResponse;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.AccountStatusChangedListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class AccountStatusTask extends AsyncTask<String, Void, String> {

	AccountStatusChangedListener listener;
	
	public AccountStatusTask(AccountStatusChangedListener listener) {
		this.listener = listener;
	}
	
	@Override
	protected String doInBackground(String... params) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String userBalanceUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		userBalanceUrl += WebUtils.URL_PART_BALANCE + "rmpId=" + params[0];

		HttpResponse response;
		try {
			response = WebUtils.stringResponseGet(userBalanceUrl, MIntel
					.getInstance().getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));

			return WebUtils.getStringFromResponse(response);
		} catch (Exception e) {
			// Silent Crash
		}

		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		listener.onStatusChanged(result);
	}

}
