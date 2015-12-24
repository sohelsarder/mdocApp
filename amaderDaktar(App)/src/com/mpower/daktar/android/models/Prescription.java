package com.mpower.daktar.android.models;

public class Prescription {

	public String id;
	public String date;
	public String status;
	public String url;
	public String filename;

	public Prescription(final String id, final String date,
			final String status, final String url, final String filename) {
		this.id = id;
		this.date = date;
		this.status = status;
		this.url = url;
		this.filename = filename;
	}

}
