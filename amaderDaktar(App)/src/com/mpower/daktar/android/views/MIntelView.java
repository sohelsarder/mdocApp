package com.mpower.daktar.android.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mpower.daktar.android.widgets.IBinaryWidget;
import com.mpower.daktar.android.widgets.QuestionWidget;
import com.mpower.daktar.android.widgets.StringWidget;
import com.mpower.daktar.android.widgets.WidgetFactory;

/**
 * This class is
 *
 * @author carlhartung
 */
public class MIntelView extends ScrollView implements OnLongClickListener {

	// starter random number for view IDs
	private final static int VIEW_ID = 12345;

	private final static String t = "CLASSNAME";
	private final static int TEXTSIZE = 21;

	private final LinearLayout mView;
	private final LinearLayout.LayoutParams mLayout;
	public final ArrayList<QuestionWidget> widgets;

	public final static String FIELD_LIST = "field-list";

	public MIntelView(final Context context,
			final FormEntryPrompt questionPrompt,
			final FormEntryCaption[] groups) {
		this(context, new FormEntryPrompt[] { questionPrompt }, groups);
	}

	public MIntelView(final Context context,
			final FormEntryPrompt[] questionPrompts,
			final FormEntryCaption[] groups) {
		super(context);

		widgets = new ArrayList<QuestionWidget>();

		mView = new LinearLayout(getContext());
		mView.setOrientation(LinearLayout.VERTICAL);
		mView.setGravity(Gravity.TOP);
		mView.setPadding(0, 7, 0, 0);

		mLayout = new LinearLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
		mLayout.setMargins(10, 0, 10, 0);

		// display which group you are in as well as the question

		addGroupText(groups);
		boolean first = true;
		int id = 0;
		for (final FormEntryPrompt p : questionPrompts) {
			if (!first) {
				final View divider = new View(getContext());
				divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
				divider.setMinimumHeight(3);
				mView.addView(divider);
			} else {
				first = false;
			}

			// if question or answer type is not supported, use text widget
			final QuestionWidget qw = WidgetFactory.createWidgetFromPrompt(p,
					getContext());
			qw.setLongClickable(true);
			qw.setOnLongClickListener(this);
			qw.setId(VIEW_ID + id++);

			widgets.add(qw);
			mView.addView(qw, mLayout);

		}

		addView(mView);

	}

	public void addChild(View anyView) {
		mView.addView(anyView, mLayout);
		this.invalidate();
	}

	public void removeChild(View anyView) {
		mView.removeView(anyView);
		this.invalidate();
	}

	/**
	 * @return a HashMap of answers entered by the user for this set of widgets
	 */
	public LinkedHashMap<FormIndex, IAnswerData> getAnswers() {
		final LinkedHashMap<FormIndex, IAnswerData> answers = new LinkedHashMap<FormIndex, IAnswerData>();
		final Iterator<QuestionWidget> i = widgets.iterator();
		while (i.hasNext()) {
			/*
			 * The FormEntryPrompt has the FormIndex, which is where the answer
			 * gets stored. The QuestionWidget has the answer the user has
			 * entered.
			 */
			final QuestionWidget q = i.next();
			final FormEntryPrompt p = q.getPrompt();
			answers.put(p.getIndex(), q.getAnswer());
		}

		return answers;
	}

	/**
	 * // * Add a TextView containing the hierarchy of groups to which the
	 * question belongs. //
	 */
	private void addGroupText(final FormEntryCaption[] groups) {
		final StringBuffer s = new StringBuffer("");
		String t = "";
		int i;
		// list all groups in one string
		for (final FormEntryCaption g : groups) {
			i = g.getMultiplicity() + 1;
			t = g.getLongText();
			if (t != null) {
				s.append(t);
				if (g.repeats() && i > 0) {
					s.append(" (" + i + ")");
				}
				s.append(" > ");
			}
		}

		// build view
		if (s.length() > 0) {
			final TextView tv = new TextView(getContext());
			tv.setText(s.substring(0, s.length() - 3));
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXTSIZE - 4);
			tv.setPadding(0, 0, 0, 5);
			mView.addView(tv, mLayout);
		}
	}

	public void setFocus(final Context context) {
		if (widgets.size() > 0) {
			widgets.get(0).setFocus(context);
		}
	}

	/**
	 * Called when another activity returns information to answer this question.
	 *
	 * @param answer
	 */
	public void setStringData(final String answer) {
		Log.w("Got Mintel View", "Inside the view: "+answer);
		boolean set = false;
		for (final QuestionWidget q : widgets) {
			if (q instanceof StringWidget) {
				if (answer != null) {
					((StringWidget) q).setAnswer(answer);
					set = true;
					break;
				}
			}
		}

		if (!set) {
			Log.w(t,
					"Attempting to return data to a widget or set of widgets no looking for data");
		}
	}

	/**
	 * Called when another activity returns information to answer this question.
	 *
	 * @param answer
	 */
	public void setBinaryData(final Object answer) {
	
		boolean set = false;
		for (final QuestionWidget q : widgets) {
			if (q instanceof IBinaryWidget) {
				if (((IBinaryWidget) q).isWaitingForBinaryData()) {
					((IBinaryWidget) q).setBinaryData(answer);
					set = true;
					break;
				}
			}
		}

		if (!set) {
			Log.w(t,
					"Attempting to return data to a widget or set of widgets no looking for data");
		}
	}

	/**
	 * @return true if the answer was cleared, false otherwise.
	 */
	public boolean clearAnswer() {
		// If there's only one widget, clear the answer.
		// If there are more, then force a long-press to clear the answer.
		if (widgets.size() == 1 && !widgets.get(0).getPrompt().isReadOnly()) {
			widgets.get(0).clearAnswer();
			return true;
		} else
			return false;
	}

	public ArrayList<QuestionWidget> getWidgets() {
		return widgets;
	}

	@Override
	public void setOnFocusChangeListener(final OnFocusChangeListener l) {
		for (int i = 0; i < widgets.size(); i++) {
			final QuestionWidget qw = widgets.get(i);
			qw.setOnFocusChangeListener(l);
		}
	}

	@Override
	public boolean onLongClick(final View v) {
		return false;
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		for (final QuestionWidget qw : widgets) {
			qw.cancelLongPress();
		}
	}

}
