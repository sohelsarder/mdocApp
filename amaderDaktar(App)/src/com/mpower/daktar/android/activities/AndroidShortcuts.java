/*
 * Copyright (C) 2011 University of Washington
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

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.mpower.daktar.android.provider.FormsProviderAPI.FormsColumns;
import com.mpower.daktar.android.R;

/**
 * Allows the user to create desktop shortcuts to any form currently avaiable to
 * mIntel
 *
 * @author ctsims
 * @author carlhartung (modified for ODK)
 */
public class AndroidShortcuts extends Activity {

	private Uri[] mCommands;
	private String[] mNames;

	@Override
	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		final Intent intent = getIntent();
		final String action = intent.getAction();

		// The Android needs to know what shortcuts are available, generate the
		// list
		if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
			buildMenuList();
		}
	}

	/**
	 * Builds a list of shortcuts
	 */
	private void buildMenuList() {
		final ArrayList<String> names = new ArrayList<String>();
		final ArrayList<Uri> commands = new ArrayList<Uri>();

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select mIntel Shortcut");

		final Cursor c = getContentResolver().query(FormsColumns.CONTENT_URI,
				null, null, null, null);
		startManagingCursor(c);

		if (c.getCount() > 0) {
			c.moveToPosition(-1);
			while (c.moveToNext()) {
				final String formName = c.getString(c
						.getColumnIndex(FormsColumns.DISPLAY_NAME));
				names.add(formName);
				final Uri uri = Uri.withAppendedPath(FormsColumns.CONTENT_URI,
						c.getString(c.getColumnIndex(BaseColumns._ID)));
				commands.add(uri);
			}
		}

		mNames = names.toArray(new String[0]);
		mCommands = commands.toArray(new Uri[0]);

		builder.setItems(mNames, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int item) {
				returnShortcut(mNames[item], mCommands[item]);
			}
		});

		builder.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(final DialogInterface dialog) {
				final AndroidShortcuts sc = AndroidShortcuts.this;
				sc.setResult(RESULT_CANCELED);
				sc.finish();
				return;
			}
		});

		final AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Returns the results to the calling intent.
	 */
	private void returnShortcut(final String name, final Uri command) {
		final Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
		shortcutIntent.setData(command);

		final Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
		final Parcelable iconResource = Intent.ShortcutIconResource
				.fromContext(this, R.drawable.notes);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

		// Now, return the result to the launcher

		setResult(RESULT_OK, intent);
		finish();
		return;
	}

}
