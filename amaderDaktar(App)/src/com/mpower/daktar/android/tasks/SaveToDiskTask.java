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

package com.mpower.daktar.android.tasks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.model.xform.XFormSerializingVisitor;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.mpower.daktar.android.activities.FormEntryActivity;
import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.FormSavedListener;
import com.mpower.daktar.android.logic.FormController;
import com.mpower.daktar.android.provider.InstanceProviderAPI;
import com.mpower.daktar.android.provider.FormsProviderAPI.FormsColumns;
import com.mpower.daktar.android.provider.InstanceProviderAPI.InstanceColumns;

/**
 * Background task for loading a form.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SaveToDiskTask extends AsyncTask<Void, String, Integer> {
	private final static String t = "SaveToDiskTask";

	private FormSavedListener mSavedListener;
	private final Boolean mSave;
	private final Boolean mMarkCompleted;
	private final Uri mUri;
	private final String mInstanceName;

	public static final int SAVED = 500;
	public static final int SAVE_ERROR = 501;
	public static final int VALIDATE_ERROR = 502;
	public static final int VALIDATED = 503;
	public static final int SAVED_AND_EXIT = 504;

	public SaveToDiskTask(final Uri uri, final Boolean saveAndExit,
			final Boolean markCompleted, final String updatedName) {
		mUri = uri;
		mSave = saveAndExit;
		mMarkCompleted = markCompleted;
		mInstanceName = updatedName;
	}

	/**
	 * Initialize {@link FormEntryController} with {@link FormDef} from binary
	 * or from XML. If given an instance, it will be used to fill the
	 * {@link FormDef}.
	 */
	@Override
	protected Integer doInBackground(final Void... nothing) {

		// validation failed, pass specific failure
		final int validateStatus = validateAnswers(mMarkCompleted);
		if (validateStatus != VALIDATED)
			return validateStatus;

		FormEntryActivity.mFormController.postProcessInstance();

		if (mSave && exportData(mMarkCompleted))
			return SAVED_AND_EXIT;
		else if (exportData(mMarkCompleted))
			return SAVED;

		return SAVE_ERROR;

	}

	/**
	 * Write's the data to the sdcard, and updates the instances content
	 * provider. In theory we don't have to write to disk, and this is where
	 * you'd add other methods.
	 *
	 * @param markCompleted
	 * @return
	 */
	public boolean exportData(final boolean markCompleted) {
		ByteArrayPayload payload;
		try {

			// assume no binary data inside the model.
			final FormInstance datamodel = FormEntryActivity.mFormController
					.getInstance();
			final XFormSerializingVisitor serializer = new XFormSerializingVisitor();
			payload = (ByteArrayPayload) serializer
					.createSerializedPayload(datamodel);

			// write out xml
			exportXmlFile(payload, FormEntryActivity.mInstancePath);

		} catch (final IOException e) {
			Log.e(t, "Error creating serialized payload");
			e.printStackTrace();
			return false;
		}

		// If FormEntryActivity was started with an Instance, just update that
		// instance
		if (MIntel.getInstance().getContentResolver().getType(mUri) == InstanceColumns.CONTENT_ITEM_TYPE) {
			final ContentValues values = new ContentValues();
			if (mInstanceName != null) {
				values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
			}
			if (!mMarkCompleted) {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_INCOMPLETE);
			} else {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_COMPLETE);
			}
			MIntel.getInstance().getContentResolver()
			.update(mUri, values, null, null);
		} else if (MIntel.getInstance().getContentResolver().getType(mUri) == FormsColumns.CONTENT_ITEM_TYPE) {
			// If FormEntryActivity was started with a form, then it's likely
			// the first time we're
			// saving.
			// However, it could be a not-first time saving if the user has been
			// using the manual
			// 'save data' option from the menu. So try to update first, then
			// make a new one if that
			// fails.
			final ContentValues values = new ContentValues();
			if (mInstanceName != null) {
				values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
			}
			if (mMarkCompleted) {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_COMPLETE);
			} else {
				values.put(InstanceColumns.STATUS,
						InstanceProviderAPI.STATUS_INCOMPLETE);
			}

			final String where = InstanceColumns.INSTANCE_FILE_PATH + "=?";
			final String[] whereArgs = { FormEntryActivity.mInstancePath };
			final int updated = MIntel
					.getInstance()
					.getContentResolver()
					.update(InstanceColumns.CONTENT_URI, values, where,
							whereArgs);
			if (updated > 1) {
				Log.w(t, "Updated more than one entry, that's not good");
			} else if (updated == 1) {
				Log.i(t, "Instance already exists, updating");
				// already existed and updated just fine
				return true;
			} else {
				Log.e(t, "No instance found, creating");
				// Entry didn't exist, so create it.
				final Cursor c = MIntel.getInstance().getContentResolver()
						.query(mUri, null, null, null, null);
				c.moveToFirst();
				final String jrformid = c.getString(c
						.getColumnIndex(FormsColumns.JR_FORM_ID));
				final String formname = c.getString(c
						.getColumnIndex(FormsColumns.DISPLAY_NAME));
				final String submissionUri = c.getString(c
						.getColumnIndex(FormsColumns.SUBMISSION_URI));

				values.put(InstanceColumns.INSTANCE_FILE_PATH,
						FormEntryActivity.mInstancePath);
				values.put(InstanceColumns.INSTANCE_FILE_PATH,
						FormEntryActivity.mInstancePath);
				values.put(InstanceColumns.SUBMISSION_URI, submissionUri);
				if (mInstanceName != null) {
					values.put(InstanceColumns.DISPLAY_NAME, mInstanceName);
				} else {
					values.put(InstanceColumns.DISPLAY_NAME, formname);
				}
				values.put(InstanceColumns.JR_FORM_ID, jrformid);
				MIntel.getInstance().getContentResolver()
				.insert(InstanceColumns.CONTENT_URI, values);
			}
		}

		return true;
	}

	/**
	 * This method actually writes the xml to disk.
	 *
	 * @param payload
	 * @param path
	 * @return
	 */
	private boolean exportXmlFile(final ByteArrayPayload payload,
			final String path) {
		// create data stream
		final InputStream is = payload.getPayloadStream();
		final int len = (int) payload.getLength();

		// read from data stream
		final byte[] data = new byte[len];
		try {
			final int read = is.read(data, 0, len);
			if (read > 0) {
				// write xml file
				try {
					// String filename = path + "/" +
					// path.substring(path.lastIndexOf('/') + 1) + ".xml";
					final BufferedWriter bw = new BufferedWriter(
							new FileWriter(path));
					bw.write(new String(data, "UTF-8"));
					bw.flush();
					bw.close();
					return true;

				} catch (final IOException e) {
					Log.e(t, "Error writing XML file");
					e.printStackTrace();
					return false;
				}
			}
		} catch (final IOException e) {
			Log.e(t, "Error reading from payload data stream");
			e.printStackTrace();
			return false;
		}

		return false;
	}

	@Override
	protected void onPostExecute(final Integer result) {
		synchronized (this) {
			if (mSavedListener != null) {
				mSavedListener.savingComplete(result);
			}
		}
	}

	public void setFormSavedListener(final FormSavedListener fsl) {
		synchronized (this) {
			mSavedListener = fsl;
		}
	}

	/**
	 * Goes through the entire form to make sure all entered answers comply with
	 * their constraints. Constraints are ignored on 'jump to', so answers can
	 * be outside of constraints. We don't allow saving to disk, though, until
	 * all answers conform to their constraints/requirements.
	 *
	 * @param markCompleted
	 * @return validatedStatus
	 */
	private int validateAnswers(final Boolean markCompleted) {
		final FormIndex i = FormEntryActivity.mFormController.getFormIndex();
		FormEntryActivity.mFormController.jumpToIndex(FormIndex
				.createBeginningOfFormIndex());

		int event;
		while ((event = FormEntryActivity.mFormController
				.stepToNextEvent(FormController.STEP_OVER_GROUP)) != FormEntryController.EVENT_END_OF_FORM) {
			if (event != FormEntryController.EVENT_QUESTION) {
				continue;
			} else {
				final int saveStatus = FormEntryActivity.mFormController
						.answerQuestion(FormEntryActivity.mFormController
								.getQuestionPrompt().getAnswerValue());
				if (markCompleted
						&& saveStatus != FormEntryController.ANSWER_OK)
					return saveStatus;
			}
		}

		FormEntryActivity.mFormController.jumpToIndex(i);
		return VALIDATED;
	}

}
