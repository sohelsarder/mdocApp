package com.mpower.mintel.android.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.models.Prescription;

public class PrescriptionListAdapter extends ArrayAdapter<String>{

	public ArrayList<Prescription> list;
	
	private class Holder{
		public TextView date;
		public TextView name;
		public TextView status;
		public String id;
	}
	
	public Prescription getElement(int position){
		return list.get(position);
	}

	
	public PrescriptionListAdapter(Context context, ArrayList<Prescription> list){
		super(context, android.R.layout.simple_list_item_1, new String[list.size()]);
		this.list = list;
	}
	
	@Override
	public String getItem(int position) {
		return list.get(position).url;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (convertView == null){
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.list_item_prescription, parent, false);
			Holder h = new Holder();
			h.date = (TextView) v.findViewById(R.id.date);
			h.name = (TextView) v.findViewById(R.id.pres_name);
			h.status = (TextView) v.findViewById(R.id.status);
			v.setTag(h);
		}
		Holder h = (Holder) v.getTag();
		Prescription p = list.get(position);
		h.date.setText(p.date);
		h.name.setText(p.filename);
		if (p.status.equals("New")){
			h.status.setBackgroundColor(getContext().getResources().getColor(android.R.color.holo_blue_bright));
		}else{
			h.status.setBackgroundColor(getContext().getResources().getColor(android.R.color.holo_red_light));
		}
		h.status.setText(p.status);
		h.id = p.id;
		v.setTag(h);
		return v;
	}
	
}
