package com.mpower.daktar.android.activities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mpower.daktar.android.activities.MainMenuActivity.LauncherIcon;
import com.mpower.daktar.android.adapters.DoctorListAdapter;
import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.DoctorListListener;
import com.mpower.daktar.android.listeners.MissedNotificationListener;
import com.mpower.daktar.android.listeners.PatientLoginListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.provider.FormsProviderAPI.FormsColumns;
import com.mpower.daktar.android.services.PushService;
import com.mpower.daktar.android.tasks.DoctorListRetrievalTask;
import com.mpower.daktar.android.tasks.MissedNotificationTask;
import com.mpower.daktar.android.tasks.PatientLoginTask;
import com.mpower.daktar.android.R;

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
		OnItemClickListener, MissedNotificationListener, DoctorListListener {

	private static final String t = "RmpHomeActivity";

	// menu options
	private static final int MENU_PREFERENCES = Menu.FIRST;
	// exit conditions
	private static boolean EXIT = true;

	private AlertDialog mAlertDialog;

	private static final int SCAN_REQUEST_CODE_PAT = 67;
	private static final int SCAN_REQUEST_CODE_IMG = 69;

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
	
	private String rmpAcc;

	private Intent service;
	private PushService serviceBind;

	private static final LauncherIcon[] HOME_ICONS = {
			new LauncherIcon(R.drawable.patient_register,
					R.string.register_patient),
			new LauncherIcon(R.drawable.doctor_appointment,
					R.string.appointment_doctor),
			new LauncherIcon(R.drawable.patient_record, R.string.patient_record),
			// new LauncherIcon(R.drawable.send_data, R.string.send_data),
			new LauncherIcon(R.drawable.manage_data, R.string.manage_data),
			new LauncherIcon(R.drawable.pic_attach, R.string.pic_upload),
			new LauncherIcon(R.drawable.follow_up, R.string.follow_up),
			new LauncherIcon(R.drawable.accounting, R.string.accounts),
			new LauncherIcon(R.drawable.doc_list, R.string.doctor_list_text)
			};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// must be at the beginning of any activity that can be called from an
		// external intent
		Log.i(t, "Starting up, creating directories");
		try {
			MIntel.createMIntelDirs();
		} catch (final RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		setContentView(R.layout.activity_rmp_home);
		setTitle("হোম স্ক্রীন");

		final Intent i = getIntent();
		final String rmp_name = i.getStringExtra("rmp_name");
		final String rmp_id = i.getStringExtra("rmp_id");
		rmpAcc = i.getStringExtra("rmp_acc");
		if (rmp_id != null) {
			rmpId = rmp_id;
		} else if (rmp_id == null) {
			finish();
		}

		final String rmp_role = i.getStringExtra("rmp_role");
		final String rmp_age = i.getStringExtra("rmp_age");
		final String rmp_phone = i.getStringExtra("rmp_phone");

		rmpName = (TextView) findViewById(R.id.rmpName);
		if (rmp_name != null) {
			rmpName.setText("নাম :" + rmp_name);
			rmpName.setVisibility(View.VISIBLE);
		}
		rmpPhone = (TextView) findViewById(R.id.rmpPhone);
		if (rmp_phone != null) {
			rmpPhone.setText("ফোন :" + rmp_phone);
			rmpPhone.setVisibility(View.VISIBLE);
		}
		rmpAge = (TextView) findViewById(R.id.rmpAge);
		if (rmp_age != null) {
			rmpAge.setText("বয়স :" + rmp_age);
			rmpAge.setVisibility(View.VISIBLE);
		}
		rmpGender = (TextView) findViewById(R.id.rmpGender);
		if (rmp_role != null) {
			rmpGender.setText(rmp_role);
			rmpGender.setVisibility(View.INVISIBLE);
		}
		final GridView gridview = (GridView) findViewById(R.id.rmp_dashboard_grid);
		gridview.setAdapter(new ImageAdapter(this));
		gridview.setOnItemClickListener(this);

		final MissedNotificationTask mnt = new MissedNotificationTask();
		mnt.setListener(this);

		final MissedNotificationTask mnt1 = new MissedNotificationTask();
		mnt1.setListener(this);

		final Handler missedNots = new Handler();
		final Runnable missedPresNot = new Runnable() {

			@Override
			public void run() {

				mnt.execute(rmpId, "pres");
				//mnt1.execute(rmpId, "call");
			}
		};
		missedNots.postDelayed(missedPresNot, 3000);

		service = new Intent(MIntel.getAppContext(), PushService.class);
		service.putExtra("rmpId", rmpId);

		startService(service);
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SCAN_REQUEST_CODE_PAT) {
			if (resultCode == RESULT_OK) {
				final String contents = data.getStringExtra("SCAN_RESULT");
				if (contents != null) {
					final PatientLoginTask loginTask = new PatientLoginTask();
					loginTask.setPatientLoginListener(RmpHome.this,
							RmpHome.this, 1);
					loginTask.execute(contents);
				}
			} else {
				createErrorDialog("সঠিক QR কোড দেখান। এই কোডটি ভুল।", false);
				return;
			}
		} else if (requestCode == SCAN_REQUEST_CODE_IMG) {
			if (resultCode == RESULT_OK) {
				final String contents = data.getStringExtra("SCAN_RESULT");
				if (contents != null) {
					final PatientLoginTask loginTask = new PatientLoginTask();
					loginTask.setPatientLoginListener(RmpHome.this,
							RmpHome.this, 2);
					loginTask.execute(contents);
				}
			} else {
				createErrorDialog("সঠিক QR কোড দেখান। এই কোডটি ভুল।", false);
				return;
			}
		}
	}

	private void startNextActivity() {
		final Intent i = new Intent(getApplicationContext(), PatientHome.class);
		i.putExtra("rmp_id", rmpId);
		i.putExtra("pat_id", patId);
		i.putExtra("pat_name", patName);
		i.putExtra("pat_phone", patPhone);
		i.putExtra("pat_age", patAge);
		i.putExtra("pat_gender", patGender);
		i.putExtra("pat_image", patImageUrl);
		startActivity(i);
	}

	private Uri findFormUri(final String formName) {
		@SuppressWarnings("deprecation")
		final Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, null,
				null, null);
		Log.w("Form Count: ", "" + c.getCount());
		Uri formUri = null;
		if (c.moveToFirst()) {
			base: do {
				final String name = c.getString(c
						.getColumnIndex(FormsColumns.DISPLAY_NAME));
				Log.d("Form Name: ", name);
				if (name.equals(formName)) {
					formUri = ContentUris
							.withAppendedId(
									com.mpower.daktar.android.provider.FormsProviderAPI.FormsColumns.CONTENT_URI,
									c.getLong(c.getColumnIndex(BaseColumns._ID)));
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
		// Intent ijk = new Intent(this, PushService.class);
		// ijk.putExtra("rmpId", rmpId);
		// startService(ijk);
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_PREFERENCES, 0,
				getString(R.string.general_preferences)).setIcon(
				android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			final Intent ig = new Intent(this, PreferencesActivity.class);
			startActivity(ig);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void createErrorDialog(final String errorMsg,
			final boolean shouldExit) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(
				RmpHome.this);
		final DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int i) {
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
	public void loginResult(final String json) {
		try {
			final JSONObject data = new JSONObject(json);
			patName = data.getString("name");
			patAge = data.getString("age");
			patGender = data.getString("gender");
			patPhone = data.getString("phone");
			patImageUrl = data.getString("image");
			patId = data.getString("id");
			startNextActivity();
		} catch (final JSONException e) {
			createErrorDialog(json, false);
		}

	}

	static class ImageAdapter extends BaseAdapter {
		private final Context mContext;

		public ImageAdapter(final Context c) {
			mContext = c;
		}

		@Override
		public int getCount() {
			return HOME_ICONS.length;
		}

		@Override
		public LauncherIcon getItem(final int position) {
			return null;
		}

		@Override
		public long getItemId(final int position) {
			return 0;
		}

		static class ViewHolder {
			public ImageView icon;
			public TextView text;
		}

		// Create a new ImageView for each item referenced by the Adapter
		@Override
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			View v = convertView;
			ViewHolder holder;
			if (v == null) {
				final LayoutInflater vi = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				v = vi.inflate(R.layout.dashboard_icon, null);
				holder = new ViewHolder();
				holder.text = (TextView) v
						.findViewById(R.id.dashboard_icon_text);
				final SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(MIntel.getInstance());
				final String question_font = settings.getString(
						PreferencesActivity.KEY_FONT_SIZE,
						MIntel.DEFAULT_FONTSIZE);
				final int mQuestionFontsize = Integer.valueOf(question_font);
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
	public void onItemClick(final AdapterView<?> parent, final View v,
			final int position, final long id) {

		Uri formUri;
		switch (position) {
		case 0:
			// Register Patient
			formUri = findFormUri("Patient Registration");
			if (formUri != null) {
				Intent register = new Intent(Intent.ACTION_EDIT, formUri);
				register.putExtra("rmpId", rmpId);
				startActivity(new Intent(Intent.ACTION_EDIT, formUri));
			} else {
				Toast.makeText(RmpHome.this,
						"রেজিস্ট্রেশান ফর্ম পাওয়া যাচ্ছে না। ডাউনলোড করুন। ",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 1:
			// Get Appointment
			formUri = findFormUri("Patient Session");
			if (formUri != null) {
				Intent appointment = new Intent(Intent.ACTION_EDIT, formUri);
				appointment.putExtra("rmpId", rmpId);
				startActivity(appointment);
			} else {
				Toast.makeText(RmpHome.this,
						"আপইন্টমেন্ট ফর্ম পাওয়া যাচ্ছে না। ডাউনলোড করুন। ",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 2:
			// View Patient Records
			final Intent intent = new Intent(
					"com.google.zxing.client.android.SCAN");
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			intent.putExtra("PROMPT_MESSAGE",
					"রোগীর QR কোড স্ক্যান করুন। কোডটি সম্পূর্ণভাবে বাক্সের ভিতরে রাখুন।");
			startActivityForResult(intent, SCAN_REQUEST_CODE_PAT);
			break;
		case 3:
			// Manage Data
			final Intent i = new Intent(RmpHome.this, MainMenuActivity.class);
			startActivity(i);
			break;
		case 4:
			// Send Picture
			final Intent loginIntent = new Intent(
					"com.google.zxing.client.android.SCAN");
			loginIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			loginIntent
			.putExtra("PROMPT_MESSAGE",
					"রোগীর QR কোড স্ক্যান করুন। কোডটি সম্পূর্ণভাবে বাক্সের ভিতরে রাখুন।");
			startActivityForResult(loginIntent, SCAN_REQUEST_CODE_IMG);
			break;
		case 5:
			// Get Pending Follow Ups
			final Intent followUp = new Intent(getApplicationContext(),
					FollowUpActivity.class);
			followUp.putExtra("rmpid", rmpId);
			startActivity(followUp);
			break;
		case 6:
			// Get Accounts Info Page
			final Intent accountsInfo = new Intent(getApplicationContext(),
					AccountViewActivity.class);
			accountsInfo.putExtra("rmpId", rmpId);
			accountsInfo.putExtra("rmpAcc", rmpAcc);
			startActivity(accountsInfo);
			break;
		case 7:
			// Get Accounts Info Page
			DoctorListRetrievalTask visitValidation = new DoctorListRetrievalTask(
					this, this);
			visitValidation.execute(rmpId);
			break;
		}
	}

	private void showAlertDialog(final String title, final String message,
			final int iconId) {

		final AlertDialog.Builder adb = new AlertDialog.Builder(RmpHome.this);

		adb.setTitle(title);
		adb.setMessage(message);
		adb.setIcon(iconId);
		adb.setPositiveButton("হ্যাঁ", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int id) {
				if (serviceBind != null) {
					serviceBind.onDestroy();
				}
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});

		adb.setNegativeButton(" না", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int id) {
				dialog.dismiss();
			}
		});

		adb.show();
	}

	@Override
	public void onBackPressed() {
		showAlertDialog(
				"সত্যি \"Log out\" করবেন?",
				"আপনি কি সত্যি \"Log out\" করতে চান? \"Log out\" করতে চাইলে \"হ্যাঁ\" চাপুন আর না চাইলে \"না\" চাপুন",
				R.drawable.ic_exclamation);
	}

	@Override
	public void onMissedNotificationRetrieved(final String json,
			final String type) {
		final Intent i = new Intent(RmpHome.this, NotificationActivity.class);
		if (type.equals("pres")) {
			try {
				final JSONObject jsonO = new JSONObject(json);
				final JSONArray urls = jsonO.getJSONArray("urls");
				final JSONArray names = jsonO.getJSONArray("names");
				Log.w("PENDING", "ARRAY CONTENT:" + urls.toString());
				i.putExtra("intent", 2);
				i.putExtra("urls", urls.toString());
				i.putExtra("names", names.toString());
				RmpHome.this.startActivity(i);
			} catch (final Exception e) {

			}
		} else if (type.equals("call")) {
			Log.w("RETURN OUTPUT", json);
			try {
				final JSONObject jsonO = new JSONObject(json);
				final JSONArray docName = jsonO.getJSONArray("docName");
				final JSONArray patName = jsonO.getJSONArray("patientName");
				final JSONArray callCount = jsonO.getJSONArray("callCount");
				i.putExtra("intent", 3);
				i.putExtra("docs", docName.toString());
				i.putExtra("pats", patName.toString());
				i.putExtra("call", callCount.toString());
				RmpHome.this.startActivity(i);
			} catch (final JSONException e) {

			}
		}
	}

	@Override
	public void loginDetail(String json) {
		try {
			final JSONObject data = new JSONObject(json);
			patName = data.getString("name");
			patAge = data.getString("age");
			patGender = data.getString("gender");
			patPhone = data.getString("phone");
			patImageUrl = data.getString("image");
			patId = data.getString("id");
			

			 Intent picSend = new Intent(getApplicationContext(),
			 PictureUpload.class);
			 picSend.putExtra("patId", patId);
			 picSend.putExtra("rmpId", rmpId);
			 startActivity(picSend);
			
		} catch (final JSONException e) {
			createErrorDialog(json, false);
		}
	}

	@Override
	public void onResponseReceived(String response) {
		String[] dataArr;
		try {
			JSONObject json = new JSONObject(response);
			JSONArray data = json.getJSONArray("data");
			dataArr = new String[data.length()];
			for (int i = 0; i < data.length(); i++) {
				dataArr[i] = data.getString(i);
			}
		} catch (JSONException e) {
			return;
		}
		if (dataArr != null){
			AlertDialog.Builder docList = new AlertDialog.Builder(this);
			ListView list = new ListView(this);
			DoctorListAdapter docAdapter = new DoctorListAdapter(this, dataArr, 2);
			list.setAdapter(docAdapter);

			docList.setView(list);
			docList.setTitle("Doctor Selection List");
			docList.setPositiveButton(R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			docList.show();
		}

	}
}