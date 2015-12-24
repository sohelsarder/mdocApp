package com.mpower.mintel.android.listeners;

import org.json.JSONArray;

public interface PendingCheckListener {

	public void pendingListRetrieved(JSONArray urls, JSONArray names);
	
}
