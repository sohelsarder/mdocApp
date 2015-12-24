package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.AccountResponseListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class AccountRechargeTask extends AsyncTask<String, Void, String>{

	private AccountResponseListener listener;
	
	public AccountRechargeTask(AccountResponseListener listener) {
		this.listener =listener;
	}
	
	@Override
	protected String doInBackground(String... params) {
		if (params.length < 2)
			return null;
		
		String rmpId = params[0];
		String trxid = params[1];
		System.out.println(""+ rmpId +" "+ trxid);
		if (rmpId == null || trxid == null)
			return null;
	
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		
		String accountUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));
		
		accountUrl += WebUtils.URL_PART_ACCOUNTS;
		accountUrl += "rmpid="+rmpId;
		accountUrl += "&trxid="+trxid;
		
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(accountUrl, MIntel
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
	protected void onPostExecute(String result) {
		System.out.println(""+result);
		this.listener.onAccountResponded(result);
	}

}
