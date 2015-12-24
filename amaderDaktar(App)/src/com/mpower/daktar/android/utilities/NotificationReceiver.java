package com.mpower.daktar.android.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mpower.daktar.android.activities.NotificationActivity;

public class NotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.w("BROADCAST", "Received");
		final String title = intent.getStringExtra("title");
		final String message = intent.getStringExtra("message");
		final String confirm = intent.getStringExtra("confirm");
		final int type = intent.getIntExtra("intent", -1);
		final Intent i = new Intent(context, NotificationActivity.class);
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
