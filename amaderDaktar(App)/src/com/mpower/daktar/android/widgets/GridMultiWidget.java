package com.mpower.daktar.android.widgets;

import java.io.File;
import java.util.Vector;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.mpower.daktar.android.utilities.FileUtils;
import com.mpower.daktar.android.R;

/**
 * GridWidget handles multiple selection fields using a grid of icons. The user
 * clicks the desired icon and the background changes from black to orange. If
 * text, audio or video are specified in the select answers they are ignored.
 * This is almost identical to GridWidget, except multiple icons can be selected
 * simultaneously.
 *
 * @author Jeff Beorse (jeff@beorse.net)
 */
public class GridMultiWidget extends QuestionWidget {
	Vector<SelectChoice> mItems;

	// The possible select choices
	String[] choices;

	// The Gridview that will hol the icons
	GridView gridview;

	// Defines which icon is selected
	boolean[] selected;

	// The image views for each of the icons
	ImageView[] imageViews;

	// The number of columns in the grid, can be user defined
	int numColumns;

	// The max width of an icon in a given column. Used to line
	// up the columns and automatically fit the columns in when
	// they are chosen automatically
	int maxColumnWidth;

	// The RGB value for the orange background
	public static final int orangeRedVal = 255;
	public static final int orangeGreenVal = 140;
	public static final int orangeBlueVal = 0;

	@SuppressWarnings("unchecked")
	public GridMultiWidget(final Context context, final FormEntryPrompt prompt,
			final int numColumns) {
		super(context, prompt);
		mItems = prompt.getSelectChoices();
		mPrompt = prompt;

		selected = new boolean[mItems.size()];
		choices = new String[mItems.size()];
		gridview = new GridView(context);
		imageViews = new ImageView[mItems.size()];
		maxColumnWidth = -1;
		this.numColumns = numColumns;
		for (int i = 0; i < mItems.size(); i++) {
			imageViews[i] = new ImageView(getContext());
		}

		// Build view
		for (int i = 0; i < mItems.size(); i++) {
			final SelectChoice sc = mItems.get(i);
			// Read the image sizes and set maxColumnWidth. This allows us to
			// make sure all of our
			// columns are going to fit
			final String imageURI = prompt.getSpecialFormSelectChoiceText(sc,
					FormEntryCaption.TEXT_FORM_IMAGE);

			if (imageURI != null) {
				choices[i] = imageURI;

				String imageFilename;
				try {
					imageFilename = ReferenceManager._()
							.DeriveReference(imageURI).getLocalURI();
					final File imageFile = new File(imageFilename);
					if (imageFile.exists()) {
						final Display display = ((WindowManager) getContext()
								.getSystemService(Context.WINDOW_SERVICE))
								.getDefaultDisplay();
						final int screenWidth = display.getWidth();
						final int screenHeight = display.getHeight();
						final Bitmap b = FileUtils.getBitmapScaledToDisplay(
								imageFile, screenHeight, screenWidth);
						if (b != null) {

							if (b.getWidth() > maxColumnWidth) {
								maxColumnWidth = b.getWidth();
							}

						}
					}
				} catch (final InvalidReferenceException e) {
					Log.e("GridWidget", "image invalid reference exception");
					e.printStackTrace();
				}

			} else {
				choices[i] = prompt.getSelectChoiceText(sc);
			}

		}

		// Use the custom image adapter and initialize the grid view
		final ImageAdapter ia = new ImageAdapter(getContext(), choices);
		gridview.setAdapter(ia);
		gridview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View v,
					final int position, final long id) {

				if (selected[position]) {
					selected[position] = false;
					imageViews[position].setBackgroundColor(Color.WHITE);
				} else {
					selected[position] = true;
					imageViews[position].setBackgroundColor(Color.rgb(
							orangeRedVal, orangeGreenVal, orangeBlueVal));
				}

			}
		});

		// Read the screen dimensions and fit the grid view to them. It is
		// important that the grid
		// view
		// knows how far out it can stretch.
		final Display display = ((WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE)).getDefaultDisplay();
		final int screenWidth = display.getWidth();
		final int screenHeight = display.getHeight();
		final GridView.LayoutParams params = new GridView.LayoutParams(
				screenWidth - 5, screenHeight - 5);
		gridview.setLayoutParams(params);

		// Use the user's choice for num columns, otherwise automatically
		// decide.
		if (numColumns > 0) {
			gridview.setNumColumns(numColumns);
		} else {
			gridview.setNumColumns(GridView.AUTO_FIT);
		}

		gridview.setColumnWidth(maxColumnWidth);
		gridview.setHorizontalSpacing(2);
		gridview.setVerticalSpacing(2);
		gridview.setGravity(Gravity.LEFT);
		gridview.setStretchMode(GridView.NO_STRETCH);

		// Fill in answer
		final IAnswerData answer = prompt.getAnswerValue();
		Vector<Selection> ve;
		if (answer == null || answer.getValue() == null) {
			ve = new Vector<Selection>();
		} else {
			ve = (Vector<Selection>) answer.getValue();
		}

		for (int i = 0; i < choices.length; ++i) {

			final String value = mItems.get(i).getValue();
			boolean found = false;
			for (final Selection s : ve) {
				if (value.equals(s.getValue())) {
					found = true;
					break;
				}
			}

			selected[i] = found;
			if (selected[i]) {
				imageViews[i].setBackgroundColor(Color.rgb(orangeRedVal,
						orangeGreenVal, orangeBlueVal));
			} else {
				imageViews[i].setBackgroundColor(Color.WHITE);
			}
		}

		addView(gridview);
	}

	@Override
	public IAnswerData getAnswer() {
		final Vector<Selection> vc = new Vector<Selection>();
		for (int i = 0; i < mItems.size(); i++) {
			if (selected[i]) {
				final SelectChoice sc = mItems.get(i);
				vc.add(new Selection(sc));
			}
		}

		if (vc.size() == 0)
			return null;
		else
			return new SelectMultiData(vc);
	}

	@Override
	public void clearAnswer() {
		for (int i = 0; i < mItems.size(); ++i) {
			selected[i] = false;
			imageViews[i].setBackgroundColor(Color.WHITE);
		}

	}

	@Override
	public void setFocus(final Context context) {
		// Hide the soft keyboard if it's showing.
		final InputMethodManager inputManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getWindowToken(), 0);

	}

	// Custom image adapter. Most of the code is copied from
	// media layout for using a picture.
	private class ImageAdapter extends BaseAdapter {
		private final String[] choices;

		public ImageAdapter(final Context c, final String[] choices) {
			this.choices = choices;
		}

		@Override
		public int getCount() {
			return choices.length;
		}

		@Override
		public Object getItem(final int position) {
			return null;
		}

		@Override
		public long getItemId(final int position) {
			return 0;
		}

		// create a new ImageView for each item referenced by the Adapter
		@Override
		public View getView(final int position, final View convertView,
				final ViewGroup parent) {
			final String imageURI = choices[position];

			// It is possible that an imageview already exists and has been
			// updated
			// by updateViewAfterAnswer
			ImageView mImageView = null;
			if (imageViews[position] != null) {
				mImageView = imageViews[position];
			}
			TextView mMissingImage = null;

			String errorMsg = null;
			if (imageURI != null) {
				try {
					final String imageFilename = ReferenceManager._()
							.DeriveReference(imageURI).getLocalURI();
					final File imageFile = new File(imageFilename);
					if (imageFile.exists()) {
						final Display display = ((WindowManager) getContext()
								.getSystemService(Context.WINDOW_SERVICE))
								.getDefaultDisplay();
						final int screenWidth = display.getWidth();
						final int screenHeight = display.getHeight();
						final Bitmap b = FileUtils.getBitmapScaledToDisplay(
								imageFile, screenHeight, screenWidth);
						if (b != null) {

							if (mImageView == null) {
								mImageView = new ImageView(getContext());
								mImageView.setBackgroundColor(Color.WHITE);
							}

							mImageView.setPadding(3, 3, 3, 3);
							mImageView.setImageBitmap(b);

							imageViews[position] = mImageView;

						} else {
							// Loading the image failed, so it's likely a bad
							// file.
							errorMsg = getContext().getString(
									R.string.file_invalid, imageFile);
						}
					} else {
						// We should have an image, but the file doesn't exist.
						errorMsg = getContext().getString(
								R.string.file_missing, imageFile);
					}

					if (errorMsg != null) {
						// errorMsg is only set when an error has occurred
						Log.e("GridWidget", errorMsg);
						mMissingImage = new TextView(getContext());
						mMissingImage.setText(errorMsg);
						mMissingImage.setPadding(10, 10, 10, 10);
					}
				} catch (final InvalidReferenceException e) {
					Log.e("GridWidget", "image invalid reference exception");
					e.printStackTrace();
				}
			} else {
				// There's no imageURI listed, so just ignore it.
			}

			if (mImageView != null) {
				mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
				return mImageView;
			} else
				return mMissingImage;
		}
	}

	@Override
	public void setOnLongClickListener(final OnLongClickListener l) {
		gridview.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		gridview.cancelLongPress();
	}

}
