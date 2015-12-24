package com.mpower.daktar.android.tasks;

import org.apache.http.HttpResponse;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.DoctorListListener;
import com.mpower.daktar.android.utilities.WebUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;

public class DoctorListRetrievalTask extends AsyncTask<String, Void, String> {

	private DoctorListListener listener;
	private Context context;
	private AlertDialog dialog;
	
	public DoctorListRetrievalTask(DoctorListListener listener, Context context) {
		this.listener = listener;
		this.context = context;
		}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("অপেক্ষা করুন");
		builder.setMessage("ডাক্তার এর লিস্ট নামানো হচ্ছে");
		dialog = builder.create();
		dialog.setCancelable(false);
		dialog.show();
	}
	
	@Override
	protected String doInBackground(String... params) {
		try {
			HttpResponse response = WebUtils.stringResponseGet(
					WebUtils.URL_PART_DOCTORLIST, MIntel.getInstance()
							.getHttpContext(), WebUtils
							.createHttpClient(WebUtils.CONNECTION_TIMEOUT));
			
			return WebUtils.getStringFromResponse(response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(String result) {
		dialog.dismiss();
		listener.onResponseReceived(result);
	}

}
