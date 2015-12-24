package com.mpower.mintel.android.tasks;

import org.apache.http.HttpResponse;

import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.utilities.WebUtils;

import android.os.AsyncTask;
import android.util.Log;

public class MissedCallResetTask extends AsyncTask<Void, Void, Void>{

	@Override
	protected Void doInBackground(Void... params) {
		String missedCallUrl = WebUtils.URL_PART_MISSEDCALLS_DELETE + 5;
		Log.w("MISSED CALL", missedCallUrl);
		HttpResponse response = null;
		try {
			response = WebUtils.stringResponseGet(missedCallUrl, MIntel
					.getInstance().getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));
		}catch (Exception e){}
		return null;
	}
	
}
