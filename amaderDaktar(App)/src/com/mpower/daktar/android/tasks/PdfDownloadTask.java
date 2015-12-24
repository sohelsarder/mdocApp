package com.mpower.daktar.android.tasks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.PdfDownloadListener;

public class PdfDownloadTask extends AsyncTask<String, Integer, String[]> {

	private PdfDownloadListener listener;

	public PdfDownloadTask(final Context context) {
	}

	public void setListener(final PdfDownloadListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		// Create Notification for Download
		super.onPreExecute();
		// pbarDialog = new ProgressDialog(appContext);
		// pbarDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		// pbarDialog.setTitle(appContext.getString(R.string.please_wait));
		// pbarDialog.setMessage("Downloading...");
		// pbarDialog.setCancelable(false);
		// pbarDialog.show();
	}

	@Override
	protected String[] doInBackground(final String... params) {
		String[] ret = null;
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		try {
			final URL url = new URL(params[0]);
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			// expect HTTP 200 OK, so we don't mistakenly save error report
			// instead of the file
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				return null;

			// this will be useful to display download percentage
			// might be -1: server did not report the length
			final int fileLength = connection.getContentLength();

			// download the file
			input = connection.getInputStream();
			final String FileOutputUrl = MIntel.PRESCRIPTION_PATH + "/"
					+ params[1];
			output = new FileOutputStream(FileOutputUrl);

			final byte data[] = new byte[4096];
			long total = 0;
			int count;
			while ((count = input.read(data)) != -1) {
				// allow canceling with back button
				if (isCancelled()) {
					input.close();
					return null;
				}
				total += count;
				// publishing the progress....
				if (fileLength > 0) {
					publishProgress((int) (total * 100 / fileLength));
				}
				output.write(data, 0, count);
			}
			ret = new String[2];
			ret[0] = FileOutputUrl;
			ret[1] = params[1];
		} catch (final Exception e) {
			return null;
		} finally {
			try {
				if (output != null) {
					output.close();
				}
				if (input != null) {
					input.close();
				}
			} catch (final IOException ignored) {
			}

			if (connection != null) {
				connection.disconnect();
			}
		}
		return ret;
	}

	@Override
	protected void onProgressUpdate(final Integer... values) {
		// TODO Notify Progress of file downloaded (Use the fileSize as Measure)
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(final String[] result) {
		// if (pbarDialog != null && pbarDialog.isShowing()) {
		// pbarDialog.dismiss();
		// }
		if (result != null && listener != null) {
			listener.onDownloadCompleteListener(result[0], result[1]);
		}
		super.onPostExecute(result);

	}

}
