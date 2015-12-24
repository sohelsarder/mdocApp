package com.mpower.daktar.android.adapters;

import java.util.Arrays;

import com.mpower.daktar.android.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class DoctorListAdapter extends ArrayAdapter<String> {

	private String[] data;
	private Context context;

	public String[] id;
	public String[] name;
	private String[] specialization;
	private String[] status;
	private String[] schedule;

	public static final int TYPE_VIEW = 2;
	public static final int TYPE_CLICK = 1;

	private int type;

	// "id$name$specialization$status$schedule"

	public class ViewHolder {
		public TextView docName;
		public TextView docTitle;
		public TextView docSchedule;
		public ImageView docStatus;
	}

	public DoctorListAdapter(Context context, String[] objects, int type) {
		super(context, android.R.layout.simple_list_item_1, objects);
		this.data = objects;
		this.context = context;
		this.type = type;

		id = new String[data.length+1];
		name = new String[data.length+1];
		specialization = new String[data.length];
		status = new String[data.length];
		schedule = new String[data.length];

		for (int i = 0; i < data.length; i++) {
			String[] vals = data[i].split("\\$");
			Log.w("Doctor Data", Arrays.toString(vals));
			if (vals.length == 5) {
				id[i] = vals[0];
				name[i] = vals[1];
				specialization[i] = vals[2];
				status[i] = vals[3];
				schedule[i] = vals[4];
			}
		}
		name[name.length-1] = "যেকোনো ডাক্তার";
		id[id.length-1] = "0";

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(context.LAYOUT_INFLATER_SERVICE);
			if (type == TYPE_VIEW) {
				view = inflater.inflate(R.layout.doctor_list_view, parent,
						false);
				ViewHolder vh = new ViewHolder();
				vh.docName = (TextView) view.findViewById(R.id.doc);
				vh.docTitle = (TextView) view.findViewById(R.id.doc_qual);
				vh.docSchedule = (TextView) view
						.findViewById(R.id.doc_schedule);
				vh.docStatus = (ImageView) view.findViewById(R.id.doc_status);
				view.setTag(vh);
			} else if (type == TYPE_CLICK) {
				view = inflater.inflate(R.layout.doctor_list_view_1, parent,
						false);
				ViewHolder vh = new ViewHolder();
				vh.docName = (TextView) view.findViewById(R.id.doc);
				vh.docTitle = (TextView) view.findViewById(R.id.doc_qual);
				vh.docSchedule = (TextView) view
						.findViewById(R.id.doc_schedule);
				view.setTag(vh);
			}
		}
		ViewHolder v = (ViewHolder) view.getTag();
		v.docName.setText(name[position]);
		v.docTitle.setText(specialization[position]);
		v.docSchedule.setText(schedule[position]);
		if (type == TYPE_VIEW){
			if (Integer.parseInt(status[position]) >= 2){
				v.docStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.online));
			}else{
				v.docStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.offline));
			}
		}
		view.setTag(v);
		return view;
	}

}
