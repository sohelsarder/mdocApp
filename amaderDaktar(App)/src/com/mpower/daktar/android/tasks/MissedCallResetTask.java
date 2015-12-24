package com.mpower.daktar.android.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.utilities.WebUtils;

public class MissedCallResetTask extends AsyncTask<Void, Void, Void> {

	@Override
	protected Void doInBackground(final Void... params) {
		final String missedCallUrl = WebUtils.URL_PART_MISSEDCALLS_DELETE + 5;
		Log.w("MISSED CALL", missedCallUrl);
		try {
			WebUtils.stringResponseGet(missedCallUrl, MIntel.getInstance()
					.getHttpContext(), WebUtils
					.createHttpClient(WebUtils.CONNECTION_TIMEOUT));
		} catch (final Exception e) {
		}
		return null;
	}

}
