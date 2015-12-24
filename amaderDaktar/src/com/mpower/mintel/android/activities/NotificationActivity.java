package com.mpower.mintel.android.activities;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.listeners.PdfDownloadListener;
import com.mpower.mintel.android.services.PushService;
import com.mpower.mintel.android.tasks.MissedCallResetTask;
import com.mpower.mintel.android.utilities.WebUtils;

public class NotificationActivity extends Activity implements
		PdfDownloadListener {

	TextView title;
	TextView message;
	TextView cancel;
	TextView ok;

	ListView list;
	Button confirm;

	String[] url;
	String[] names;
	ArrayList<String> items;

	NotificationManager manager;

	boolean isDialog = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme_Translucent_NoTitleBar);
		Intent data = getIntent();
		Log.w("DIALOG", "Activity Started " + data.getIntExtra("intent", -1));
		if (data != null) {
			String title = data.getStringExtra("title");
			String message = data.getStringExtra("message");
			String confirm = data.getStringExtra("confirm");
			String cancel = data.getStringExtra("cancel");
			String[] args = data.getStringArrayExtra("args");
			int type = data.getIntExtra("intent", -1);
			if (type == -1)
				finish();
			if (type == 0 || type == 1) {
				if (title == null || message == null || confirm == null
						|| cancel == null) {
					Log.w("DIALOG", "Activity Ended Missing Values");
					finish();
				}
				manager = (NotificationManager) MIntel.getAppContext()
						.getSystemService(Context.NOTIFICATION_SERVICE);
				showAlertDialog(title, message, confirm, cancel, type, args);
			} else if (type == 2) {
				parseArrays(data);
				showPrescriptionList(items);
			} else if (type == 3) {
				parseMissedCalls(data);
				showMissedCallList();
			}
		}
	}

	private void parseMissedCalls(Intent data) {
		try {
			JSONArray doctor = new JSONArray(data.getStringExtra("docs"));
			JSONArray patient = new JSONArray(data.getStringExtra("pats"));
			JSONArray calls = new JSONArray(data.getStringExtra("call"));
			items = new ArrayList<String>();
			for (int i = 0; i < doctor.length(); i++) {
				items.add(doctor.getString(i).replaceAll("_", " ") + " কল দিয়েছিলো "
						+ patient.getString(i).replaceAll("_", " ") + " কে  " + calls.getInt(i)
						+ " বার");

			}
		} catch (JSONException e) {
		}
	}

	private void parseArrays(Intent intent) {
		try {
			JSONArray urls = new JSONArray(intent.getStringExtra("urls"));
			JSONArray names = new JSONArray(intent.getStringExtra("names"));
			if (urls.length() < 1) {
				NotificationActivity.this.finish();
			}
			this.url = new String[urls.length()];
			this.names = new String[urls.length()];
			this.items = new ArrayList<String>();
			for (int i = 0; i < url.length; i++) {
				this.url[i] = urls.getString(i);
				this.names[i] = names.getString(i);
				this.items.add("এই প্রেস্ক্রিপশান ডাউনলোড করুন : " + this.names[i]);
			}
		} catch (JSONException je) {

		}
	}

	private void showMissedCallList() {
		setFinishOnTouchOutside(false);
		setTheme(android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
		this.setContentView(R.layout.activity_notification);
		title = (TextView) findViewById(R.id.title);
		title.setText("আপনি ডাক্তার এর কল মিস করেছেন ");
		title.setTextSize(32);
		list = (ListView) findViewById(R.id.prescriptionList);
		list.setAdapter(new ArrayAdapter<String>(NotificationActivity.this,
				android.R.layout.simple_selectable_list_item, items));
		confirm = (Button) findViewById(R.id.confirm);
		confirm.setText("শেষ");
		confirm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				MissedCallResetTask mst = new MissedCallResetTask();
				mst.execute();
				NotificationActivity.this.finish();
			}
		});

	}

	private void showPrescriptionList(final ArrayList<String> items) {
		setFinishOnTouchOutside(false);
		setTheme(android.R.style.Theme_Holo_Dialog_NoActionBar);
		this.setContentView(R.layout.activity_notification);
		RelativeLayout back = (RelativeLayout) findViewById(R.id.not_rel_lay);
		//back.setBackgroundColor(Color.rgb(190, 190, 190));
		Log.w("NOTIFICATION ACTIVITY", "CREATING PRESCRIPTION LIST");
		title = (TextView) findViewById(R.id.title);
		title.setText("আপনার জন্য অপেক্ষমাণ প্রেস্ক্রিপশান");
		title.setTextSize(32);
		title.setTextColor(Color.rgb(255, 100, 100));
		list = (ListView) findViewById(R.id.prescriptionList);
		list.setAdapter(new ArrayAdapter<String>(NotificationActivity.this,
				android.R.layout.simple_selectable_list_item, items));
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				arg1.setSelected(true);
				Log.w("POSITION INDEXING", "" + arg2);
				ArrayAdapter<String> adapter = (ArrayAdapter<String>) list
						.getAdapter();
				Intent i = new Intent(getApplicationContext(), DownloadActivity.class);
				i.putExtra("url", url[arg2]);
				i.putExtra("name", names[arg2]);

				adapter.remove(items.get(arg2));
				adapter.notifyDataSetChanged();
				
				NotificationActivity.this.startActivity(i);
				if (adapter.isEmpty())
					NotificationActivity.this.finish();
			}
		});
		confirm = (Button) findViewById(R.id.confirm);
		confirm.setText("এখন না");
		confirm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				NotificationActivity.this.finish();
			}
		});

	}

	private void showAlertDialog(String title, String message, String confirm,
			String cancel, final int type, final String[] args) {

		AlertDialog.Builder adb = new AlertDialog.Builder(
				NotificationActivity.this);

		adb.setTitle(title);
		adb.setMessage(message);
		adb.setCancelable(false);
		if (type == 0)
			adb.setIcon(R.drawable.ic_notification_call);
		else if (type == 1)
			adb.setIcon(R.drawable.ic_notification_pres);
		adb.setPositiveButton(confirm, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Log.w("NOTIFICATION ACTIVITY", "Click Occured : " + type);
				Intent in = null;
				if (type == 0) {
					in = new Intent(NotificationActivity.this,
							DownloadActivity.class);
					in.putExtra("url", args[0]);
					in.putExtra("name", args[1]);
					manager.cancel(PushService.NOTIFICATION_PRESCRIPTION);
				} else if (type == 1) {
					in = new Intent("android.intent.action.MAIN");
					in.setComponent(ComponentName
							.unflattenFromString("com.android.chrome/com.android.chrome.Main"));
					in.addCategory("android.intent.category.LAUNCHER");
					String url = WebUtils.RMP_CALL_URL + args[0] + "/"
							+ args[1] + "/" + args[2];
					in.setData(Uri.parse(url));
					manager.cancel(PushService.NOTIFICATION_CALL);
				} else if (type == -1) {
					Log.w("NOTIFICATION ACTIVITY", "AHAAA!!!!");
				} else {
					Log.w("NOTIFICATION ACTIVITY", "AHAAA!!!!AHAA!!");
				}
				if (in != null) {
					in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					MIntel.getAppContext().startActivity(in);
				}
				NotificationActivity.this.finish();
			}
		});
		adb.show();
	}

	@Override
	public void onDownloadCompleteListener(String fileUrl, String fileName) {
		Toast.makeText(NotificationActivity.this,
				fileName + "- Download Complete.", Toast.LENGTH_LONG).show();
	}

}
