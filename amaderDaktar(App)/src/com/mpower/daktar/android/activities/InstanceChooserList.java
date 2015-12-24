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
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.provider.InstanceProviderAPI;
import com.mpower.daktar.android.provider.InstanceProviderAPI.InstanceColumns;
import com.mpower.daktar.android.R;

/**
 * Responsible for displaying all the valid instances in the instance directory.
 *
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class InstanceChooserList extends ListActivity {

	private static boolean EXIT = true;
	private AlertDialog mAlertDialog;

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
				+ getString(R.string.review_data));
		final TextView tv = (TextView) findViewById(R.id.status_text);
		tv.setVisibility(View.GONE);

		final String selection = InstanceColumns.STATUS + "!=?";
		final String[] selectionArgs = { InstanceProviderAPI.STATUS_SUBMITTED };
		final Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null,
				selection, selectionArgs, InstanceColumns.STATUS + " desc");

		final String[] data = new String[] { InstanceColumns.DISPLAY_NAME,
				InstanceColumns.DISPLAY_SUBTEXT };
		final int[] view = new int[] { R.id.text1, R.id.text2 };

		// render total instance view
		final SimpleCursorAdapter instances = new SimpleCursorAdapter(this,
				R.layout.two_item, c, data, view);
		setListAdapter(instances);
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
	}

	/**
	 * Stores the path of selected instance in the parent class and finishes.
	 */
	@Override
	protected void onListItemClick(final ListView listView, final View view,
			final int position, final long id) {
		final Cursor c = (Cursor) getListAdapter().getItem(position);
		startManagingCursor(c);
		final Uri instanceUri = ContentUris.withAppendedId(
				InstanceColumns.CONTENT_URI,
				c.getLong(c.getColumnIndex(BaseColumns._ID)));

		final String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)) {
			// caller is waiting on a picked form
			setResult(RESULT_OK, new Intent().setData(instanceUri));
		} else {
			// caller wants to view/edit a form, so launch formentryactivity
			startActivity(new Intent(Intent.ACTION_EDIT, instanceUri));
		}
		finish();
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

}
