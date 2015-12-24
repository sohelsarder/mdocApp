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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.InstanceUploaderListener;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.provider.InstanceProviderAPI.InstanceColumns;
import com.mpower.daktar.android.tasks.InstanceUploaderTask;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

/**
 * Activity to upload completed forms.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class InstanceUploaderActivity extends Activity implements
		InstanceUploaderListener {
	private final static String t = "InstanceUploaderActivity";
	private final static int PROGRESS_DIALOG = 1;
	private final static int AUTH_DIALOG = 2;

	private ProgressDialog mProgressDialog;
	private AlertDialog mAlertDialog;

	private String mAlertMsg;
	private final String ALERT_MSG = "alertmsg";
	private final String ALERT_SHOWING = "alertshowing";
	private static final String TO_SEND = "tosend";
	private boolean mAlertShowing;

	private InstanceUploaderTask mInstanceUploaderTask;

	// maintain a list of what we've yet to send, in case we're interrupted by
	// auth requests
	private ArrayList<Long> mInstancesToSend;

	// maintain a list of what we've sent, in case we're interrupted by auth
	// requests
	private HashMap<String, String> mUploadedInstances;
	private String mUrl;

	private final static String AUTH_URI = "auth";

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAlertMsg = getString(R.string.please_wait);

		mUploadedInstances = new HashMap<String, String>();

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.send_data));

		// get instances to upload
		final Intent intent = getIntent();
		final long[] selectedInstanceIDs = intent
				.getLongArrayExtra(FormEntryActivity.KEY_INSTANCES);
		if (selectedInstanceIDs.length == 0) {
			// If we get nothing, toast and quit
			Toast.makeText(this, R.string.noselect_error, Toast.LENGTH_LONG);
			finish();
			return;
		}

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(ALERT_MSG)) {
				mAlertMsg = savedInstanceState.getString(ALERT_MSG);
			}
			if (savedInstanceState.containsKey(ALERT_SHOWING)) {
				mAlertShowing = savedInstanceState.getBoolean(ALERT_SHOWING,
						false);
			}
		}

		if (savedInstanceState != null
				&& !savedInstanceState.containsKey(TO_SEND)) {
			mInstancesToSend = (ArrayList<Long>) savedInstanceState
					.getSerializable(TO_SEND);
		} else {
			mInstancesToSend = new ArrayList<Long>();
			for (int i = 0; i < selectedInstanceIDs.length; i++) {
				mInstancesToSend.add(new Long(selectedInstanceIDs[i]));
			}
		}

		// get the task if we've changed orientations. If it's null it's a new
		// upload.
		mInstanceUploaderTask = (InstanceUploaderTask) getLastNonConfigurationInstance();
		if (mInstanceUploaderTask == null) {
			// setup dialog and upload task
			showDialog(PROGRESS_DIALOG);
			PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			mInstanceUploaderTask = new InstanceUploaderTask();

			// register this activity with the new uploader task
			mInstanceUploaderTask
					.setUploaderListener(InstanceUploaderActivity.this);

			final Long[] toSendArray = new Long[mInstancesToSend.size()];
			mInstancesToSend.toArray(toSendArray);
			// String auth = settings.getString(PreferencesActivity.KEY_AUTH,
			// "");
			final String auth = "";
			mInstanceUploaderTask.setAuth(auth);
			mInstanceUploaderTask.execute(toSendArray);
		}
	}

	@Override
	public void uploadingComplete(final HashMap<String, String> result) {
		try {
			dismissDialog(PROGRESS_DIALOG);

		} catch (final Exception e) {
			// tried to close a dialog not open. don't care.
		}

		final StringBuilder selection = new StringBuilder();
		final Set<String> keys = result.keySet();
		final Iterator<String> it = keys.iterator();

		final String[] selectionArgs = new String[keys.size()];
		int i = 0;
		while (it.hasNext()) {
			final String id = it.next();
			selection.append(BaseColumns._ID + "=?");
			selectionArgs[i++] = id;
			if (i != keys.size()) {
				selection.append(" or ");
			}
		}

		final Cursor results = managedQuery(InstanceColumns.CONTENT_URI, null,
				selection.toString(), selectionArgs, null);
		final StringBuilder message = new StringBuilder();
		if (results.getCount() > 0) {
			results.moveToPosition(-1);
			while (results.moveToNext()) {
				final String name = results.getString(results
						.getColumnIndex(InstanceColumns.DISPLAY_NAME));
				final String id = results.getString(results
						.getColumnIndex(BaseColumns._ID));
				Log.w("Result Info", "ID= " + id + " Result= " + result.get(id));
				boolean isSuccess = false;
				long lId = -1;
				try {
					// ######
					lId = Long.parseLong(id);
					isSuccess = result.get(id).contains("id=\"success\"");
					Log.w("Result Status", "After Parsing: " + isSuccess);
				} catch (final NumberFormatException nfe) {
					isSuccess = false;
				}
				Log.w("Result Status", "Before Check: " + isSuccess);
				if (isSuccess) {
					final Uri myUri = Uri.fromFile(new File(
							MIntel.METADATA_PATH + "/form_s.ogg")); // initialize Uri
					// here
					final MediaPlayer mediaPlayer = new MediaPlayer();
					mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					try {
						mediaPlayer.setDataSource(getApplicationContext(),
								myUri);
						mediaPlayer.prepare();
					} catch (final IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final IOException e) {
						e.printStackTrace();
					}
					mediaPlayer.start();
					message.append(name + " - " + result.get(id) + "\n\n");
					final ArrayList<Long> mSelected = new ArrayList<Long>();
					mSelected.add(lId);
					deleteSelectedInstances(mSelected);
				} else {
					message.append(name + " - " + result.get(id) + "\n\n");
				}
			}
		} else {
			message.append(getString(R.string.no_forms_uploaded));
		}

		createAlertDialog(message.toString().trim());
	}

	@Override
	public void progressUpdate(final int progress, final int total) {
		mAlertMsg = getString(R.string.sending_items, progress, total);
		mProgressDialog.setMessage(mAlertMsg);
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			mProgressDialog = new ProgressDialog(this);
			final DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog,
						final int which) {
					dialog.dismiss();
					mInstanceUploaderTask.cancel(true);
					mInstanceUploaderTask.setUploaderListener(null);
					finish();
				}
			};
			mProgressDialog.setTitle(getString(R.string.uploading_data));
			mProgressDialog.setMessage(mAlertMsg);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setButton(getString(R.string.cancel),
					loadingButtonListener);
			return mProgressDialog;
		case AUTH_DIALOG:
			final AlertDialog.Builder b = new AlertDialog.Builder(this);

			final LayoutInflater factory = LayoutInflater.from(this);
			final View dialogView = factory.inflate(
					R.layout.server_auth_dialog, null);

			// Get the server, username, and password from the settings
			final SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());

			String server = mUrl;
			if (server == null) {
				// if the bundle is null, we're looking for a formlist
				// server =
				// settings.getString(PreferencesActivity.KEY_SERVER_URL,
				// getString(R.string.default_server_url))
				// + settings.getString(PreferencesActivity.KEY_FORMLIST_URL,
				// "/formList");
				server = settings.getString(PreferencesActivity.KEY_SERVER_URL,
						getString(R.string.default_server_url)) + "/formList";
			}

			final String url = server;

			Log.i(t, "Trying connecting to: " + url);

			final EditText username = (EditText) dialogView
					.findViewById(R.id.username_edit);
			final String storedUsername = settings.getString(
					PreferencesActivity.KEY_USERNAME, null);
			username.setText(storedUsername);

			final EditText password = (EditText) dialogView
					.findViewById(R.id.password_edit);
			final String storedPassword = settings.getString(
					PreferencesActivity.KEY_PASSWORD, null);
			password.setText(storedPassword);

			b.setTitle(getString(R.string.server_requires_auth));
			b.setMessage(getString(R.string.server_auth_credentials, url));
			b.setView(dialogView);
			b.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
						final int which) {
							final EditText username = (EditText) dialogView
									.findViewById(R.id.username_edit);
							final EditText password = (EditText) dialogView
									.findViewById(R.id.password_edit);

							final URI u = URI.create(mUrl);
							WebUtils.addCredentials(username.getText()
									.toString(), WebUtils.getSHA512(password
									.getText().toString()), u.getHost());

							mInstanceUploaderTask = new InstanceUploaderTask();

							// register this activity with the new uploader task
							mInstanceUploaderTask
									.setUploaderListener(InstanceUploaderActivity.this);

							final Long[] toSendArray = new Long[mInstancesToSend
									.size()];
							mInstancesToSend.toArray(toSendArray);
							mInstanceUploaderTask.execute(toSendArray);
							showDialog(PROGRESS_DIALOG);
						}
					});
			b.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog,
						final int which) {
							finish();
						}
					});

			b.setCancelable(false);
			return b.create();
		}
		return null;
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mUrl = savedInstanceState.getString(AUTH_URI);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ALERT_MSG, mAlertMsg);
		outState.putBoolean(ALERT_SHOWING, mAlertShowing);
		outState.putSerializable(TO_SEND, mInstancesToSend);
		outState.putString(AUTH_URI, mUrl);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mInstanceUploaderTask;
	}

	@Override
	protected void onDestroy() {
		if (mInstanceUploaderTask != null) {
			mInstanceUploaderTask.setUploaderListener(null);
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}

	@Override
	protected void onResume() {
		if (mInstanceUploaderTask != null) {
			mInstanceUploaderTask.setUploaderListener(this);
		}
		if (mAlertShowing) {
			createAlertDialog(mAlertMsg);
		}
		super.onResume();
	}

	@Override
	public void authRequest(final URI url,
			final HashMap<String, String> doneSoFar) {
		if (mProgressDialog.isShowing()) {
			// should always be showing here
			mProgressDialog.dismiss();
		}

		// add our list of completed uploads to "completed"
		// and remove them from our toSend list.
		if (doneSoFar != null) {
			final Set<String> uploadedInstances = doneSoFar.keySet();
			final Iterator<String> itr = uploadedInstances.iterator();

			while (itr.hasNext()) {
				final Long removeMe = new Long(itr.next());
				final boolean removed = mInstancesToSend.remove(removeMe);
				if (removed) {
					Log.i(t,
							removeMe
									+ " was already sent, removing from queue before restarting task");
				}
			}
			mUploadedInstances.putAll(doneSoFar);
		}

		// Bundle b = new Bundle();
		// b.putString(AUTH_URI, url.toString());
		// showDialog(AUTH_DIALOG, b);
		mUrl = url.toString();
		showDialog(AUTH_DIALOG);
	}

	private void createAlertDialog(final String message) {
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setTitle(getString(R.string.upload_results));
		mAlertDialog.setMessage(Html.fromHtml(message));
		final DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int i) {
				switch (i) {
				case DialogInterface.BUTTON1: // ok
					// always exit this activity since it has no interface
					mAlertShowing = false;
					finish();
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), quitListener);
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertShowing = true;
		mAlertMsg = message;
		mAlertDialog.show();
	}

	/**
	 * Deletes the selected files. Content provider handles removing the files
	 * from the filesystem.
	 */
	private void deleteSelectedInstances(final ArrayList<Long> mSelected) {
		final ContentResolver cr = getContentResolver();
		int deleted = 0;
		for (int i = 0; i < mSelected.size(); i++) {
			final Uri deleteForm = Uri.withAppendedPath(
					InstanceColumns.CONTENT_URI, mSelected.get(i).toString());
			deleted += cr.delete(deleteForm, null, null);
		}

		if (deleted == mSelected.size()) {
			// all deletes were successful
			Toast.makeText(this, getString(R.string.file_deleted_ok, deleted),
					Toast.LENGTH_SHORT).show();
		} else {
			// had some failures
			Log.e(t, "Failed to delete " + (mSelected.size() - deleted)
					+ " instances");
			Toast.makeText(
					this,
					getString(R.string.file_deleted_error, mSelected.size()
							- deleted, mSelected.size()), Toast.LENGTH_LONG)
							.show();
		}
	}

}
