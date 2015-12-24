package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.VisitValidationListener;
import com.mpower.daktar.android.utilities.WebUtils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.AsyncTask;

public class VisitValidationTask extends AsyncTask<String, Void, String> {

	private VisitValidationListener listener;
	private Context context;

	private AlertDialog progress;
	
	public VisitValidationTask(VisitValidationListener listener, Context context) {
		this.listener = listener;
		this.context =context;
	}

	@Override
	protected void onPreExecute() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("অপেক্ষা করুন");
		builder.setMessage("আপনার তথ্য যাচাই করা হচ্ছে। কিছুক্ষণ অপেক্ষা করুন");
		progress = builder.create();
		progress.setCancelable(false);
		progress.show();
	}
	
	@Override
	protected String doInBackground(String... params) {
		try {
			HttpResponse response = WebUtils.stringResponseGet(
					WebUtils.URL_PART_FOLLOWUPCHECK+params[1], MIntel.getInstance()
							.getHttpContext(), WebUtils
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
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		progress.dismiss();
		listener.isVisitValid(result);
	}

}
