package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.AccountinfoRetrieveListener;
import com.mpower.daktar.android.utilities.WebUtils;

import android.os.AsyncTask;

public class AccountInfoRetrieveTask extends AsyncTask<String, Void, String> {

	private AccountinfoRetrieveListener listener;

	public AccountInfoRetrieveTask(AccountinfoRetrieveListener listener) {
		this.listener = listener;
	}

	@Override
	protected String doInBackground(String... params) {
		final String unitcost = WebUtils.URL_PART_UNITCOSTS + params[0];
		try {
			HttpResponse response = WebUtils.stringResponseGet(unitcost, MIntel
					.getInstance().getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));

			return WebUtils.getStringFromResponse(response);
		} catch (Exception e) {
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		this.listener.onAccountInfoRetrieved(result);
	}

}
