package com.mpower.daktar.android.activities;

import com.mpower.daktar.android.R;
import com.mpower.daktar.android.fingerprint.BluetoothActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FingerprintActivity extends Activity {

	public static final String FLAG_MODE = "mod";
	public static final String FLAG_DATA = "UID";

	public static final String ENROLL = "ENR";
	public static final String CHECK = "CHK";

	private String operatingMode;

	private String value;

	private LinearLayout mainLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent callingIntent = getIntent();
		operatingMode = callingIntent.getStringExtra(FLAG_MODE);
		setContentView(R.layout.ativity_fingerprint);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mainLayout = (LinearLayout) findViewById(R.id.fingerprint_layout);
		mainLayout.removeAllViews();
		if (operatingMode.equals(ENROLL)) {
			Button b = new Button(this);
			b.setText("হাতের ছাপ নিন।");
			b.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					
				}
			});
			mainLayout.addView(b);
			setContentView(mainLayout);
		} else if (operatingMode.equals(CHECK)) {

		} else {
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (operatingMode.equals(ENROLL)) {
			setResult(RESULT_OK, data);
			Log.w("Enroll Result", data.getStringExtra(FLAG_DATA));
		} else if (operatingMode.equals(CHECK)) {
			setResult(RESULT_OK, data);
			Log.w("Enroll Result", data.getStringExtra(FLAG_DATA));
		} else {
			return;
		}
	}
}
