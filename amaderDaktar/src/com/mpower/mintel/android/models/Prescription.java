package com.mpower.mintel.android.models;

public class Prescription {

	public String id;
	public String date;
	public String status;
	public String url;
	public String filename;
	
	public Prescription(String id, String date, String status, String url, String filename){
		this.id = id;
		this.date = date;
		this.status = status;
		this.url = url;
		this.filename = filename;
	}
	
	
}
