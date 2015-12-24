package com.mpower.mintel.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.activities.LoginActivity;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.listeners.PatientLoginListener;
import com.mpower.mintel.android.preferences.PreferencesActivity;
import com.mpower.mintel.android.utilities.WebUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PatientLoginTask extends AsyncTask<String, Void, String> {

	private PatientLoginListener listener;
	private Context appContext;
	private ProgressDialog pbarDialog;
	
	public void setPatientLoginListener(Context context, PatientLoginListener listener){
		this.listener = listener;
		this.appContext = context;
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
	protected String doInBackground(String... params) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String loginUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		loginUrl += WebUtils.URL_PART_PATIENTLOGIN + "?code="+params[0];
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(loginUrl, MIntel
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
		} catch (Exception e) {
			Toast.makeText(MIntel.getAppContext(), "Web Response Error",
					Toast.LENGTH_SHORT).show();
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		if (pbarDialog != null && pbarDialog.isShowing()) {
			pbarDialog.dismiss();
		}
		
		listener.loginResult(result);
	}

}
