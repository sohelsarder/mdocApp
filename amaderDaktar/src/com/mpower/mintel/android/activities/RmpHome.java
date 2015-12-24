package com.mpower.mintel.android.activities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.activities.MainMenuActivity.LauncherIcon;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.listeners.FollowUpListener;
import com.mpower.mintel.android.listeners.MissedNotificationListener;
import com.mpower.mintel.android.listeners.PatientLoginListener;
import com.mpower.mintel.android.preferences.PreferencesActivity;
import com.mpower.mintel.android.provider.FormsProviderAPI.FormsColumns;
import com.mpower.mintel.android.services.PushService;
import com.mpower.mintel.android.tasks.FollowUpCheckerTask;
import com.mpower.mintel.android.tasks.MissedNotificationTask;
import com.mpower.mintel.android.tasks.PatientLoginTask;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * Main Home Screen for the RMP Provides the RMP Information as well as some
 * shortcuts
 * 
 * @author Sadat Sakif Ahmed<sadat@mpower-social.com>
 * 
 * 
 * 
 */
public class RmpHome extends Activity implements PatientLoginListener,
		OnItemClickListener, MissedNotificationListener, FollowUpListener {

	private static final String t = "RmpHomeActivity";

	// menu options
	private static final int MENU_PREFERENCES = Menu.FIRST;
	// exit conditions
	private static boolean EXIT = true;

	private AlertDialog mAlertDialog;

	private static final int SCAN_REQUEST_CODE_PAT = 67;

	private TextView rmpName;
	private TextView rmpPhone;
	private TextView rmpAge;
	private TextView rmpGender;

	private String rmpId;

	private String patId;

	private String patName;

	private String patPhone;

	private String patAge;

	private String patGender;

	private String patImageUrl;
	
	private Intent service;
	private PushService serviceBind;

	private static final LauncherIcon[] HOME_ICONS = {
			new LauncherIcon(R.drawable.patient_register,
					R.string.register_patient),
			new LauncherIcon(R.drawable.doctor_appointment,
					R.string.appointment_doctor),
			new LauncherIcon(R.drawable.patient_record, R.string.patient_record),
			//new LauncherIcon(R.drawable.send_data, R.string.send_data),
			new LauncherIcon(R.drawable.manage_data, R.string.manage_data) };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// must be at the beginning of any activity that can be called from an
		// external intent
		Log.i(t, "Starting up, creating directories");
		try {
			MIntel.createMIntelDirs();
		} catch (RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		setContentView(R.layout.activity_rmp_home);
		setTitle("হোম স্ক্রীন");

		Intent i = getIntent();
		String rmp_name = i.getStringExtra("rmp_name");
		String rmp_id = i.getStringExtra("rmp_id");
		if (rmp_id != null)
			this.rmpId = rmp_id;
		else if (rmp_id == null)
			this.finish();

		String rmp_role = i.getStringExtra("rmp_role");
		String rmp_age = i.getStringExtra("rmp_age");
		String rmp_phone = i.getStringExtra("rmp_phone");

		rmpName = (TextView) findViewById(R.id.rmpName);
		if (rmp_name != null) {
			rmpName.setText("নাম :"+rmp_name);
			rmpName.setVisibility(View.VISIBLE);
		}
		rmpPhone = (TextView) findViewById(R.id.rmpPhone);
		if (rmp_phone != null) {
			rmpPhone.setText("ফোন :"+rmp_phone);
			rmpPhone.setVisibility(View.VISIBLE);
		}
		rmpAge = (TextView) findViewById(R.id.rmpAge);
		if (rmp_age != null) {
			rmpAge.setText("বয়স :"+rmp_age);
			rmpAge.setVisibility(View.VISIBLE);
		}
		rmpGender = (TextView) findViewById(R.id.rmpGender);
		if (rmp_role != null) {
			rmpGender.setText(rmp_role);
			rmpGender.setVisibility(View.INVISIBLE);
		}
		GridView gridview = (GridView) findViewById(R.id.rmp_dashboard_grid);
		gridview.setAdapter(new ImageAdapter(this));
		gridview.setOnItemClickListener(this);
		
		

		final MissedNotificationTask mnt = new MissedNotificationTask();
		mnt.setListener(this);
		
		final MissedNotificationTask mnt1 = new MissedNotificationTask();
		mnt1.setListener(this);
		
		final FollowUpCheckerTask follow = new FollowUpCheckerTask();
		follow.addListener(this);
		
		Handler missedNots = new Handler();
		Runnable missedPresNot = new Runnable() {
			
			@Override
			public void run() {

				mnt.execute(rmpId, "pres");
				mnt1.execute(rmpId, "call");
				follow.execute(rmpId);
			}
		};
		missedNots.postDelayed(missedPresNot, 3000);

		
		

		 service = new Intent(MIntel.getAppContext(), PushService.class);
		 service.putExtra("rmpId", rmpId);

		 startService(service);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SCAN_REQUEST_CODE_PAT) {
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
				if (contents != null) {
					PatientLoginTask loginTask = new PatientLoginTask();
					loginTask.setPatientLoginListener(RmpHome.this,
							RmpHome.this);
					loginTask.execute(contents);
				}
			} else {
				createErrorDialog("সঠিক QR কোড দেখান। এই কোডটি ভুল।", false);
				return;
			}
		}
	}

	private void startNextActivity() {
		Intent i = new Intent(getApplicationContext(), PatientHome.class);
		i.putExtra("rmp_id", rmpId);
		i.putExtra("pat_id", patId);
		i.putExtra("pat_name", patName);
		i.putExtra("pat_phone", patPhone);
		i.putExtra("pat_age", patAge);
		i.putExtra("pat_gender", patGender);
		i.putExtra("pat_image", patImageUrl);
		startActivity(i);
	}

	private Uri findFormUri(String formName) {
		@SuppressWarnings("deprecation")
		Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, null, null,
				null);
		Log.w("Form Count: ", "" + c.getCount());
		Uri formUri = null;
		if (c.moveToFirst()) {
			base: do {
				String name = c.getString(c
						.getColumnIndex(FormsColumns.DISPLAY_NAME));
				Log.d("Form Name: ", name);
				if (name.equals(formName)) {
					formUri = ContentUris
							.withAppendedId(
									com.mpower.mintel.android.provider.FormsProviderAPI.FormsColumns.CONTENT_URI,
									c.getLong(c
											.getColumnIndex(FormsColumns._ID)));
					break base;
				}
			} while (c.moveToNext());
		}

		// c.close();
		return formUri;
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onResume() {
		Log.w("RMP HOME", "On Resume Called");
		//Intent ijk = new Intent(this, PushService.class);
		//ijk.putExtra("rmpId", rmpId);
		//startService(ijk);
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_PREFERENCES, 0,
				getString(R.string.general_preferences)).setIcon(
				android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			Intent ig = new Intent(this, PreferencesActivity.class);
			startActivity(ig);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
		AlertDialog.Builder builder = new AlertDialog.Builder(RmpHome.this);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				if (shouldExit) {
					finish();
				}
				dialog.dismiss();
			}
		};
		builder.setPositiveButton(RmpHome.this.getString(R.string.ok),
				errorListener);
		mAlertDialog = builder.create();

		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(Html.fromHtml(errorMsg));
		mAlertDialog.setCancelable(false);
		mAlertDialog.show();
	}

	@Override
	public void loginResult(String json) {
		try {
			JSONObject data = new JSONObject(json);
			patName = data.getString("name");
			patAge = data.getString("age");
			patGender = data.getString("gender");
			patPhone = data.getString("phone");
			patImageUrl = data.getString("image");
			patId = data.getString("id");
			startNextActivity();
		} catch (JSONException e) {
			createErrorDialog(json, false);
		}

	}

	static class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		@Override
		public int getCount() {
			return HOME_ICONS.length;
		}

		@Override
		public LauncherIcon getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		static class ViewHolder {
			public ImageView icon;
			public TextView text;
		}

		// Create a new ImageView for each item referenced by the Adapter
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			ViewHolder holder;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				v = vi.inflate(R.layout.dashboard_icon, null);
				holder = new ViewHolder();
				holder.text = (TextView) v
						.findViewById(R.id.dashboard_icon_text);
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(MIntel.getInstance());
				String question_font = settings.getString(
						PreferencesActivity.KEY_FONT_SIZE,
						MIntel.DEFAULT_FONTSIZE);
				int mQuestionFontsize = Integer.valueOf(question_font);
				holder.text.setTextSize(mQuestionFontsize);
				holder.icon = (ImageView) v
						.findViewById(R.id.dashboard_icon_img);
				v.setTag(holder);
			} else {
				holder = (ViewHolder) v.getTag();
			}

			holder.icon.setImageResource(HOME_ICONS[position].imgId);

			holder.text.setText(HOME_ICONS[position].text);

			return v;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

		Uri formUri;
		switch (position) {
		//case 9:
		//	// New
		//	Intent iNew = new Intent(getApplicationContext(),
		//			FormChooserList.class);
		//	startActivity(iNew);
		//	break;
		case 0:
			// Register Patient
			formUri = findFormUri("Patient Registration");
			if (formUri != null)
				startActivity(new Intent(Intent.ACTION_EDIT, formUri));
			else
				Toast.makeText(RmpHome.this,
						"রেজিস্ট্রেশান ফর্ম পাওয়া যাচ্ছে না। ডাউনলোড করুন। ",
						Toast.LENGTH_SHORT).show();
			break;
		case 1:
			// Get Appointment
			formUri = findFormUri("Patient Session");
			if (formUri != null)
				startActivity(new Intent(Intent.ACTION_EDIT, formUri));
			else
				Toast.makeText(RmpHome.this,
						"আপইন্টমেন্ট ফর্ম পাওয়া যাচ্ছে না। ডাউনলোড করুন। ",
						Toast.LENGTH_SHORT).show();
			break;
		case 2:
			// View Patient Records
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(intent, SCAN_REQUEST_CODE_PAT);
			break;
		case 3:
			// Manage Data
			Intent i = new Intent(RmpHome.this, MainMenuActivity.class);
			startActivity(i);
			break;
		case 4:
			// Send Data
			Intent iSend = new Intent(getApplicationContext(),
					InstanceUploaderList.class);
			startActivity(iSend);
			break;
		}
	}

	private void showAlertDialog(String title, String message, int iconId) {

		AlertDialog.Builder adb = new AlertDialog.Builder(RmpHome.this);

		adb.setTitle(title);
		adb.setMessage(message);
		adb.setIcon(iconId);
		adb.setPositiveButton("হ্যাঁ", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (serviceBind != null)
					serviceBind.onDestroy();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});

		adb.setNegativeButton(" না",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});

		adb.show();
	}

	@Override
	public void onBackPressed() {
		showAlertDialog("সত্যি \"Log out\" করবেন?",
				"আপনি কি সত্যি \"Log out\" করতে চান? \"Log out\" করতে চাইলে \"হ্যাঁ\" চাপুন আর না চাইলে \"না\" চাপুন",
				R.drawable.ic_exclamation);
	}

	@Override
	public void onMissedNotificationRetrieved(String json, String type) {
		Intent i = new Intent(RmpHome.this, NotificationActivity.class);
		if (type.equals("pres")){
		try{
		JSONObject jsonO = new JSONObject(json);
		JSONArray urls = jsonO.getJSONArray("urls");
		JSONArray names = jsonO.getJSONArray("names");
		Log.w("PENDING", "ARRAY CONTENT:" + urls.toString());
		i.putExtra("intent", 2);
		i.putExtra("urls", urls.toString());
		i.putExtra("names", names.toString());
		RmpHome.this.startActivity(i);
		}catch (JSONException e){
			
		}
		}else if (type.equals("call")){
			Log.w("RETURN OUTPUT", json);
			try{
			JSONObject jsonO = new JSONObject(json);
			JSONArray docName = jsonO.getJSONArray("docName");
			JSONArray patName = jsonO.getJSONArray("patientName");
			JSONArray callCount = jsonO.getJSONArray("callCount");
			i.putExtra("intent", 3);
			i.putExtra("docs", docName.toString());
			i.putExtra("pats", patName.toString());
			i.putExtra("call", callCount.toString());
			RmpHome.this.startActivity(i);
			}catch (JSONException e){
				
			}
		}
	}

	@Override
	public void onFollowUpReceived(String response) {
		Intent i = new Intent(RmpHome.this, NotificationActivity.class);
		try{
		JSONObject json = new JSONObject(response);
		}catch (JSONException jsone){}
	}
}