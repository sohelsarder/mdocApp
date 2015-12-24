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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.model.xform.XFormsModule;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mpower.daktar.android.adapters.DoctorListAdapter;
import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.fingerprint.BluetoothActivity;
import com.mpower.daktar.android.listeners.AdvanceToNextListener;
import com.mpower.daktar.android.listeners.DoctorListListener;
import com.mpower.daktar.android.listeners.FormLoaderListener;
import com.mpower.daktar.android.listeners.FormSavedListener;
import com.mpower.daktar.android.listeners.VisitValidationListener;
import com.mpower.daktar.android.logic.FormController;
import com.mpower.daktar.android.logic.PropertyManager;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.provider.InstanceProviderAPI;
import com.mpower.daktar.android.provider.FormsProviderAPI.FormsColumns;
import com.mpower.daktar.android.provider.InstanceProviderAPI.InstanceColumns;
import com.mpower.daktar.android.tasks.DoctorListRetrievalTask;
import com.mpower.daktar.android.tasks.FormLoaderTask;
import com.mpower.daktar.android.tasks.SaveToDiskTask;
import com.mpower.daktar.android.tasks.VisitValidationTask;
import com.mpower.daktar.android.utilities.FileUtils;
import com.mpower.daktar.android.views.MIntelView;
import com.mpower.daktar.android.widgets.QuestionWidget;
import com.mpower.daktar.android.widgets.StringWidget;
import com.mpower.daktar.android.R;

/**
 * FormEntryActivity is responsible for displaying questions, animating
 * transitions between questions, and allowing the user to enter data.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class FormEntryActivity extends Activity implements AnimationListener,
		FormLoaderListener, FormSavedListener, AdvanceToNextListener,
		OnGestureListener, VisitValidationListener, DoctorListListener {
	private static final String t = "FormEntryActivity";

	// Defines for FormEntryActivity
	private static final boolean EXIT = true;
	private static final boolean DO_NOT_EXIT = false;
	private static final boolean EVALUATE_CONSTRAINTS = true;
	private static final boolean DO_NOT_EVALUATE_CONSTRAINTS = false;

	// Request codes for returning data from specified intent.
	public static final int IMAGE_CAPTURE = 1;
	public static final int BARCODE_CAPTURE = 2;
	public static final int AUDIO_CAPTURE = 3;
	public static final int VIDEO_CAPTURE = 4;
	public static final int LOCATION_CAPTURE = 5;
	public static final int HIERARCHY_ACTIVITY = 6;
	public static final int IMAGE_CHOOSER = 7;
	public static final int AUDIO_CHOOSER = 8;
	public static final int VIDEO_CHOOSER = 9;

	// Extra returned from gp activity
	public static final String LOCATION_RESULT = "LOCATION_RESULT";

	// Identifies the gp of the form used to launch form entry
	public static final String KEY_FORMPATH = "formpath";
	public static final String KEY_INSTANCEPATH = "instancepath";
	public static final String KEY_INSTANCES = "instances";
	public static final String KEY_SUCCESS = "success";
	public static final String KEY_ERROR = "error";

	// Identifies whether this is a new form, or reloading a form after a screen
	// rotation (or similar)
	private static final String NEWFORM = "newform";

	private static final int MENU_LANGUAGES = Menu.FIRST;
	private static final int MENU_HIERARCHY_VIEW = Menu.FIRST + 1;
	private static final int MENU_SAVE = Menu.FIRST + 2;
	private static final int MENU_PREFERENCES = Menu.FIRST + 3;

	private static final int PROGRESS_DIALOG = 1;
	private static final int SAVING_DIALOG = 2;

	// Random ID
	private static final int DELETE_REPEAT = 654321;

	private static final int FINGERPRINT_CHECK = 223;

	private static final int FINGERPRINT_ENROLL = 224;

	private String mFormPath;
	public static String mInstancePath;
	private GestureDetector mGestureDetector;

	public static FormController mFormController;

	private Animation mInAnimation;
	private Animation mOutAnimation;

	private RelativeLayout mRelativeLayout;
	private View mCurrentView;

	private AlertDialog mAlertDialog;
	private ProgressDialog mProgressDialog;
	private String mErrorMessage;

	// used to limit forward/backward swipes to one per question
	private boolean mBeenSwiped;

	private FormLoaderTask mFormLoaderTask;
	private SaveToDiskTask mSaveToDiskTask;

	enum AnimationType {
		LEFT, RIGHT, FADE
	}

	// ##### private fields Declaration
	private String patientCode;
	private String rmpId;
	private String docId;
	private boolean isInvalid = false;
	private Dialog docList;

	/** Called when the activity is first created. */
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

		Intent i = getIntent();
		if (i != null) {
			rmpId = i.getStringExtra("rmpId");
		}

		setContentView(R.layout.form_entry);
		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.loading_form));

		mRelativeLayout = (RelativeLayout) findViewById(R.id.rl);

		mBeenSwiped = false;
		mAlertDialog = null;
		mCurrentView = null;
		mInAnimation = null;
		mOutAnimation = null;
		mGestureDetector = new GestureDetector(this);

		// Load JavaRosa modules. needed to restore forms.
		new XFormsModule().registerModule();

		// needed to override rms property manager
		org.javarosa.core.services.PropertyManager
				.setPropertyManager(new PropertyManager(getApplicationContext()));

		Boolean newForm = true;
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(KEY_FORMPATH)) {
				mFormPath = savedInstanceState.getString(KEY_FORMPATH);
			}
			if (savedInstanceState.containsKey(NEWFORM)) {
				newForm = savedInstanceState.getBoolean(NEWFORM, true);
			}
			if (savedInstanceState.containsKey(KEY_ERROR)) {
				mErrorMessage = savedInstanceState.getString(KEY_ERROR);
			}
		}

		// If a parse error message is showing then nothing else is loaded
		// Dialogs mid form just disappear on rotation.
		if (mErrorMessage != null) {
			createErrorDialog(mErrorMessage, EXIT);
			return;
		}

		// Check to see if this is a screen flip or a new form load.
		final Object data = getLastNonConfigurationInstance();
		if (data instanceof FormLoaderTask) {
			mFormLoaderTask = (FormLoaderTask) data;
		} else if (data instanceof SaveToDiskTask) {
			mSaveToDiskTask = (SaveToDiskTask) data;
		} else if (data == null) {
			if (!newForm) {
				refreshCurrentView();
				return;
			}

			// Not a restart from a screen orientation change (or other).
			mFormController = null;
			mInstancePath = null;

			final Intent intent = getIntent();
			if (intent != null) {
				final Uri uri = intent.getData();

				if (getContentResolver().getType(uri) == InstanceColumns.CONTENT_ITEM_TYPE) {
					final Cursor instanceCursor = managedQuery(uri, null, null,
							null, null);
					if (instanceCursor.getCount() != 1) {
						createErrorDialog("Bad URI: " + uri, EXIT);
						return;
					} else {
						instanceCursor.moveToFirst();
						mInstancePath = instanceCursor
								.getString(instanceCursor
										.getColumnIndex(InstanceColumns.INSTANCE_FILE_PATH));

						final String jrFormId = instanceCursor
								.getString(instanceCursor
										.getColumnIndex(InstanceColumns.JR_FORM_ID));

						final String[] selectionArgs = { jrFormId };
						final String selection = FormsColumns.JR_FORM_ID
								+ " like ?";

						final Cursor formCursor = managedQuery(
								FormsColumns.CONTENT_URI, null, selection,
								selectionArgs, null);
						if (formCursor.getCount() == 1) {
							formCursor.moveToFirst();
							mFormPath = formCursor
									.getString(formCursor
											.getColumnIndex(FormsColumns.FORM_FILE_PATH));
						} else if (formCursor.getCount() < 1) {
							createErrorDialog("Parent form does not exist",
									EXIT);
							return;
						} else if (formCursor.getCount() > 1) {
							createErrorDialog(
									"More than one possible parent form", EXIT);
							return;
						}

					}

				} else if (getContentResolver().getType(uri) == FormsColumns.CONTENT_ITEM_TYPE) {
					final Cursor c = managedQuery(uri, null, null, null, null);
					if (c.getCount() != 1) {
						createErrorDialog("Bad URI: " + uri, EXIT);
						return;
					} else {
						c.moveToFirst();
						mFormPath = c.getString(c
								.getColumnIndex(FormsColumns.FORM_FILE_PATH));
					}
				} else {
					Log.e(t, "unrecognized URI");
					createErrorDialog("unrecognized URI: " + uri, EXIT);
					return;
				}

				mFormLoaderTask = new FormLoaderTask();
				mFormLoaderTask.execute(mFormPath);
				showDialog(PROGRESS_DIALOG);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_FORMPATH, mFormPath);
		outState.putBoolean(NEWFORM, false);
		outState.putString(KEY_ERROR, mErrorMessage);
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode == RESULT_CANCELED)
			// request was canceled, so do nothing
			return;

		ContentValues values;
		Uri imageURI;
		switch (requestCode) {
		case BARCODE_CAPTURE:
			final String sb = intent.getStringExtra("SCAN_RESULT");
			((MIntelView) mCurrentView).setBinaryData(sb);

			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			break;
		case IMAGE_CAPTURE:
			/*
			 * We saved the image to the tempfile_path, but we really want it to
			 * be in: /sdcard/mintel/instances/[current instnace]/something.jpg
			 * so we move it there before inserting it into the content
			 * provider. Once the android image capture bug gets fixed, (read,
			 * we move on from Android 1.6) we want to handle images the audio
			 * and video
			 */
			// The intent is empty, but we know we saved the image to the temp
			// file
			final File fi = new File(MIntel.TMPFILE_PATH);

			final String mInstanceFolder = mInstancePath.substring(0,
					mInstancePath.lastIndexOf("/") + 1);
			final String s = mInstanceFolder + "/" + System.currentTimeMillis()
					+ ".jpg";

			final File nf = new File(s);
			if (!fi.renameTo(nf)) {
				Log.e(t, "Failed to rename " + fi.getAbsolutePath());
			} else {
				Log.i(t,
						"renamed " + fi.getAbsolutePath() + " to "
								+ nf.getAbsolutePath());
			}

			// Add the new image to the Media content provider so that the
			// viewing is fast in Android 2.0+
			values = new ContentValues(6);
			values.put(MediaColumns.TITLE, nf.getName());
			values.put(MediaColumns.DISPLAY_NAME, nf.getName());
			values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
			values.put(MediaColumns.MIME_TYPE, "image/jpeg");
			values.put(MediaColumns.DATA, nf.getAbsolutePath());

			imageURI = getContentResolver().insert(
					Images.Media.EXTERNAL_CONTENT_URI, values);
			Log.i(t, "Inserting image returned uri = " + imageURI.toString());

			((MIntelView) mCurrentView).setBinaryData(imageURI);
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			refreshCurrentView();
			break;
		case IMAGE_CHOOSER:
			/*
			 * We have a saved image somewhere, but we really want it to be in:
			 * /sdcard/mintel/instances/[current instnace]/something.jpg so we
			 * move it there before inserting it into the content provider. Once
			 * the android image capture bug gets fixed, (read, we move on from
			 * Android 1.6) we want to handle images the audio and video
			 */

			// get gp of chosen file
			String sourceImagePath = null;
			final Uri selectedImage = intent.getData();
			if (selectedImage.toString().startsWith("file")) {
				sourceImagePath = selectedImage.toString().substring(6);
			} else {
				final String[] projection = { MediaColumns.DATA };
				final Cursor cursor = managedQuery(selectedImage, projection,
						null, null, null);
				startManagingCursor(cursor);
				final int column_index = cursor
						.getColumnIndexOrThrow(MediaColumns.DATA);
				cursor.moveToFirst();
				sourceImagePath = cursor.getString(column_index);
			}

			// Copy file to sdcard
			final String mInstanceFolder1 = mInstancePath.substring(0,
					mInstancePath.lastIndexOf("/") + 1);
			final String destImagePath = mInstanceFolder1 + "/"
					+ System.currentTimeMillis() + ".jpg";

			final File source = new File(sourceImagePath);
			final File newImage = new File(destImagePath);
			FileUtils.copyFile(source, newImage);

			if (newImage.exists()) {
				// Add the new image to the Media content provider so that the
				// viewing is fast in Android 2.0+
				values = new ContentValues(6);
				values.put(MediaColumns.TITLE, newImage.getName());
				values.put(MediaColumns.DISPLAY_NAME, newImage.getName());
				values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
				values.put(MediaColumns.MIME_TYPE, "image/jpeg");
				values.put(MediaColumns.DATA, newImage.getAbsolutePath());

				imageURI = getContentResolver().insert(
						Images.Media.EXTERNAL_CONTENT_URI, values);
				Log.i(t,
						"Inserting image returned uri = " + imageURI.toString());

				((MIntelView) mCurrentView).setBinaryData(imageURI);
				saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			} else {
				Log.e(t, "NO IMAGE EXISTS at: " + source.getAbsolutePath());
			}
			refreshCurrentView();
			break;
		case AUDIO_CAPTURE:
		case VIDEO_CAPTURE:
		case AUDIO_CHOOSER:
		case VIDEO_CHOOSER:
			// For audio/video capture/chooser, we get the URI from the content
			// provider
			// then the widget copies the file and makes a new entry in the
			// content provider.
			final Uri media = intent.getData();
			((MIntelView) mCurrentView).setBinaryData(media);
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			refreshCurrentView();
			break;
		case LOCATION_CAPTURE:
			final String sl = intent.getStringExtra(LOCATION_RESULT);
			((MIntelView) mCurrentView).setBinaryData(sl);
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			break;
		case HIERARCHY_ACTIVITY:
			// We may have jumped to a new index in hierarchy activity, so
			// refresh
			refreshCurrentView();
			break;
		// returns with a form path, start entry
		case INSTANCE_UPLOADER:
			if (intent.getBooleanExtra(FormEntryActivity.KEY_SUCCESS, false)) {
				// TODO NEED TO SET UP VOICE NOTIFICATION
				MediaPlayer mp = MediaPlayer.create(
						getApplicationContext(),
						Uri.fromFile(new File(MIntel.METADATA_PATH
								+ "/form_s.ogg")));
				try {
					mp.prepareAsync();
					mp.setOnPreparedListener(new OnPreparedListener() {

						@Override
						public void onPrepared(MediaPlayer mp) {
							mp.start();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				finish();
			} else {
				createErrorDialog(
						"ফর্ম পাঠানো হয়নি। আপনি \"ফর্ম এর ডাটা পাঠান\" এ গিয়ে ফর্ম পাঠিয়ে  দিন।",
						true);
				// finish();
			}
			break;
		case FINGERPRINT_ENROLL:
			String euid = intent.getStringExtra("UID");
			JSONObject eres;
			String uid1 = null;
			try {
				eres = new JSONObject(euid);
				uid1 = (String) eres.get("UID");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.w("Result: ", euid);
			((MIntelView)mCurrentView).setStringData(uid1);
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			break;
		case FINGERPRINT_CHECK:
			String cuid = intent.getStringExtra("UID");
			JSONObject res;
			String uid = null;
			try {
				res = new JSONObject(cuid);
				uid = (String) res.get("UID");
			} catch (JSONException e) {
				uid = "";
			}
			Log.w("Result: ","CUID RES:"+ cuid);
			((MIntelView)mCurrentView).setStringData(uid);
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			break;
		default:
			break;
		}
	}

	/**
	 * Refreshes the current view. the controller and the displayed view can get
	 * out of sync due to dialogs and restarts caused by screen orientation
	 * changes, so they're resynchronized here.
	 */
	public void refreshCurrentView() {
		int event = mFormController.getEvent();

		// When we refresh, repeat dialog state isn't maintained, so step back
		// to the previous
		// question.
		// Also, if we're within a group labeled 'field list', step back to the
		// beginning of that
		// group.
		// That is, skip backwards over repeat prompts, groups that are not
		// field-lists,
		// repeat events, and indexes in field-lists that is not the containing
		// group.
		while (event == FormEntryController.EVENT_PROMPT_NEW_REPEAT
				|| event == FormEntryController.EVENT_GROUP
				&& !mFormController.indexIsInFieldList()
				|| event == FormEntryController.EVENT_REPEAT
				|| mFormController.indexIsInFieldList()
				&& !(event == FormEntryController.EVENT_GROUP)) {
			event = mFormController.stepToPreviousEvent();
		}
		final View current = createView(event);
		showView(current, AnimationType.FADE);

	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		menu.removeItem(MENU_LANGUAGES);
		menu.removeItem(MENU_HIERARCHY_VIEW);
		menu.removeItem(MENU_SAVE);
		menu.removeItem(MENU_PREFERENCES);

		menu.add(0, MENU_SAVE, 0, R.string.save_all_answers).setIcon(
				android.R.drawable.ic_menu_save);
		menu.add(0, MENU_HIERARCHY_VIEW, 0, getString(R.string.view_hierarchy))
				.setIcon(R.drawable.ic_menu_goto);
		/*
		 * menu.add(0, MENU_LANGUAGES, 0, getString(R.string.change_language))
		 * .setIcon(R.drawable.ic_menu_start_conversation) .setEnabled(
		 * (mFormController.getLanguages() == null || mFormController
		 * .getLanguages().length == 1) ? false : true);
		 */
		menu.add(0, MENU_PREFERENCES, 0,
				getString(R.string.general_preferences)).setIcon(
				android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_LANGUAGES:
			createLanguageDialog();
			return true;
		case MENU_SAVE:
			// don't exit
			saveDataToDisk(DO_NOT_EXIT, isInstanceComplete(false), null);
			return true;
		case MENU_HIERARCHY_VIEW:
			if (currentPromptIsQuestion()) {
				saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			}
			final Intent i = new Intent(this, FormHierarchyActivity.class);
			startActivityForResult(i, HIERARCHY_ACTIVITY);
			return true;
		case MENU_PREFERENCES:
			final Intent pref = new Intent(this, PreferencesActivity.class);
			startActivity(pref);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * @return true if the current View represents a question in the form
	 */
	private boolean currentPromptIsQuestion() {
		return mFormController.getEvent() == FormEntryController.EVENT_QUESTION
				|| mFormController.getEvent() == FormEntryController.EVENT_GROUP;
	}

	/**
	 * Attempt to save the answer(s) in the current screen to into the data
	 * model.
	 *
	 * @param evaluateConstraints
	 * @return false if any error occurs while saving (constraint violated,
	 *         etc...), true otherwise.
	 */
	private boolean saveAnswersForCurrentScreen(boolean evaluateConstraints) {
		// only try to save if the current event is a question or a field-list
		// group
		if (mFormController.getEvent() == FormEntryController.EVENT_QUESTION
				|| mFormController.getEvent() == FormEntryController.EVENT_GROUP
				&& mFormController.indexIsInFieldList()) {
			final LinkedHashMap<FormIndex, IAnswerData> answers = ((MIntelView) mCurrentView)
					.getAnswers();
			final Iterator<FormIndex> it = answers.keySet().iterator();
			while (it.hasNext()) {
				final FormIndex index = it.next();
				// Within a group, you can only save for question events
				if (mFormController.getEvent(index) == FormEntryController.EVENT_QUESTION) {
					int saveStatus = saveAnswer(answers.get(index), index,
							evaluateConstraints);

					if (mFormController.getEvent(index) == FormEntryController.EVENT_QUESTION) {
						if (mFormController.getQuestionPrompt(index)
								.getQuestion().getTextID()
								.endsWith("Patient_ID:label")) {
							if (answers.get(index) != null) {
								patientCode = (String) answers.get(index)
										.getValue();
							}
						} else if (mFormController.getQuestionPrompt(index)
								.getQuestion().getTextID()
								.endsWith("Visit_type:label")) {
							isInvalid = (Integer
									.parseInt(((SelectOneData) answers
											.get(index)).getDisplayText()) > 1) ? isInvalid
									: false;
							if (isInvalid) {
								createConstraintToast("রোগীর কোনও ফলো আপ নেই",
										saveStatus);
								return false;
							}
						}else if (mFormController.getQuestionPrompt(index)
								.getQuestion().getTextID()
								.endsWith("Doctor_id:label")) {
							if (isInvalid) {
								createConstraintToast("ডাক্তার এর লিস্ট আসছে। কিছুক্ষণ অপেক্ষা করুন।",
										saveStatus);
								return false;
							}
						}
						else if (mFormController.getQuestionPrompt(index)
								.getQuestion().getTextID()
								.endsWith("Fingerprint_enroll:label")) {
							if (isInvalid) {
								createConstraintToast("আপনার হাতের ছাপ সঠিক হয় নাই।",
										saveStatus);
								return false;
							}
						}
					}
					if (evaluateConstraints
							&& saveStatus != FormEntryController.ANSWER_OK) {
						createConstraintToast(mFormController
								.getQuestionPrompt(index).getConstraintText(),
								saveStatus);
						return false;
					}
				} else {
					Log.w(t,
							"Attempted to save an index referencing something other than a question: "
									+ index.getReference());
				}
			}
		}
		return true;
	}

	/**
	 * Clears the answer on the screen.
	 */
	private void clearAnswer(final QuestionWidget qw) {
		qw.clearAnswer();
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, v.getId(), 0, getString(R.string.clear_answer));
		if (mFormController.indexContainsRepeatableGroup()) {
			menu.add(0, DELETE_REPEAT, 0, getString(R.string.delete_repeat));
		}
		menu.setHeaderTitle(getString(R.string.edit_prompt));
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		/*
		 * We don't have the right view here, so we store the View's ID as the
		 * item ID and loop through the possible views to find the one the user
		 * clicked on.
		 */
		for (final QuestionWidget qw : ((MIntelView) mCurrentView).getWidgets()) {
			if (item.getItemId() == qw.getId()) {
				createClearDialog(qw);
			}
		}
		if (item.getItemId() == DELETE_REPEAT) {
			createDeleteRepeatConfirmDialog();
		}

		return super.onContextItemSelected(item);
	}

	/**
	 * If we're loading, then we pass the loading thread to our next instance.
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		// if a form is loading, pass the loader task
		if (mFormLoaderTask != null
				&& mFormLoaderTask.getStatus() != AsyncTask.Status.FINISHED)
			return mFormLoaderTask;

		// if a form is writing to disk, pass the save to disk task
		if (mSaveToDiskTask != null
				&& mSaveToDiskTask.getStatus() != AsyncTask.Status.FINISHED)
			return mSaveToDiskTask;

		// mFormEntryController is static so we don't need to pass it.
		if (mFormController != null && currentPromptIsQuestion()) {
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
		}
		return null;
	}

	/**
	 * Creates a view given the View type and an event
	 *
	 * @param event
	 * @return newly created View
	 */
	private View createView(final int event) {
		setTitle(getString(R.string.app_name) + " > "
				+ mFormController.getFormTitle());

		switch (event) {
		case FormEntryController.EVENT_BEGINNING_OF_FORM:
			final View startView = View.inflate(this,
					R.layout.form_entry_start, null);
			setTitle(getString(R.string.app_name) + " > "
					+ mFormController.getFormTitle());
			((TextView) startView.findViewById(R.id.description))
					.setText(getString(R.string.enter_data_description,
							mFormController.getFormTitle()));

			Drawable image = null;
			final String[] projection = { FormsColumns.FORM_MEDIA_PATH };
			final String selection = FormsColumns.FORM_FILE_PATH + "=?";
			final String[] selectionArgs = { mFormPath };
			final Cursor c = managedQuery(FormsColumns.CONTENT_URI, projection,
					selection, selectionArgs, null);
			String mediaDir = null;
			if (c.getCount() < 1) {
				createErrorDialog("form Doesn't exist", true);
				return new View(this);
			} else {
				c.moveToFirst();
				mediaDir = c.getString(c
						.getColumnIndex(FormsColumns.FORM_MEDIA_PATH));
			}

			BitmapDrawable bitImage = null;
			// attempt to load the form-specific logo...
			// this is arbitrarily silly
			bitImage = new BitmapDrawable(mediaDir + "/form_logo.png");

			if (bitImage != null && bitImage.getBitmap() != null
					&& bitImage.getIntrinsicHeight() > 0
					&& bitImage.getIntrinsicWidth() > 0) {
				image = bitImage;
			}

			if (image == null) {
				// show the opendatakit zig...
				// image =
				// getResources().getDrawable(R.drawable.opendatakit_zig);
				((ImageView) startView.findViewById(R.id.form_start_bling))
						.setVisibility(View.GONE);
			} else {
				((ImageView) startView.findViewById(R.id.form_start_bling))
						.setImageDrawable(image);
			}

			return startView;
		case FormEntryController.EVENT_END_OF_FORM:
			final View endView = View.inflate(this, R.layout.form_entry_end,
					null);
			((TextView) endView.findViewById(R.id.description))
					.setText(getString(R.string.save_enter_data_description,
							mFormController.getFormTitle()));

			// checkbox for if finished or ready to send
			final CheckBox instanceComplete = (CheckBox) endView
					.findViewById(R.id.mark_finished);
			instanceComplete.setChecked(isInstanceComplete(true));

			// edittext to change the displayed name of the instance
			final EditText saveAs = (EditText) endView
					.findViewById(R.id.save_name);

			// disallow carriage returns in the name
			final InputFilter returnFilter = new InputFilter() {
				@Override
				public CharSequence filter(final CharSequence source,
						final int start, final int end, final Spanned dest,
						final int dstart, final int dend) {
					for (int i = start; i < end; i++) {
						if (Character.getType(source.charAt(i)) == Character.CONTROL)
							return "";
					}
					return null;
				}
			};
			saveAs.setFilters(new InputFilter[] { returnFilter });

			String saveName = mFormController.getFormTitle();
			if (getContentResolver().getType(getIntent().getData()) == InstanceColumns.CONTENT_ITEM_TYPE) {
				final Uri instanceUri = getIntent().getData();
				final Cursor instance = managedQuery(instanceUri, null, null,
						null, null);
				if (instance.getCount() == 1) {
					instance.moveToFirst();
					saveName = instance.getString(instance
							.getColumnIndex(InstanceColumns.DISPLAY_NAME))
							+ " ("
							+ instance.getString(instance
									.getColumnIndex(BaseColumns._ID)) + ")";
				}
			}

			saveAs.setText(saveName);

			// Create 'save' button
			((Button) endView.findViewById(R.id.save_exit_button))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(final View v) {
							// Form is marked as 'saved' here.
							if (saveAs.getText().length() < 1) {
								Toast.makeText(FormEntryActivity.this,
										R.string.save_as_error,
										Toast.LENGTH_SHORT).show();
							} else {
								saveDataToDisk(EXIT, instanceComplete
										.isChecked(), saveAs.getText()
										.toString());
							}
						}
					});

			return endView;
		case FormEntryController.EVENT_QUESTION:
		case FormEntryController.EVENT_GROUP:
			MIntelView mintelv = null;
			// should only be a group here if the event_group is a field-list
			try {
				mintelv = new MIntelView(this,
						mFormController.getQuestionPrompts(),
						mFormController.getGroupsForCurrentIndex());
				Log.i(t, "created view for group");
			} catch (final RuntimeException e) {
				createErrorDialog(e.getMessage(), EXIT);
				e.printStackTrace();
				// this is badness to avoid a crash.
				// really a next view should increment the formcontroller,
				// create the view
				// if the view is null, then keep the current view and pop an
				// error.
				return new View(this);
			}

			// Makes a "clear answer" menu pop up on long-click
			for (final QuestionWidget qw : mintelv.getWidgets()) {
				if (!qw.getPrompt().isReadOnly()) {
					registerForContextMenu(qw);
				}
			}

			return mintelv;
		default:
			Log.e(t, "Attempted to create a view that does not exist.");
			return null;
		}
	}

	@Override
	public boolean dispatchTouchEvent(final MotionEvent mv) {
		final boolean handled = mGestureDetector.onTouchEvent(mv);
		if (!handled)
			return super.dispatchTouchEvent(mv);

		return handled; // this is always true
	}

	/**
	 * Determines what should be displayed on the screen. Possible options are:
	 * a question, an ask repeat dialog, or the submit screen. Also saves
	 * answers to the data model after checking constraints.
	 */
	private void showNextView() {
		if (currentPromptIsQuestion()) {
			if (!saveAnswersForCurrentScreen(EVALUATE_CONSTRAINTS))
				// A constraint was violated so a dialog should be showing.
				return;
		}

		if (mFormController.getEvent() != FormEntryController.EVENT_END_OF_FORM) {
			int event;
			group_skip: do {
				event = mFormController
						.stepToNextEvent(FormController.STEP_INTO_GROUP);
				boolean netValidate = false;
				switch (event) {
				case FormEntryController.EVENT_QUESTION:
					if (checkFollowup(event)) {
						// No other actions
					} else if (checkDoctorList(event)) {
					}else if (saveFinger(event)){
						// No other actions
					}else if(checkFinger(event)){
						
					}
					Log.w("View Created", mCurrentView == null ? "NULL":"CREATED");
				case FormEntryController.EVENT_END_OF_FORM:
					final View next = createView(event);
					showView(next, AnimationType.RIGHT);
					break group_skip;
				case FormEntryController.EVENT_PROMPT_NEW_REPEAT:
					createRepeatDialog();
					break group_skip;
				case FormEntryController.EVENT_GROUP:
					if (mFormController.indexIsInFieldList()
							&& mFormController.getQuestionPrompts().length != 0) {
						final View nextGroupView = createView(event);
						showView(nextGroupView, AnimationType.RIGHT);
						break group_skip;
					}
					// otherwise it's not a field-list group, so just skip it
					break;
				case FormEntryController.EVENT_REPEAT:
					Log.i(t, "repeat: "
							+ mFormController.getFormIndex().getReference());
					// skip repeats
					break;
				case FormEntryController.EVENT_REPEAT_JUNCTURE:
					Log.i(t, "repeat juncture: "
							+ mFormController.getFormIndex().getReference());
					// skip repeat junctures until we implement them
					break;
				default:
					Log.w(t,
							"JavaRosa added a new EVENT type and didn't tell us... shame on them.");
					break;

				}
			} while (event != FormEntryController.EVENT_END_OF_FORM);

		} else {
			mBeenSwiped = false;
		}
	}

	private boolean checkFinger(int event) {
		if (event == FormEntryController.EVENT_QUESTION) {
			if (mFormController.getQuestionPrompt().getQuestion().getTextID()
					.endsWith("Finger_check:label")) {
				Intent intent = new Intent(FormEntryActivity.this,
						BluetoothActivity.class);
				intent.putExtra("mod", FingerprintActivity.CHECK);
				
				// #####
				FormEntryActivity.this.startActivityForResult(intent, FINGERPRINT_CHECK);
				//((MIntelView)mCurrentView).setStringData("Sadat");
				isInvalid = true;
				return isInvalid;
			}
		}
		return false;
	}

	private boolean saveFinger(int event) {
		if (event == FormEntryController.EVENT_QUESTION) {
			if (mFormController.getQuestionPrompt().getQuestion().getTextID()
					.endsWith("Finger_save:label")) {
				
				Intent intent = new Intent(FormEntryActivity.this,
						BluetoothActivity.class);
				intent.putExtra("mod", FingerprintActivity.ENROLL);
				FormEntryActivity.this.startActivityForResult(intent, FINGERPRINT_ENROLL);
				isInvalid = true;
				return isInvalid;
			}
		}
		return false;
	}

	/**
	 * Determines what should be displayed between a question, or the start
	 * screen and displays the appropriate view. Also saves answers to the data
	 * model without checking constraints.
	 */
	private void showPreviousView() {
		// The answer is saved on a back swipe, but question constraints are
		// ignored.
		if (currentPromptIsQuestion()) {
			saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
		}

		if (mFormController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
			int event = mFormController.stepToPreviousEvent();

			while (event != FormEntryController.EVENT_BEGINNING_OF_FORM
					&& event != FormEntryController.EVENT_QUESTION
					&& !(event == FormEntryController.EVENT_GROUP
							&& mFormController.indexIsInFieldList() && mFormController
							.getQuestionPrompts().length != 0)) {
				event = mFormController.stepToPreviousEvent();
			}

			checkFollowup(event);
			checkDoctorList(event);
			final View next = createView(event);
			showView(next, AnimationType.LEFT);

		} else {
			mBeenSwiped = false;
		}
	}

	/**
	 * Displays the View specified by the parameter 'next', animating both the
	 * current view and next appropriately given the AnimationType. Also updates
	 * the progress bar.
	 */
	public void showView(final View next, final AnimationType from) {
		switch (from) {
		case RIGHT:
			mInAnimation = AnimationUtils.loadAnimation(this,
					R.anim.push_left_in);
			mOutAnimation = AnimationUtils.loadAnimation(this,
					R.anim.push_left_out);
			break;
		case LEFT:
			mInAnimation = AnimationUtils.loadAnimation(this,
					R.anim.push_right_in);
			mOutAnimation = AnimationUtils.loadAnimation(this,
					R.anim.push_right_out);
			break;
		case FADE:
			mInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
			mOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			break;
		}

		if (mCurrentView != null) {
			mCurrentView.startAnimation(mOutAnimation);
			mRelativeLayout.removeView(mCurrentView);
		}

		mInAnimation.setAnimationListener(this);

		final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		mCurrentView = next;
		mRelativeLayout.addView(mCurrentView, lp);

		mCurrentView.startAnimation(mInAnimation);
		if (mCurrentView instanceof MIntelView) {
			((MIntelView) mCurrentView).setFocus(this);
		} else {
			final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputManager.hideSoftInputFromWindow(mCurrentView.getWindowToken(),
					0);
		}
	}

	// Hopefully someday we can use managed dialogs when the bugs are fixed
	/*
	 * Ideally, we'd like to use Android to manage dialogs with onCreateDialog()
	 * and onPrepareDialog(), but dialogs with dynamic content are broken in 1.5
	 * (cupcake). We do use managed dialogs for our static loading
	 * ProgressDialog. The main issue we noticed and are waiting to see fixed
	 * is: onPrepareDialog() is not called after a screen orientation change.
	 * http://code.google.com/p/android/issues/detail?id=1639
	 */

	//
	/**
	 * Creates and displays a dialog displaying the violated constraint.
	 */
	private void createConstraintToast(String constraintText,
			final int saveStatus) {
		switch (saveStatus) {
		case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
			if (constraintText == null) {
				constraintText = getString(R.string.invalid_answer_error);
			}
			break;
		case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
			constraintText = getString(R.string.required_answer_error);
			break;
		}

		showCustomToast(constraintText, Toast.LENGTH_SHORT);
		mBeenSwiped = false;
	}

	/**
	 * Creates a toast with the specified message.
	 *
	 * @param message
	 */
	private void showCustomToast(final String message, final int duration) {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final View view = inflater.inflate(R.layout.toast_view, null);

		// set the text in the view
		final TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(message);

		final Toast t = new Toast(this);
		t.setView(view);
		t.setDuration(duration);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	/**
	 * Creates and displays a dialog asking the user if they'd like to create a
	 * repeat of the current group.
	 */
	private void createRepeatDialog() {
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		final DialogInterface.OnClickListener repeatListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int i) {
				switch (i) {
				case DialogInterface.BUTTON1: // yes, repeat
					try {
						mFormController.newRepeat();
					} catch (final XPathTypeMismatchException e) {
						FormEntryActivity.this.createErrorDialog(
								e.getMessage(), EXIT);
						return;
					}
					showNextView();
					break;
				case DialogInterface.BUTTON2: // no, no repeat
					showNextView();
					break;
				}
			}
		};
		if (mFormController.getLastRepeatCount() > 0) {
			mAlertDialog.setTitle(getString(R.string.leaving_repeat_ask));
			mAlertDialog.setMessage(getString(R.string.add_another_repeat,
					mFormController.getLastGroupText()));
			mAlertDialog.setButton(getString(R.string.add_another),
					repeatListener);
			mAlertDialog.setButton2(getString(R.string.leave_repeat_yes),
					repeatListener);

		} else {
			mAlertDialog.setTitle(getString(R.string.entering_repeat_ask));
			mAlertDialog.setMessage(getString(R.string.add_repeat,
					mFormController.getLastGroupText()));
			mAlertDialog.setButton(getString(R.string.entering_repeat),
					repeatListener);
			mAlertDialog.setButton2(getString(R.string.add_repeat_no),
					repeatListener);
		}
		mAlertDialog.setCancelable(false);
		mAlertDialog.show();
		mBeenSwiped = false;
	}

	/**
	 * Creates and displays dialog with the given errorMsg.
	 */
	private void createErrorDialog(final String errorMsg,
			final boolean shouldExit) {
		mErrorMessage = errorMsg;
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setTitle(getString(R.string.error_occured));
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

	/**
	 * Creates a confirm/cancel dialog for deleting repeats.
	 */
	private void createDeleteRepeatConfirmDialog() {
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		String name = mFormController.getLastRepeatedGroupName();
		final int repeatcount = mFormController
				.getLastRepeatedGroupRepeatCount();
		if (repeatcount != -1) {
			name += " (" + (repeatcount + 1) + ")";
		}
		mAlertDialog.setTitle(getString(R.string.delete_repeat_ask));
		mAlertDialog
				.setMessage(getString(R.string.delete_repeat_confirm, name));
		final DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int i) {
				switch (i) {
				case DialogInterface.BUTTON1: // yes
					mFormController.deleteRepeat();
					showPreviousView();
					break;
				case DialogInterface.BUTTON2: // no
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.discard_group), quitListener);
		mAlertDialog.setButton2(getString(R.string.delete_repeat_no),
				quitListener);
		mAlertDialog.show();
	}

	/**
	 * Saves data and writes it to disk. If exit is set, program will exit after
	 * save completes. Complete indicates whether the user has marked the
	 * isntancs as complete. If updatedSaveName is non-null, the instances
	 * content provider is updated with the new name
	 */
	private boolean saveDataToDisk(final boolean exit, final boolean complete,
			final String updatedSaveName) {
		// save current answer
		if (!saveAnswersForCurrentScreen(complete)) {
			Toast.makeText(this, getString(R.string.data_saved_error),
					Toast.LENGTH_SHORT).show();
			return false;
		}

		mSaveToDiskTask = new SaveToDiskTask(getIntent().getData(), exit,
				complete, updatedSaveName);
		mSaveToDiskTask.setFormSavedListener(this);
		mSaveToDiskTask.execute();
		showDialog(SAVING_DIALOG);

		return true;
	}

	/**
	 * Create a dialog with options to save and exit, save, or quit without
	 * saving
	 */
	private void createQuitDialog() {
		final String[] items = { getString(R.string.keep_changes),
				getString(R.string.do_not_save) };

		mAlertDialog = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setTitle(
						getString(R.string.quit_application,
								mFormController.getFormTitle()))
				.setNeutralButton(getString(R.string.do_not_exit),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int id) {

								dialog.cancel();

							}
						})
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						switch (which) {

						case 0: // save and exit
							saveDataToDisk(EXIT, isInstanceComplete(false),
									null);
							break;

						case 1: // discard changes and exit

							final String selection = InstanceColumns.INSTANCE_FILE_PATH
									+ " like '" + mInstancePath + "'";
							final Cursor c = FormEntryActivity.this
									.managedQuery(InstanceColumns.CONTENT_URI,
											null, selection, null, null);

							// if it's not already saved, erase everything
							if (c.getCount() < 1) {
								int images = 0;
								int audio = 0;
								int video = 0;
								// delete media first
								final String instanceFolder = mInstancePath.substring(
										0, mInstancePath.lastIndexOf("/") + 1);
								Log.i(t, "attempting to delete: "
										+ instanceFolder);

								final String where = MediaColumns.DATA
										+ " like '" + instanceFolder + "%'";

								final String[] projection = { BaseColumns._ID };

								// images
								final Cursor imageCursor = getContentResolver()
										.query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
												projection, where, null, null);
								if (imageCursor.getCount() > 0) {
									imageCursor.moveToFirst();
									final String id = imageCursor.getString(imageCursor
											.getColumnIndex(BaseColumns._ID));

									Log.i(t,
											"attempting to delete: "
													+ Uri.withAppendedPath(
															android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
															id));
									images = getContentResolver()
											.delete(Uri
													.withAppendedPath(
															android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
															id), null, null);
								}
								imageCursor.close();

								// audio
								final Cursor audioCursor = getContentResolver()
										.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
												projection, where, null, null);
								if (audioCursor.getCount() > 0) {
									audioCursor.moveToFirst();
									final String id = audioCursor.getString(imageCursor
											.getColumnIndex(BaseColumns._ID));

									Log.i(t,
											"attempting to delete: "
													+ Uri.withAppendedPath(
															MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
															id));
									audio = getContentResolver()
											.delete(Uri
													.withAppendedPath(
															MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
															id), null, null);
								}
								audioCursor.close();

								// video
								final Cursor videoCursor = getContentResolver()
										.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
												projection, where, null, null);
								if (videoCursor.getCount() > 0) {
									videoCursor.moveToFirst();
									final String id = videoCursor.getString(imageCursor
											.getColumnIndex(BaseColumns._ID));

									Log.i(t,
											"attempting to delete: "
													+ Uri.withAppendedPath(
															MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
															id));
									video = getContentResolver()
											.delete(Uri
													.withAppendedPath(
															MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
															id), null, null);
								}
								audioCursor.close();

								Log.i(t, "removed from content providers: "
										+ images + " image files, " + audio
										+ " audio files," + " and " + video
										+ " video files.");
								final File f = new File(instanceFolder);
								if (f.exists() && f.isDirectory()) {
									for (final File del : f.listFiles()) {
										Log.i(t,
												"deleting file: "
														+ del.getAbsolutePath());
										del.delete();
									}
									f.delete();
								}
							}

							finishReturnInstance();
							break;

						case 2:// do nothing
							break;
						}
					}
				}).create();
		mAlertDialog.show();
	}

	/**
	 * Confirm clear answer dialog
	 */
	private void createClearDialog(final QuestionWidget qw) {
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);

		mAlertDialog.setTitle(getString(R.string.clear_answer_ask));

		String question = qw.getPrompt().getLongText();
		if (question.length() > 50) {
			question = question.substring(0, 50) + "...";
		}

		mAlertDialog.setMessage(getString(R.string.clearanswer_confirm,
				question));

		final DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int i) {
				switch (i) {
				case DialogInterface.BUTTON1: // yes
					clearAnswer(qw);
					saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
					break;
				case DialogInterface.BUTTON2: // no
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog
				.setButton(getString(R.string.discard_answer), quitListener);
		mAlertDialog.setButton2(getString(R.string.clear_answer_no),
				quitListener);
		mAlertDialog.show();
	}

	/**
	 * Creates and displays a dialog allowing the user to set the language for
	 * the form.
	 */
	private void createLanguageDialog() {
		final String[] languages = mFormController.getLanguages();
		int selected = -1;
		if (languages != null) {
			final String language = mFormController.getLanguage();
			for (int i = 0; i < languages.length; i++) {
				if (language.equals(languages[i])) {
					selected = i;
				}
			}
		}
		mAlertDialog = new AlertDialog.Builder(this)
				.setSingleChoiceItems(languages, selected,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
								// Update the language in the content provider
								// when selecting a new
								// language
								final ContentValues values = new ContentValues();
								values.put(FormsColumns.LANGUAGE,
										languages[whichButton]);
								final String selection = FormsColumns.FORM_FILE_PATH
										+ "=?";
								final String selectArgs[] = { mFormPath };
								final int updated = getContentResolver()
										.update(FormsColumns.CONTENT_URI,
												values, selection, selectArgs);
								Log.i(t, "Updated language to: "
										+ languages[whichButton] + " in "
										+ updated + " rows");

								mFormController
										.setLanguage(languages[whichButton]);
								dialog.dismiss();
								if (currentPromptIsQuestion()) {
									saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
								}
								refreshCurrentView();
							}
						})
				.setTitle(getString(R.string.change_language))
				.setNegativeButton(getString(R.string.do_not_change),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int whichButton) {
							}
						}).create();
		mAlertDialog.show();
	}

	/**
	 * We use Android's dialog management for loading/saving progress dialogs
	 */
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
					mFormLoaderTask.setFormLoaderListener(null);
					mFormLoaderTask.cancel(true);
					finish();
				}
			};
			mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
			mProgressDialog.setTitle(getString(R.string.loading_form));
			mProgressDialog.setMessage(getString(R.string.please_wait));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setButton(getString(R.string.cancel_loading_form),
					loadingButtonListener);
			return mProgressDialog;
		case SAVING_DIALOG:
			mProgressDialog = new ProgressDialog(this);
			final DialogInterface.OnClickListener savingButtonListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog,
						final int which) {
					dialog.dismiss();
					mSaveToDiskTask.setFormSavedListener(null);
					mSaveToDiskTask.cancel(true);
				}
			};
			mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
			mProgressDialog.setTitle(getString(R.string.saving_form));
			mProgressDialog.setMessage(getString(R.string.please_wait));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setButton(getString(R.string.cancel),
					savingButtonListener);
			mProgressDialog.setButton(getString(R.string.cancel_saving_form),
					savingButtonListener);
			return mProgressDialog;
		}
		return null;
	}

	/**
	 * Dismiss any showing dialogs that we manually manage.
	 */
	private void dismissDialogs() {
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}

	@Override
	protected void onPause() {
		dismissDialogs();
		// make sure we're not already saving to disk. if we are, currentPrompt
		// is getting constantly updated
		if (mSaveToDiskTask != null
				&& mSaveToDiskTask.getStatus() != AsyncTask.Status.RUNNING) {
			if (mCurrentView != null && currentPromptIsQuestion()) {
				saveAnswersForCurrentScreen(DO_NOT_EVALUATE_CONSTRAINTS);
			}
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mFormLoaderTask != null) {
			mFormLoaderTask.setFormLoaderListener(this);
			if (mFormController != null
					&& mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
				dismissDialog(PROGRESS_DIALOG);
				refreshCurrentView();
			}
		}
		if (mSaveToDiskTask != null) {
			mSaveToDiskTask.setFormSavedListener(this);
		}
		if (mErrorMessage != null && mAlertDialog != null
				&& !mAlertDialog.isShowing()) {
			createErrorDialog(mErrorMessage, EXIT);
			return;
		}
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			createQuitDialog();
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (event.isAltPressed() && !mBeenSwiped) {
				mBeenSwiped = true;
				showNextView();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (event.isAltPressed() && !mBeenSwiped) {
				mBeenSwiped = true;
				showPreviousView();
				return true;
			}
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		if (mFormLoaderTask != null) {
			mFormLoaderTask.setFormLoaderListener(null);
			// We have to call cancel to terminate the thread, otherwise it
			// lives on and retains the FEC in memory.
			// but only if it's done, otherwise the thread never returns
			if (mFormLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
				mFormLoaderTask.cancel(true);
				mFormLoaderTask.destroy();
			}
		}
		if (mSaveToDiskTask != null) {
			mSaveToDiskTask.setFormSavedListener(null);
			// We have to call cancel to terminate the thread, otherwise it
			// lives on and retains the FEC in memory.
			if (mSaveToDiskTask.getStatus() == AsyncTask.Status.FINISHED) {
				mSaveToDiskTask.cancel(false);
			}
		}

		super.onDestroy();

	}

	@Override
	public void onAnimationEnd(final Animation arg0) {
		mBeenSwiped = false;
	}

	@Override
	public void onAnimationRepeat(final Animation animation) {
		// Added by AnimationListener interface.
	}

	@Override
	public void onAnimationStart(final Animation animation) {
		// Added by AnimationListener interface.
	}

	/**
	 * loadingComplete() is called by FormLoaderTask once it has finished
	 * loading a form.
	 */
	@Override
	public void loadingComplete(final FormController fc) {
		dismissDialog(PROGRESS_DIALOG);

		mFormController = fc;

		// Set saved answer path
		if (mInstancePath == null) {

			// Create new answer folder.
			final String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
					.format(Calendar.getInstance().getTime());
			final String file = mFormPath.substring(
					mFormPath.lastIndexOf('/') + 1, mFormPath.lastIndexOf('.'));
			final String path = MIntel.INSTANCES_PATH + "/" + file + "_" + time;
			if (FileUtils.createFolder(path)) {
				mInstancePath = path + "/" + file + "_" + time + ".xml";
			}
		} else {
			// we've just loaded a saved form, so start in the hierarchy view
			final Intent i = new Intent(this, FormHierarchyActivity.class);
			startActivity(i);
			return; // so we don't show the intro screen before jumping to the
			// hierarchy
		}

		// Set the language if one has already been set in the past
		final String[] languageTest = mFormController.getLanguages();
		if (languageTest != null) {
			final String defaultLanguage = mFormController.getLanguage();
			String newLanguage = "";
			final String selection = FormsColumns.FORM_FILE_PATH + "=?";
			final String selectArgs[] = { mFormPath };
			final Cursor c = managedQuery(FormsColumns.CONTENT_URI, null,
					selection, selectArgs, null);
			if (c.getCount() == 1) {
				c.moveToFirst();
				newLanguage = c.getString(c
						.getColumnIndex(FormsColumns.LANGUAGE));
			}

			// if somehow we end up with a bad language, set it to the default
			try {
				mFormController.setLanguage(newLanguage);
			} catch (final Exception e) {
				mFormController.setLanguage(defaultLanguage);
			}
		}

		refreshCurrentView();
	}

	/**
	 * called by the FormLoaderTask if something goes wrong.
	 */
	@Override
	public void loadingError(final String errorMsg) {
		dismissDialog(PROGRESS_DIALOG);
		if (errorMsg != null) {
			createErrorDialog(errorMsg, EXIT);
		} else {
			createErrorDialog(getString(R.string.parse_error), EXIT);
		}
	}

	/**
	 * Called by SavetoDiskTask if everything saves correctly.
	 */
	@Override
	public void savingComplete(final int saveStatus) {
		dismissDialog(SAVING_DIALOG);
		Log.w("Save status", "" + saveStatus);
		switch (saveStatus) {
		case SaveToDiskTask.SAVED:
			Toast.makeText(this, getString(R.string.data_saved_ok),
					Toast.LENGTH_SHORT).show();
			break;
		case SaveToDiskTask.SAVED_AND_EXIT:
			Toast.makeText(this, getString(R.string.data_saved_ok),
					Toast.LENGTH_SHORT).show();
			finishReturnInstance();
			break;
		case SaveToDiskTask.SAVE_ERROR:
			Toast.makeText(this, getString(R.string.data_saved_error),
					Toast.LENGTH_LONG).show();
			break;
		case FormEntryController.ANSWER_CONSTRAINT_VIOLATED:
		case FormEntryController.ANSWER_REQUIRED_BUT_EMPTY:
			refreshCurrentView();
			// an answer constraint was violated, so do a 'swipe' to the next
			// question to display the proper toast(s)
			next();
			break;
		}
	}

	/**
	 * Attempts to save an answer to the specified index.
	 *
	 * @param answer
	 * @param index
	 * @param evaluateConstraints
	 * @return status as determined in FormEntryController
	 */
	public int saveAnswer(final IAnswerData answer, final FormIndex index,
			final boolean evaluateConstraints) {
		if (evaluateConstraints)
			return mFormController.answerQuestion(index, answer);
		else {
			mFormController.saveAnswer(index, answer);
			return FormEntryController.ANSWER_OK;
		}
	}

	/**
	 * Checks the database to determine if the current instance being edited has
	 * already been 'marked completed'. A form can be 'unmarked' complete and
	 * then resaved.
	 *
	 * @return true if form has been marked completed, false otherwise.
	 */
	private boolean isInstanceComplete(final boolean end) {
		// default to false if we're mid form
		boolean complete = false;

		// if we're at the end of the form, then check the preferences
		if (end) {
			// First get the value from the preferences
			// SharedPreferences sharedPreferences = PreferenceManager
			// .getDefaultSharedPreferences(this);
			// complete =
			// sharedPreferences.getBoolean(PreferencesActivity.KEY_COMPLETED_DEFAULT,
			// true);
			complete = true;
		}

		// Then see if we've already marked this form as complete before
		final String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
		final String[] selectionArgs = { mInstancePath };
		final Cursor c = getContentResolver().query(
				InstanceColumns.CONTENT_URI, null, selection, selectionArgs,
				null);
		startManagingCursor(c);
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			final String status = c.getString(c
					.getColumnIndex(InstanceColumns.STATUS));
			if (InstanceProviderAPI.STATUS_COMPLETE.compareTo(status) == 0) {
				complete = true;
			}
		}
		return complete;
	}

	public void next() {
		if (!mBeenSwiped) {
			mBeenSwiped = true;
			showNextView();
		}
	}

	/**
	 * Returns the instance that was just filled out to the calling activity, if
	 * requested.
	 */
	private void finishReturnInstance() {
		final String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_EDIT.equals(action)) {
			// caller is waiting on a picked form
			final String selection = InstanceColumns.INSTANCE_FILE_PATH + "=?";
			final String[] selectionArgs = { mInstancePath };
			final Cursor c = managedQuery(InstanceColumns.CONTENT_URI, null,
					selection, selectionArgs, null);
			if (c.getCount() > 0) {
				// should only be one...
				c.moveToFirst();
				final String id = c
						.getString(c.getColumnIndex(BaseColumns._ID));
				final Uri instance = Uri.withAppendedPath(
						InstanceColumns.CONTENT_URI, id);
				// ##### TODO Here is the cream of the cake
				Log.w("INSTANCE SAVED AND DATA RETRIEVED", "Returned Uri: "
						+ instance.toString());
				final long[] instanceIDs = { Long.parseLong(id) };
				// Done with the auto sending of the form data
				final Intent i = new Intent(this,
						InstanceUploaderActivity.class);
				i.putExtra(FormEntryActivity.KEY_INSTANCES, instanceIDs);
				startActivityForResult(i, INSTANCE_UPLOADER);
				setResult(RESULT_OK, new Intent().setData(instance));
			}
		}
		finish();
	}

	private static final int INSTANCE_UPLOADER = 0;

	@Override
	public boolean onDown(final MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(final MotionEvent e1, final MotionEvent e2,
			final float velocityX, final float velocityY) {
		// Looks for user swipes. If the user has swiped, move to the
		// appropriate screen.

		// for all screens a swipe is left/right of at least
		// .25" and up/down of less than .25"
		// OR left/right of > .5"
		final DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		final int xPixelLimit = (int) (dm.xdpi * .25);
		final int yPixelLimit = (int) (dm.ydpi * .25);

		if (Math.abs(e1.getX() - e2.getX()) > xPixelLimit
				&& Math.abs(e1.getY() - e2.getY()) < yPixelLimit
				|| Math.abs(e1.getX() - e2.getX()) > xPixelLimit * 2) {
			if (velocityX > 0) {
				mBeenSwiped = true;
				showPreviousView();
				return true;
			} else {
				mBeenSwiped = true;
				showNextView();
				return true;
			}
		}

		return false;
	}

	@Override
	public void onLongPress(final MotionEvent e) {
	}

	@Override
	public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
			final float distanceX, final float distanceY) {
		// The onFling() captures the 'up' event so our view thinks it gets long
		// pressed.
		// We don't wnat that, so cancel it.
		mCurrentView.cancelLongPress();
		return false;
	}

	@Override
	public void onShowPress(final MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(final MotionEvent e) {
		return false;
	}

	@Override
	public void advance() {
		next();
	}

	@Override
	public void isVisitValid(String response) {
		// Toast.makeText(this, response, 1000).show();
		if (Integer.parseInt(response) >= 2)
			isInvalid = false;
		else
			isInvalid = true;
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
		if (dataArr != null) {
			isInvalid = false;
			final DoctorListAdapter adapter = new DoctorListAdapter(
					FormEntryActivity.this, dataArr, 1);

			AlertDialog.Builder builder = new AlertDialog.Builder(
					FormEntryActivity.this);
			ListView list = new ListView(FormEntryActivity.this);
			ArrayAdapter<String> docAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, adapter.name);
			list.setAdapter(docAdapter);
			list.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					docId = adapter.id[position];

					// #####
					final Iterator<QuestionWidget> i = ((MIntelView) mCurrentView).widgets
							.iterator();
					while (i.hasNext()) {
						final QuestionWidget q = i.next();
						StringWidget ans = (StringWidget) q;
						ans.mAnswer.setText("");
						ans.mAnswer.setClickable(false);
						ans.mAnswer.setLongClickable(false);
					}

					final LinkedHashMap<FormIndex, IAnswerData> answers = ((MIntelView) mCurrentView)
							.getAnswers();
					final Iterator<FormIndex> it = answers.keySet().iterator();
					while (it.hasNext()) {
						final FormIndex index = it.next();
						if (mFormController.getEvent(index) == FormEntryController.EVENT_QUESTION) {
							final Iterator<QuestionWidget> ii = ((MIntelView) mCurrentView).widgets
									.iterator();
							while (ii.hasNext()) {
								final QuestionWidget q = ii.next();
								StringWidget ans = (StringWidget) q;
								ans.mAnswer.setText(docId);
							}
						}
					}
					// refreshCurrentView();
					// #####

					Log.w("Item Clicked", "" + docId);
					if (docList.isShowing())
						docList.dismiss();
				}
			});
			builder.setView(list);
			builder.setTitle("Doctor Selection List");
			docList = builder.create();
			docList.setCancelable(false);
			docList.show();
		}

	}

	private boolean checkDoctorList(int event) {
		if (event == FormEntryController.EVENT_QUESTION) {
			if (mFormController.getQuestionPrompt().getQuestion().getTextID()
					.endsWith("Doctor_id:label")) {
				DoctorListRetrievalTask visitValidation = new DoctorListRetrievalTask(
						this, this);
				visitValidation.execute(rmpId, patientCode);
				isInvalid = true;
				return isInvalid;
			}
		}
		return false;
	}

	private boolean checkFollowup(int event) {
		if (event == FormEntryController.EVENT_QUESTION) {
			if (mFormController.getQuestionPrompt().getQuestion().getTextID()
					.endsWith("Visit_type:label")) {
				VisitValidationTask visitValidation = new VisitValidationTask(
						this,this);
				visitValidation.execute(rmpId, patientCode);
				isInvalid = true;
				return isInvalid;
			}
		}
		return false;
	}

}