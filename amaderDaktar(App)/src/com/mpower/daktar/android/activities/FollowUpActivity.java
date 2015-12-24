package com.mpower.daktar.android.activities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.mpower.daktar.android.adapters.FollowUpAdapter;
import com.mpower.daktar.android.adapters.FollowUpAdapter.ViewHolder;
import com.mpower.daktar.android.listeners.FollowUpListener;
import com.mpower.daktar.android.tasks.FollowUpCheckerTask;
import com.mpower.daktar.android.R;

public class FollowUpActivity extends Activity implements FollowUpListener {

	private ListView followuplist;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent caller = getIntent();
		final String rmpId = caller.getStringExtra("rmpid");
		setContentView(R.layout.activity_followup);
		followuplist = (ListView) findViewById(R.id.list_followup);
		if (rmpId != null) {
			final FollowUpCheckerTask follow = new FollowUpCheckerTask();
			follow.addListener(this);
			follow.execute(rmpId);
		}
	}

	@Override
	public void onFollowUpReceived(final String response) {
		new Intent(FollowUpActivity.this, NotificationActivity.class);
		try {
			final JSONObject json = new JSONObject(response);
			final JSONArray data = json.getJSONArray("data");
			final String[] objects = new String[data.length()];
			for (int i = 0; i < data.length(); i++) {
				objects[i] = data.getString(i);
			}

			followuplist.setAdapter(new FollowUpAdapter(this, objects));
			followuplist.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(final AdapterView<?> arg0,
						final View arg1, final int arg2, final long arg3) {
					final Intent callIntent = new Intent(Intent.ACTION_CALL);
					final FollowUpAdapter.ViewHolder vh = (ViewHolder) arg1
							.getTag();
					callIntent.setData(Uri.parse("tel:" + vh.phone.getText()));
					startActivity(callIntent);
				}
			});

		} catch (final JSONException jsone) {

		}
	}
}