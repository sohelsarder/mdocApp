package com.mpower.mintel.android.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.R.layout;
import com.mpower.mintel.android.R.menu;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.listeners.PdfDownloadListener;
import com.mpower.mintel.android.services.PushService;
import com.mpower.mintel.android.tasks.PdfDownloadTask;

import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadActivity extends Activity implements PdfDownloadListener {

	private Button print;
	private TextView label;
	private ProgressBar progress;
	private String fileUrl;

	// Send 2 Printer package name
	private static final String PACKAGE_NAME = "com.hp.android.print";
	// intent action to trigger printing
	public static final String PRINT_ACTION = "org.androidprinting.intent.action.PRINT";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);

		Intent i = getIntent();
		Log.w("DOWNLOAD ACTIVITY", "Getting Intent");
		if (i != null) {
			String fileUrl = i.getStringExtra("url");
			String fileName = i.getStringExtra("name");
			Log.w("Download Parameters", "url =" + fileUrl + " Name ="
					+ fileName);
			if (fileUrl != null && fileName != null) {
				PdfDownloadTask pdfDownloadTask = new PdfDownloadTask(
						getApplicationContext());
				pdfDownloadTask.setListener(this);
				pdfDownloadTask.execute(fileUrl, fileName);
			}
		} else {
			Log.w("DOWNLOAD ACTIVITY", "Failed. Intent Flawed");
			finish();
		}
		print = (Button) findViewById(R.id.print_button);
		print.setEnabled(false);
		print.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (fileUrl != null) {
					WifiManager wifiManager = (WifiManager) DownloadActivity.this
							.getSystemService(Context.WIFI_SERVICE);
					if (wifiManager.isWifiEnabled() == false)
						wifiManager.setWifiEnabled(true);

					//Toast.makeText(getApplicationContext(),
					//		"আপনার WiFi চালু করা হচ্ছে। অপেক্ষা করুন একটু।  ",
					//		Toast.LENGTH_LONG).show();
					createCustomToast(R.drawable.ic_wifi_status, "আপনার WiFi চালু করা হচ্ছে। অপেক্ষা করুন একটু।  ");
					
					File file = new File(fileUrl);
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(file), "application/pdf");
					intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivityForResult(intent, 15);

					DownloadActivity.this.startActivityForResult(intent, 15);
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
		if (requestCode == 15) {
			WifiManager wifiManager = (WifiManager) DownloadActivity.this
					.getSystemService(Context.WIFI_SERVICE);
			if (wifiManager.isWifiEnabled() == true)
				wifiManager.setWifiEnabled(false);

			createCustomToast(R.drawable.ic_wifi_status, "আপনার WiFi বন্ধ করা হচ্ছে। অপেক্ষা করুন একটু। ");
			File f = new File(fileUrl);
			if (f.exists()) {
				f.delete();
			}

			File ff = new File(MIntel.METADATA_PATH + "/session.tmp");
			Scanner sc;
			String rmpId = null;
			try {
				sc = new Scanner(ff);
				rmpId = sc.next();
				sc.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Intent ijk = new Intent(this, PushService.class);
			ijk.putExtra("rmpId", rmpId);
			startService(ijk);
			this.finish();
		}
	}

	private void createCustomToast(int iconId, String text) {

		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.custom_toast,
		                               (ViewGroup) findViewById(R.id.toast_layout_root));

		ImageView icon = (ImageView) layout.findViewById(R.id.toast_icon);
		icon.setImageDrawable(getApplicationContext().getResources().getDrawable(iconId));
		
		TextView msg = (TextView) layout.findViewById(R.id.toast_text);
		if (text != null)
		msg.setText(text);
		else
			msg.setVisibility(View.GONE);

		
		Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM, 0, 0);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}

}
