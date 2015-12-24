package com.mpower.daktar.android.activities;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mpower.daktar.android.adapters.PrescriptionListAdapter;
import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.PrescriptionListDownloadListener;
import com.mpower.daktar.android.models.Prescription;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.tasks.ImageDownloadTask;
import com.mpower.daktar.android.tasks.PdfDownloadTask;
import com.mpower.daktar.android.tasks.PrescriptionListDownloadTask;
import com.mpower.daktar.android.R;

public class PatientHome extends Activity implements
		PrescriptionListDownloadListener {

	// menu options
	private static final int MENU_PREFERENCES = Menu.FIRST;
	private AlertDialog mAlertDialog;

	private static final String PATIENT_IMAGE_URI_PART = "/uploads/";

	private String rmp_id;
	private String pat_id;

	private String pat_name;
	private String pat_phone;
	private String pat_gender;
	private String pat_age;
	private String pat_image;

	private TextView name;
	private TextView phone;
	private TextView gender;
	private TextView age;
	private ImageView image;

	private ListView presList;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_patient_home);
		setTitle(getString(R.string.app_name) + " > " + "Patient Home");
		// Get the patient information details from the previous page
		final Intent i = getIntent();
		rmp_id = i.getStringExtra("rmp_id");
		pat_id = i.getStringExtra("pat_id");
		pat_name = i.getStringExtra("pat_name");
		pat_phone = i.getStringExtra("pat_phone");
		pat_age = i.getStringExtra("pat_age");
		pat_gender = i.getStringExtra("pat_gender");
		pat_image = i.getStringExtra("pat_image");

		name = (TextView) findViewById(R.id.name);
		name.setText(pat_name);

		phone = (TextView) findViewById(R.id.phone);
		phone.setText(pat_phone);

		age = (TextView) findViewById(R.id.age);
		age.setText(pat_age);

		gender = (TextView) findViewById(R.id.gender);
		gender.setText(pat_gender);

		image = (ImageView) findViewById(R.id.image);

		// Get the server details and the image to view
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		// Get the base server url
		String downloadUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));
		// Append the patient image uri part
		downloadUrl += PATIENT_IMAGE_URI_PART;
		// Append te patient image name
		downloadUrl += pat_image;

		presList = (ListView) findViewById(R.id.prescription_list);
		presList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> arg0, final View arg1,
					final int arg2, final long arg3) {

				new PdfDownloadTask(PatientHome.this);
				final Prescription pres = ((PrescriptionListAdapter) presList
						.getAdapter()).getElement(arg2);
				final Intent i = new Intent(PatientHome.this,
						DownloadActivity.class);
				i.putExtra("url", pres.url);
				i.putExtra("name", pres.filename);
				startActivityForResult(i, 123);
			}
		});

		final PrescriptionListDownloadTask presDownload = new PrescriptionListDownloadTask(
				PatientHome.this);
		presDownload.execute(pat_id);

		final ImageDownloadTask imgTask = new ImageDownloadTask(
				PatientHome.this, image);
		imgTask.execute(downloadUrl);

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
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		final DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int i) {
				switch (i) {
				case DialogInterface.BUTTON1:
					if (shouldExit) {
						finish();
					}
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		mAlertDialog.show();
	}

	@Override
	public void onPrescriptionListDownloaded(final String json) {
		try {
			final JSONObject data = new JSONObject(json);
			final JSONArray appointmentIds = data.getJSONArray("appointmentId");
			final JSONArray appointmentDate = data
					.getJSONArray("appointmentDate");
			final JSONArray status = data.getJSONArray("status");
			final JSONArray prescriptionUrl = data
					.getJSONArray("prescriptionUrl");
			final JSONArray filenames = data.getJSONArray("filename");
			final ArrayList<Prescription> list = new ArrayList<Prescription>();
			for (int i = 0; i < status.length(); i++) {
				list.add(new Prescription(appointmentIds.getString(i),
						appointmentDate.getString(i), status.getString(i),
						prescriptionUrl.getString(i), filenames.getString(i)));
			}
			presList.setAdapter(new PrescriptionListAdapter(PatientHome.this,
					list));
		} catch (final JSONException e) {
			createErrorDialog(json, false);
		}

	}

}
