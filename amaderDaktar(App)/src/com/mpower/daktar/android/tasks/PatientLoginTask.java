package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.PatientLoginListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

public class PatientLoginTask extends AsyncTask<String, Void, String> {

	private PatientLoginListener listener;
	private Context appContext;
	private ProgressDialog pbarDialog;
	private int type;

	public void setPatientLoginListener(final Context context,
			final PatientLoginListener listener, final int type) {
		this.listener = listener;
		appContext = context;
		this.type = type;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		pbarDialog = new ProgressDialog(appContext);
		pbarDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pbarDialog.setTitle(appContext.getString(R.string.please_wait));
		pbarDialog.setMessage("Logging in...");
		pbarDialog.setCancelable(false);
		pbarDialog.show();
	}

	@Override
	protected String doInBackground(final String... params) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String loginUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		loginUrl += WebUtils.URL_PART_PATIENTLOGIN + "?code=" + params[0];
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(loginUrl, MIntel
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
	protected void onPostExecute(final String result) {
		super.onPostExecute(result);
		if (pbarDialog != null && pbarDialog.isShowing()) {
			pbarDialog.dismiss();
		}
		if (type == 1){
		listener.loginResult(result);
		}else if (type == 2){
		listener.loginDetail(result);	
		}
	}

}
