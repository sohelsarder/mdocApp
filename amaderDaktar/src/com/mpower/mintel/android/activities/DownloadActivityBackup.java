package com.mpower.mintel.android.activities;

import java.io.File;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.R.layout;
import com.mpower.mintel.android.R.menu;
import com.mpower.mintel.android.listeners.PdfDownloadListener;
import com.mpower.mintel.android.tasks.PdfDownloadTask;

import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadActivityBackup extends Activity implements PdfDownloadListener{

	private Button print;
	private TextView label;
	private ProgressBar progress;
	private String fileUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);

		Intent i = getIntent();
		Log.w("DOWNLOAD ACTIVITY", "Getting Intent");
		if (i != null) {
			String fileUrl = i.getStringExtra("url");
			String fileName = i.getStringExtra("name");
			Log.w("Download Parameters", "url ="+fileUrl+" Name ="+fileName);
			if (fileUrl != null && fileName != null) {
				PdfDownloadTask pdfDownloadTask = new PdfDownloadTask(
						getApplicationContext());
				pdfDownloadTask.setListener(this);
				pdfDownloadTask.execute(fileUrl, fileName);
			}
		}else{
			Log.w("DOWNLOAD ACTIVITY", "Failed. Intent Flawed");
			finish();
		}
		print = (Button) findViewById(R.id.print_button);
		print.setEnabled(false);
		print.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (fileUrl != null){
					WifiManager wifiManager = (WifiManager) DownloadActivityBackup.this.getSystemService(Context.WIFI_SERVICE);
					if (wifiManager.isWifiEnabled() == false)
						wifiManager.setWifiEnabled(true);
					/*
					 * 
					 Intent intent = new Intent("com.sec.print.mobileprint.action.PRINT");
					 Uri uri = Uri.parse("http://www.samsung.com");
					 intent.putExtra("com.sec.print.mobileprint.extra.CONTENT", uri );
					 intent.putExtra("com.sec.print.mobileprint.extra.CONTENT_TYPE", "WEBPAGE");
					 intent.putExtra("com.sec.print.mobileprint.extra.OPTION_TYPE", "DOCUMENT_PRINT");
					 intent.putExtra("com.sec.print.mobileprint.extra.JOB_NAME", "Untitled");
					 * 
					 * */
					
					
					 File file = new File(fileUrl);
				        Intent intent = new Intent(Intent.ACTION_VIEW);
				        intent.setDataAndType(Uri.fromFile(file),"application/pdf");
				        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				        startActivityForResult(intent, 15);
				}
			}
		});
		
		progress = (ProgressBar) findViewById(R.id.progress_1);
		progress.setVisibility(View.VISIBLE);
		
		label = (TextView) findViewById(R.id.label_1);
		label.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.download, menu);
		return true;
	}

	@Override
	public void onDownloadCompleteListener(String url, String name) {
			progress.setVisibility(View.INVISIBLE);
			print.setEnabled(true);
			label.setText(name);
			fileUrl = url;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 15){
			WifiManager wifiManager = (WifiManager) DownloadActivityBackup.this.getSystemService(Context.WIFI_SERVICE); 
			if (wifiManager.isWifiEnabled() == true)
			wifiManager.setWifiEnabled(false);
			File f = new File(fileUrl);
			if (f.exists()){
				f.delete();
			}
			this.finish();
		}
	}

}
