package com.mpower.daktar.android.widgets;

import java.io.File;
import java.util.Vector;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mpower.daktar.android.utilities.FileUtils;
import com.mpower.daktar.android.R;

/**
 * ListWidget handles select-one fields using radio buttons. The radio buttons
 * are aligned horizontally. They are typically meant to be used in a field
 * list, where multiple questions with the same multiple choice answers can sit
 * on top of each other and make a grid of buttons that is easy to navigate
 * quickly. Optionally, you can turn off the labels. This would be done if a
 * label widget was at the top of your field list to provide the labels. If
 * audio or video are specified in the select answers they are ignored.
 *
 * @author Jeff Beorse (jeff@beorse.net)
 */
public class ListWidget extends QuestionWidget implements
OnCheckedChangeListener {
	private static final int RANDOM_BUTTON_ID = 4853487;
	protected final static int TEXTSIZE = 21;
	private static final String t = "ListWidget";

	// Layout holds the horizontal list of buttons
	LinearLayout buttonLayout;

	// Holds the entire question and answers. It is a horizontally aligned
	// linear layout
	LinearLayout questionLayout;

	// Option to keep labels blank
	boolean displayLabel;

	Vector<SelectChoice> mItems;
	Vector<RadioButton> buttons;

	public ListWidget(final Context context, final FormEntryPrompt prompt,
			final boolean displayLabel) {
		super(context, prompt);

		mItems = prompt.getSelectChoices();
		buttons = new Vector<RadioButton>();

		this.displayLabel = displayLabel;

		buttonLayout = new LinearLayout(context);

		String s = null;
		if (prompt.getAnswerValue() != null) {
			s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
		}

		if (prompt.getSelectChoices() != null) {
			for (int i = 0; i < mItems.size(); i++) {
				final RadioButton r = new RadioButton(getContext());

				r.setOnCheckedChangeListener(this);
				r.setId(i + RANDOM_BUTTON_ID);
				r.setEnabled(!prompt.isReadOnly());
				r.setFocusable(!prompt.isReadOnly());

				buttons.add(r);

				if (mItems.get(i).getValue().equals(s)) {
					r.setChecked(true);
				}

				String imageURI = null;
				imageURI = prompt.getSpecialFormSelectChoiceText(mItems.get(i),
						FormEntryCaption.TEXT_FORM_IMAGE);

				// build image view (if an image is provided)
				ImageView mImageView = null;
				TextView mMissingImage = null;

				// Now set up the image view
				String errorMsg = null;
				if (imageURI != null) {
					try {
						final String imageFilename = ReferenceManager._()
								.DeriveReference(imageURI).getLocalURI();
						final File imageFile = new File(imageFilename);
						if (imageFile.exists()) {
							Bitmap b = null;
							try {
								final Display display = ((WindowManager) getContext()
										.getSystemService(
												Context.WINDOW_SERVICE))
												.getDefaultDisplay();
								final int screenWidth = display.getWidth();
								final int screenHeight = display.getHeight();
								b = FileUtils.getBitmapScaledToDisplay(
										imageFile, screenHeight, screenWidth);
							} catch (final OutOfMemoryError e) {
								errorMsg = "ERROR: " + e.getMessage();
							}

							if (b != null) {
								mImageView = new ImageView(getContext());
								mImageView.setPadding(2, 2, 2, 2);
								mImageView.setAdjustViewBounds(true);
								mImageView.setImageBitmap(b);
								mImageView.setId(23423534);
							} else if (errorMsg == null) {
								// An error hasn't been logged and loading the
								// image failed, so it's
								// likely
								// a bad file.
								errorMsg = getContext().getString(
										R.string.file_invalid, imageFile);

							}
						} else if (errorMsg == null) {
							// An error hasn't been logged. We should have an
							// image, but the file
							// doesn't
							// exist.
							errorMsg = getContext().getString(
									R.string.file_missing, imageFile);
						}

						if (errorMsg != null) {
							// errorMsg is only set when an error has occured
							Log.e(t, errorMsg);
							mMissingImage = new TextView(getContext());
							mMissingImage.setText(errorMsg);

							mMissingImage.setPadding(2, 2, 2, 2);
							mMissingImage.setId(234873453);
						}
					} catch (final InvalidReferenceException e) {
						Log.e(t, "image invalid reference exception");
						e.printStackTrace();
					}
				} else {
					// There's no imageURI listed, so just ignore it.
				}

				// build text label. Don't assign the text to the built in label
				// to he
				// button because it aligns horizontally, and we want the label
				// on top
				final TextView label = new TextView(getContext());
				label.setText(prompt.getSelectChoiceText(mItems.get(i)));
				label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXTSIZE);
				if (!displayLabel) {
					label.setVisibility(View.GONE);
				}

				// answer layout holds the label text/image on top and the radio
				// button on bottom
				final LinearLayout answer = new LinearLayout(getContext());
				answer.setOrientation(LinearLayout.VERTICAL);
				final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.TOP;
				answer.setLayoutParams(params);

				if (mImageView != null) {
					if (!displayLabel) {
						mImageView.setVisibility(View.GONE);
					}
					answer.addView(mImageView);
				} else if (mMissingImage != null) {
					answer.addView(mMissingImage);
				} else {
					if (displayLabel) {
						answer.addView(label);
					}

				}
				answer.addView(r);
				answer.setPadding(4, 0, 4, 0);

				// Each button gets equal weight
				final LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
						android.view.ViewGroup.LayoutParams.FILL_PARENT,
						android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				answerParams.weight = 1;

				buttonLayout.addView(answer, answerParams);

			}
		}

		// Align the buttons so that they appear horizonally and are right
		// justified
		// buttonLayout.setGravity(Gravity.RIGHT);
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
		// LinearLayout.LayoutParams params = new
		// LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
		// LayoutParams.WRAP_CONTENT);
		// buttonLayout.setLayoutParams(params);

		// The buttons take up the right half of the screen
		final LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		buttonParams.weight = 1;

		questionLayout.addView(buttonLayout, buttonParams);
		addView(questionLayout);

	}

	@Override
	public void clearAnswer() {
		for (final RadioButton button : buttons) {
			if (button.isChecked()) {
				button.setChecked(false);
				return;
			}
		}
	}

	@Override
	public IAnswerData getAnswer() {
		final int i = getCheckedId();
		if (i == -1)
			return null;
		else {
			final SelectChoice sc = mItems.elementAt(i - RANDOM_BUTTON_ID);
			return new SelectOneData(new Selection(sc));
		}
	}

	@Override
	public void setFocus(final Context context) {
		// Hide the soft keyboard if it's showing.
		final InputMethodManager inputManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getWindowToken(), 0);
	}

	public int getCheckedId() {
		for (final RadioButton button : buttons) {
			if (button.isChecked())
				return button.getId();
		}
		return -1;
	}

	@Override
	public void onCheckedChanged(final CompoundButton buttonView,
			final boolean isChecked) {
		if (!isChecked)
			// If it got unchecked, we don't care.
			return;

		for (final RadioButton button : buttons) {
			if (button.isChecked() && !(buttonView == button)) {
				button.setChecked(false);
			}
		}
	}

	// Override QuestionWidget's add question text. Build it the same
	// but add it to the relative layout
	@Override
	protected void addQuestionText(final FormEntryPrompt p) {

		// Add the text view. Textview always exists, regardless of whether
		// there's text.
		final TextView questionText = new TextView(getContext());
		questionText.setText(p.getLongText());
		questionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXTSIZE);
		questionText.setTypeface(null, Typeface.BOLD);
		questionText.setPadding(0, 0, 0, 7);
		questionText.setId(RANDOM_BUTTON_ID); // assign random id

		// Wrap to the size of the parent view
		questionText.setHorizontallyScrolling(false);

		if (p.getLongText() == null) {
			questionText.setVisibility(GONE);
		}

		// Put the question text on the left half of the screen
		final LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		labelParams.weight = 1;

		questionLayout = new LinearLayout(getContext());
		questionLayout.setOrientation(LinearLayout.HORIZONTAL);

		questionLayout.addView(questionText, labelParams);
	}

	@Override
	public void setOnLongClickListener(final OnLongClickListener l) {
		for (final RadioButton r : buttons) {
			r.setOnLongClickListener(l);
		}
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		for (final RadioButton r : buttons) {
			r.cancelLongPress();
		}
	}

}
