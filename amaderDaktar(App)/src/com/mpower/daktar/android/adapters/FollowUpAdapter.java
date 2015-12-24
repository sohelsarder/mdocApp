package com.mpower.daktar.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mpower.daktar.android.R;

public class FollowUpAdapter extends ArrayAdapter<String> {

	private final String[] rows;
	private final Context context;

	public class ViewHolder {
		public TextView name;
		public TextView phone;
		public TextView date;
	}

	public FollowUpAdapter(final Context context, final String[] objects) {
		super(context, R.id.list_followup, android.R.layout.simple_list_item_1,
				objects);
		rows = objects;
		this.context = context;
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			final LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.list_view_followup, parent, false);
			final ViewHolder vh = new ViewHolder();
			v.setTag(vh);
			vh.name = (TextView) v.findViewById(R.id.followup_name);
			vh.date = (TextView) v.findViewById(R.id.followup_date);
			vh.phone = (TextView) v.findViewById(R.id.followup_phone);
		}
		final String[] vals = parseRows(rows[position]);
		final ViewHolder vh = (ViewHolder) v.getTag();
		vh.name.setText(vals[0]);
		vh.phone.setText(vals[1]);
		vh.date.setText(vals[2]);
		return v;
	}

	private String[] parseRows(final String row) {
		String[] ret;
		ret = row.split(":");
		return ret;
	}

}
