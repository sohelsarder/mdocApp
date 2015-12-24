package com.mpower.daktar.android.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mpower.daktar.android.models.Prescription;
import com.mpower.daktar.android.R;

public class PrescriptionListAdapter extends ArrayAdapter<String> {

	public ArrayList<Prescription> list;

	private class Holder {
		public TextView date;
		public TextView name;
		public TextView status;
	}

	public Prescription getElement(final int position) {
		return list.get(position);
	}

	public PrescriptionListAdapter(final Context context,
			final ArrayList<Prescription> list) {
		super(context, android.R.layout.simple_list_item_1, new String[list
		                                                               .size()]);
		this.list = list;
	}

	@Override
	public String getItem(final int position) {
		return list.get(position).url;
	}

	@Override
	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		View v = convertView;
		if (convertView == null) {
			final LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater
					.inflate(R.layout.list_item_prescription, parent, false);
			final Holder h = new Holder();
			h.date = (TextView) v.findViewById(R.id.date);
			h.name = (TextView) v.findViewById(R.id.pres_name);
			h.status = (TextView) v.findViewById(R.id.status);
			v.setTag(h);
		}
		final Holder h = (Holder) v.getTag();
		final Prescription p = list.get(position);
		h.date.setText(p.date);
		h.name.setText(p.filename);
		if (p.status.equals("New")) {
			h.status.setBackgroundColor(getContext().getResources().getColor(
					android.R.color.holo_blue_bright));
		} else {
			h.status.setBackgroundColor(getContext().getResources().getColor(
					android.R.color.holo_red_light));
		}
		h.status.setText(p.status);
		v.setTag(h);
		return v;
	}

}
