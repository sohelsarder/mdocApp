package com.mpower.daktar.android.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.PdfDownloadListener;
import com.mpower.daktar.android.services.PushService;
import com.mpower.daktar.android.tasks.PdfDownloadTask;
import com.mpower.daktar.android.R;

public class DownloadActivity extends Activity implements PdfDownloadListener {

	private Button print;
	private TextView label;
	private ProgressBar progress;
	private String fileUrl;

	// intent action to trigger printing
	public static final String PRINT_ACTION = "org.androidprinting.intent.action.PRINT";

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);

		final Intent i = getIntent();
		Log.w("DOWNLOAD ACTIVITY", "Getting Intent");
		if (i != null) {
			final String fileUrl = i.getStringExtra("url");
			this.fileUrl = fileUrl;
			final String fileName = i.getStringExtra("name");
			Log.w("Download Parameters", "url =" + fileUrl + " Name ="
					+ fileName);
			if (fileUrl != null && fileName != null) {
				final PdfDownloadTask pdfDownloadTask = new PdfDownloadTask(
						getApplicationContext());
				pdfDownloadTask.setListener(this);
				pdfDownloadTask.execute(fileUrl, fileName);
			}
		} else {
			Log.w("DOWNLOAD ACTIVITY", "Failed. Intent Flawed");
			finish();
		}
		print = (Button) findViewById(R.id.print_button);
		//print.setEnabled(false);
		print.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View arg0) {
				if (fileUrl != null) {
					final WifiManager wifiManager = (WifiManager) DownloadActivity.this
							.getSystemService(Context.WIFI_SERVICE);
					if (wifiManager.isWifiEnabled() == false) {
						wifiManager.setWifiEnabled(true);
					}

					 Toast.makeText(getApplicationContext(),
					 "আপনার WiFi চালু করা হচ্ছে। অপেক্ষা করুন একটু।  ",
					 Toast.LENGTH_LONG).show();
					createCustomToast(R.drawable.ic_wifi_status,
							"আপনার WiFi চালু করা হচ্ছে। অপেক্ষা করুন একটু।  ");
/* HP Print App*/
					final File file = new File(fileUrl);
					final Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(file), "application/pdf");
					intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivityForResult(intent, 15);

/*Samsung Print App*/
/*					Intent intent = new Intent("com.sec.print.mobileprint.action.PRINT");   
					intent.putExtra("com.sec.print.mobileprint.extra.CONTENT", Uri.parse(fileUrl) );
					intent.putExtra("com.sec.print.mobileprint.extra.CONTENT_TYPE", "WEBPAGE_AUTO");
					intent.putExtra("com.sec.print.mobileprint.extra.OPTION_TYPE", "DOCUMENT_PRINT");
					intent.putExtra("com.sec.print.mobileprint.extra.JOB_NAME", "Prescription");
					startActivity(intent);
					DownloadActivity.this.startActivityForResult(intent, 15);
*/
				}
			}
		});

		progress = (ProgressBar) findViewById(R.id.progress_1);
		progress.setVisibility(View.VISIBLE);

		label = (TextView) findViewById(R.id.label_1);
		label.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.download, menu);
		return true;
	}

	@Override
	public void onDownloadCompleteListener(final String url, final String name) {
		progress.setVisibility(View.INVISIBLE);
		print.setEnabled(true);
		label.setText(name);
		fileUrl = url;
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (requestCode == 15) {
			final WifiManager wifiManager = (WifiManager) DownloadActivity.this
					.getSystemService(Context.WIFI_SERVICE);
			if (wifiManager.isWifiEnabled() == true) {
				wifiManager.setWifiEnabled(false);
			}

			createCustomToast(R.drawable.ic_wifi_status,
					"আপনার WiFi বন্ধ করা হচ্ছে। অপেক্ষা করুন একটু। ");
			final File f = new File(fileUrl);
			if (f.exists()) {
				f.delete();
			}

			final File ff = new File(MIntel.METADATA_PATH + "/session.tmp");
			Scanner sc;
			String rmpId = null;
			try {
				sc = new Scanner(ff);
				rmpId = sc.next();
				sc.close();
			} catch (final FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			final Intent ijk = new Intent(this, PushService.class);
			ijk.putExtra("rmpId", rmpId);
			startService(ijk);
			finish();
		}
	}

	private void createCustomToast(final int iconId, final String text) {

		final LayoutInflater inflater = getLayoutInflater();
		final View layout = inflater.inflate(R.layout.custom_toast,
				(ViewGroup) findViewById(R.id.toast_layout_root));

		final ImageView icon = (ImageView) layout.findViewById(R.id.toast_icon);
		icon.setImageDrawable(getApplicationContext().getResources()
				.getDrawable(iconId));

		final TextView msg = (TextView) layout.findViewById(R.id.toast_text);
		if (text != null) {
			msg.setText(text);
		} else {
			msg.setVisibility(View.GONE);
		}

		final Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(layout);
		toast.show();
	}

}
