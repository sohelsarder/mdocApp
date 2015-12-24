package com.mpower.daktar.android.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import com.mpower.daktar.android.activities.PictureUpload;
import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PictureUploadTask extends AsyncTask<String, Void, String>{

	PictureUpload context;
	private AlertDialog progress;
	public PictureUploadTask(PictureUpload pictureUpload) {
		this.context = pictureUpload;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		AlertDialog.Builder builder =new AlertDialog.Builder(context);
		builder.setTitle("কিছুক্ষণ অপেক্ষা করুন");
		builder.setMessage("আপনার ছবি পাঠানো হচ্ছে");
		builder.setCancelable(false);
		progress = builder.create();
		progress.show();
	}
	
	@Override
	protected String doInBackground(String... params) {
		File uploadee = new File(params[0]);
		System.out.println(""+uploadee.getAbsolutePath());
		String filename = uploadee.getName();
		
		long byteCount = 0L;
		
		final MultipartEntity entity = new MultipartEntity();
		final FileBody body = new FileBody(uploadee, "image/jpeg");
		byteCount += uploadee.length();
		
		try{
		entity.addPart("rmpId", new StringBody(params[1]));
		entity.addPart("patId", new StringBody(params[2]));
		}catch(Exception e){
			// Blah blah blah
		}
		if (byteCount < 8000000L){
			entity.addPart(filename, body);
		}
		
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String uploadUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));
		uploadUrl += WebUtils.URL_PART_UPLOADPIC;
		
		HttpPost post = new HttpPost(uploadUrl);
		post.setEntity(entity);
		
		HttpResponse response = null;
		try {
			HttpClient httpclient  = WebUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT);
			HttpContext localContext = MIntel.getInstance().getHttpContext();
			response = httpclient.execute(post, localContext);
			
			final int responseCode = response.getStatusLine()
					.getStatusCode();

			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));

			String line;
			final StringBuilder resultBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				resultBuilder.append(line);
			}

			return resultBuilder.toString();
		} catch (final Exception e) {
			Log.w("Web Response Error", e.getMessage());
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		progress.dismiss();
		super.onPostExecute(result);
	}
	
}
