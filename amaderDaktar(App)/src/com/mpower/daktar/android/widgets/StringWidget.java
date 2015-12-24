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

package com.mpower.daktar.android.widgets;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;

/**
 * The most basic widget that allows for entry of any text.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class StringWidget extends QuestionWidget {

	boolean mReadOnly = false;
	public EditText mAnswer;

	public StringWidget(final Context context, final FormEntryPrompt prompt) {
		super(context, prompt);
		mAnswer = new EditText(context);

		mAnswer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);

		final TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);
		mAnswer.setLayoutParams(params);
		// mAnswer.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light));
		// mAnswer.setTextColor(context.getResources().getColor(android.R.color.white));
		// capitalize the first letter of the sentence
		mAnswer.setKeyListener(new TextKeyListener(Capitalize.SENTENCES, false));

		// needed to make long read only text scroll
		mAnswer.setHorizontallyScrolling(false);
		mAnswer.setSingleLine(false);

		if (prompt != null) {
			mReadOnly = prompt.isReadOnly();
			final String s = prompt.getAnswerText();
			if (s != null) {
				mAnswer.setText(s);
			}

			if (mReadOnly) {
				mAnswer.setBackgroundDrawable(null);
				mAnswer.setFocusable(false);
				mAnswer.setClickable(false);
			}
		}

		addView(mAnswer);
	}

	@Override
	public void clearAnswer() {
		mAnswer.setText(null);
	}
	
	public void setAnswer(String answer) {
		Log.w("Inside String Widget","Value: "+ answer);
		mAnswer.setText(answer);
	}

	@Override
	public IAnswerData getAnswer() {
		final String s = mAnswer.getText().toString();
		if (s == null || s.equals(""))
			return null;
		else
			return new StringData(s);
	}

	@Override
	public void setFocus(final Context context) {
		// Put focus on text input field and display soft keyboard if
		// appropriate.
		mAnswer.requestFocus();
		final InputMethodManager inputManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (!mReadOnly) {
			inputManager.showSoftInput(mAnswer, 0);
			/*
			 * If you do a multi-question screen after a "add another group"
			 * dialog, this won't automatically pop up. It's an Android issue.
			 *
			 * That is, if I have an edit text in an activity, and pop a dialog,
			 * and in that dialog's button's OnClick() I call
			 * edittext.requestFocus() and showSoftInput(edittext, 0),
			 * showSoftinput() returns false. However, if the edittext is
			 * focused before the dialog pops up, everything works fine. great.
			 */
		} else {
			inputManager.hideSoftInputFromWindow(mAnswer.getWindowToken(), 0);
		}
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (event.isAltPressed() == true)
			return false;
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void setOnLongClickListener(final OnLongClickListener l) {
		mAnswer.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		mAnswer.cancelLongPress();
	}

}
