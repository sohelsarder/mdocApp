package com.mpower.mintel.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.listeners.PrescriptionListDownloadListener;
import com.mpower.mintel.android.preferences.PreferencesActivity;
import com.mpower.mintel.android.utilities.WebUtils;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PrescriptionListDownloadTask extends AsyncTask<String, String, String>{
	
	private PrescriptionListDownloadListener listener;
	
	public PrescriptionListDownloadTask(PrescriptionListDownloadListener listener){
		this.listener = listener;
	}
	
	@Override
	protected String doInBackground(String... params) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String downloadListUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		downloadListUrl += WebUtils.URL_PART_PRESCRIPTIONLIST+ "?patId="+params[0];
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(downloadListUrl, MIntel
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
	protected void onProgressUpdate(String... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		listener.onPrescriptionListDownloaded(result);
	}

}
