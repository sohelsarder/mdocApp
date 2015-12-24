package com.mpower.mintel.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;

import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.utilities.WebUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageDownloadTask extends AsyncTask<String, Void, Bitmap>{

	private Context appContext;
	private ImageView view;
	private Bitmap bm;
	
	public ImageDownloadTask(Context context, ImageView view){
		this.appContext = context;
		this.view = view;
	}
	
	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		super.onPreExecute();
	}
	
	@Override
	protected Bitmap doInBackground(String... urls) {
		// get shared HttpContext so that authentication and cookies are retained.
/*        
		HttpContext localContext = MIntel.getInstance().getHttpContext();

        HttpClient httpclient = WebUtils.createHttpClient(WebUtils.CONNECTION_TIMEOUT);
        try{
        String url = urls[0];
        InputStream in = new java.net.URL(url).openStream();
        bm = BitmapFactory.decodeStream(in);
        in.close();
        }catch(Exception e){
        	
        }
*/      
		String src = urls[0];
		try {
            Log.e("src",""+src);
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            Log.e("Bitmap","returned");
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Exception",e.getMessage());
            return null;
        }
	}
	
	@Override
	protected void onPostExecute(Bitmap result) {
		view.setImageBitmap(result);
		//Toast.makeText(appContext, "Image Loaded", Toast.LENGTH_SHORT).show();
	}

}
