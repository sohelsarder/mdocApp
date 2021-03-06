/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mpower.mintel.android.activities;

import com.mpower.mintel.android.R;

import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.preferences.PreferencesActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class MainMenuActivity extends Activity implements OnItemClickListener {
	private static final String t = "MainMenuActivity";

	// menu options
	private static final int MENU_PREFERENCES = Menu.FIRST;

	static final LauncherIcon[] ICONS = {
			//new LauncherIcon(R.drawable.pencil, R.string.new_data),
			new LauncherIcon(R.drawable.edit_data, R.string.review_data),
			new LauncherIcon(R.drawable.send_data, R.string.send_data),
			new LauncherIcon(R.drawable.download_form, R.string.download_form),
			new LauncherIcon(R.drawable.delete_data, R.string.delete)
			//new LauncherIcon(R.drawable.tools, R.string.settings) 
			};

	private AlertDialog mAlertDialog;

	private static boolean EXIT = true;

	// private static boolean DO_NOT_EXIT = false;

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

		setContentView(R.layout.main_menu);
		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.main_menu));

		GridView gridview = (GridView) findViewById(R.id.dashboard_grid);
		gridview.setAdapter(new ImageAdapter(this));
		gridview.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		switch (position) {
		case 9:
			// New
			Intent iNew = new Intent(getApplicationContext(),
					FormChooserList.class);
			startActivity(iNew);
			break;
		case 0:
			// Edit Data
			Intent iEdit = new Intent(getApplicationContext(),
					InstanceChooserList.class);
			startActivity(iEdit);
			break;
		case 1:
			// Send Data
			Intent iSend = new Intent(getApplicationContext(),
					InstanceUploaderList.class);
			startActivity(iSend);
			break;
		case 2:
			// Download Form
			Intent iDownload = new Intent(getApplicationContext(),
					FormDownloadList.class);
			startActivity(iDownload);
			break;
		case 3:
			// Delete
			Intent iManage = new Intent(getApplicationContext(),
					FileManagerTabs.class);
			startActivity(iManage);
			break;
		case 5:
			// Settings
			Intent ig = new Intent(this, PreferencesActivity.class);
			startActivity(ig);
			break;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
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
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
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

	static class LauncherIcon {
		final int text;
		final int imgId;

		public LauncherIcon(int imgId, int text) {
			super();
			this.imgId = imgId;
			this.text = text;
		}

	}

	static class ImageAdapter extends BaseAdapter {
		private Context mContext;

		public ImageAdapter(Context c) {
			mContext = c;
		}

		@Override
		public int getCount() {
			return ICONS.length;
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
				SharedPreferences settings =
		                PreferenceManager.getDefaultSharedPreferences(MIntel.getInstance());
		            String question_font =
		                settings.getString(PreferencesActivity.KEY_FONT_SIZE, MIntel.DEFAULT_FONTSIZE);
		            int mQuestionFontsize = new Integer(question_font).intValue();
		            holder.text.setTextSize(mQuestionFontsize);
				holder.icon = (ImageView) v
						.findViewById(R.id.dashboard_icon_img);
				v.setTag(holder);
			} else {
				holder = (ViewHolder) v.getTag();
			}

			holder.icon.setImageResource(ICONS[position].imgId);
			holder.text.setText(ICONS[position].text);

			return v;
		}
	}

}
