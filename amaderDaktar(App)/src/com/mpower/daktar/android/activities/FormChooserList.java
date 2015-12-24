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

package com.mpower.daktar.android.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.DiskSyncListener;
import com.mpower.daktar.android.provider.FormsProviderAPI.FormsColumns;
import com.mpower.daktar.android.tasks.DiskSyncTask;
import com.mpower.daktar.android.R;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores
 * the path to selected form for use by {@link MainMenuActivity}.
 *
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class FormChooserList extends ListActivity implements DiskSyncListener {

	private static final String t = "FormChooserList";
	private DiskSyncTask mDiskSyncTask;

	private static final boolean EXIT = true;

	private AlertDialog mAlertDialog;

	private final String syncMsgKey = "syncmsgkey";

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// must be at the beginning of any activity that can be called from an
		// external intent
		try {
			MIntel.createMIntelDirs();
		} catch (final RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		setContentView(R.layout.chooser_list_layout);
		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.enter_data));

		// DiskSyncTask checks the disk for any forms not already in the content
		// provider
		// that is, put here by dragging and dropping onto the SDCard
		mDiskSyncTask = (DiskSyncTask) getLastNonConfigurationInstance();
		if (mDiskSyncTask == null) {
			Log.i(t, "Starting new disk sync task");
			mDiskSyncTask = new DiskSyncTask();
			mDiskSyncTask.setDiskSyncListener(this);
			mDiskSyncTask.execute((Void[]) null);
		}

		if (mDiskSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
			mDiskSyncTask.setDiskSyncListener(null);
		}

		final Cursor c = managedQuery(FormsColumns.CONTENT_URI, null, null,
				null, null);

		final String[] data = new String[] { FormsColumns.DISPLAY_NAME,
				FormsColumns.DISPLAY_SUBTEXT };
		final int[] view = new int[] { R.id.text1, R.id.text2 };

		// render total instance view
		final SimpleCursorAdapter instances = new SimpleCursorAdapter(this,
				R.layout.two_item, c, data, view);
		setListAdapter(instances);

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(syncMsgKey)) {
			final TextView tv = (TextView) findViewById(R.id.status_text);
			tv.setText(savedInstanceState.getString(syncMsgKey));
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		// pass the thread on restart
		return mDiskSyncTask;
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		final TextView tv = (TextView) findViewById(R.id.status_text);
		outState.putString(syncMsgKey, tv.getText().toString());
	}

	/**
	 * Stores the path of selected form and finishes.
	 */
	@Override
	protected void onListItemClick(final ListView listView, final View view,
			final int position, final long id) {
		// get uri to form
		final Cursor c = (Cursor) getListAdapter().getItem(position);
		startManagingCursor(c);
		final Uri formUri = ContentUris.withAppendedId(
				FormsColumns.CONTENT_URI,
				c.getLong(c.getColumnIndex(BaseColumns._ID)));

		Log.w("formUri", formUri.toString());

		final String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)) {
			// caller is waiting on a picked form
			// setResult(RESULT_OK, new Intent().setData(formUri));
		} else {
			// caller wants to view/edit a form, so launch formentryactivity
			// startActivity(new Intent(Intent.ACTION_EDIT, formUri));
		}

		// finish();
	}

	@Override
	protected void onResume() {
		mDiskSyncTask.setDiskSyncListener(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		mDiskSyncTask.setDiskSyncListener(null);
		super.onPause();
	}

	/**
	 * Called by DiskSyncTask when the task is finished
	 */
	@Override
	public void SyncComplete(final String result) {
		Log.i(t, "disk sync task complete");
		final TextView tv = (TextView) findViewById(R.id.status_text);
		tv.setText(result);
	}

	/**
	 * Creates a dialog with the given message. Will exit the activity when the
	 * user preses "ok" if shouldExit is set to true.
	 *
	 * @param errorMsg
	 * @param shouldExit
	 */
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

}
