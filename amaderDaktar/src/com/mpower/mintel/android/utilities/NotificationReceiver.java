package com.mpower.mintel.android.utilities;

import com.mpower.mintel.android.activities.NotificationActivity;
import com.mpower.mintel.android.application.MIntel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.w("BROADCAST", "Received");
		String title = intent.getStringExtra("title");
		String message = intent.getStringExtra("message");
		String confirm = intent.getStringExtra("confirm");
		int type = intent.getIntExtra("intent", -1);
		Intent i = new Intent(context, NotificationActivity.class);
		i.putExtra("title", title);
		i.putExtra("message", message);
		i.putExtra("confirm", confirm);
		i.putExtra("intent", type);
		i.putExtra("cancel", "Cancel");
		i.putExtra("args", intent.getStringArrayExtra("args"));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}

}
