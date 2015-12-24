package com.mpower.daktar.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class ImageDownloadTask extends AsyncTask<String, Void, Bitmap> {

	private final ImageView view;

	public ImageDownloadTask(final Context context, final ImageView view) {
		this.view = view;
	}

	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
	}

	@Override
	protected Bitmap doInBackground(final String... urls) {
		// get shared HttpContext so that authentication and cookies are
		// retained.
		/*
		 * HttpContext localContext = MIntel.getInstance().getHttpContext();
		 *
		 * HttpClient httpclient =
		 * WebUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT); try{ String
		 * url = urls[0]; InputStream in = new java.net.URL(url).openStream();
		 * bm = BitmapFactory.decodeStream(in); in.close(); }catch(Exception e){
		 *
		 * }
		 */
		final String src = urls[0];
		try {
			Log.e("src", "" + src);
			final URL url = new URL(src);
			final HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.connect();
			final InputStream input = connection.getInputStream();
			final Bitmap myBitmap = BitmapFactory.decodeStream(input);
			Log.e("Bitmap", "returned");
			return myBitmap;
		} catch (final IOException e) {
			e.printStackTrace();
			Log.e("Exception", e.getMessage());
			return null;
		}
	}

	@Override
	protected void onPostExecute(final Bitmap result) {
		view.setImageBitmap(result);
		// Toast.makeText(appContext, "Image Loaded",
		// Toast.LENGTH_SHORT).show();
	}

}
