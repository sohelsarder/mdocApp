package com.mpower.mintel.android.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.activities.DownloadActivity;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.preferences.PreferencesActivity;
import com.mpower.mintel.android.utilities.NotificationReceiver;
import com.mpower.mintel.android.utilities.UrlUtils;
import com.mpower.mintel.android.utilities.WebUtils;

public class PushService extends Service implements MqttCallback {

	private static String MQTT_HOST;
	private static final String MQTT_PROTO = "tcp://";

	
	public static final int NOTIFICATION_PRESCRIPTION = 76;
	public static final int NOTIFICATION_CALL = 68;

	private static final String TOPIC = "amdoc/not";

	private static String userId;

	private static int wifiState = -9999;

	MqttAsyncClient client;
	MqttClientPersistence persistence;
	final static AlarmManager alarm = (AlarmManager) MIntel.getAppContext()
			.getSystemService(Context.ALARM_SERVICE);

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String mosquitto = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));
		String port = settings.getString(
				PreferencesActivity.KEY_NOTIFICATION_PORT, MIntel.getInstance()
						.getString(R.string.default_notif_port));
		MQTT_HOST = mosquitto.replace("http://", MQTT_PROTO);
		MQTT_HOST = MQTT_HOST.replace(UrlUtils.getPortNumber(MQTT_HOST), port);
		Log.e("Final Host Name", MQTT_HOST);

		WifiManager wifiManager = (WifiManager) MIntel.getAppContext()
				.getSystemService(Context.WIFI_SERVICE);
		wifiState = wifiManager.getWifiState();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart();
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void onStart() {

		File ff = new File(MIntel.METADATA_PATH + "/session.tmp");
		Scanner sc;
		try {
			sc = new Scanner(ff);
			userId = sc.next();
			sc.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Log.w("Process Flow", "Started Service");
		String serverURI = MQTT_HOST;
		String clientId = MIntel.getDeviceId();
		Log.w("URI Info", serverURI + "->" + clientId);
		File f = new File(MIntel.METADATA_PATH);

		persistence = new MqttDefaultFilePersistence();
		
		try {
			persistence = new MqttDefaultFilePersistence(
					f.getAbsolutePath());
			persistence.close();
		} catch (MqttPersistenceException e1) {
			// Silent Close since it means the persistence is already closed
		}
		try {
			persistence.open(clientId, serverURI);
			client = new MqttAsyncClient(serverURI, clientId, persistence);
			client.setCallback(this);
			if (!client.isConnected()) {
				IMqttToken connecting = client.connect();
				connecting.waitForCompletion();
				Log.w("MQTT STATUS", "Connection Done");
				connecting = client.subscribe(TOPIC + "/" + userId, 2);
				connecting.waitForCompletion();
				Log.w("MQTT STATUS", "Should have subscribed to " + TOPIC + "/"
						+ userId);
				
			} else {
			}
		} catch (MqttPersistenceException mpe) {
			Log.e("MQTT PERSISTENCE EXCEPTION", mpe.getMessage());
			mpe.printStackTrace();
			resetPersistence();
		} catch (MqttException e) {
			Log.e("MQTT EXCEPTION", e.getMessage());
			e.printStackTrace();
		} 
		if (client.isConnected())
			try {
				keepConnecting(persistence);
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public void keepConnecting(final MqttClientPersistence persistence)
			throws MqttException {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					IMqttDeliveryToken delivering;
					try {
						delivering = client.publish(TOPIC + "/" + userId,
								"a".getBytes(), 2, true);

						delivering.waitForCompletion();
					} catch (MqttException e) {
						// TODO Auto-generated catch block
						Log.e("MQTT EXCEPTION", e.getMessage());
					}
					Log.w("MQTT STATUS", "Should have delivered to " + TOPIC
							+ "/" + userId);

					try {
						Thread.sleep(AlarmManager.INTERVAL_HALF_HOUR);
						Thread.yield();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
	}

	public void onStop() {
		if (client != null) {
			try {
				client.unsubscribe(TOPIC + "/" + userId);
				client.disconnect();
				client.close();
			} catch (MqttException e) {
				// Silent Crash
			}
		}
	}

	@Override
	public void connectionLost(Throwable arg0) {
		Log.w("MQTT STATUS", "Connection Lost");
		int i = 0;
		while (!client.isConnected() && i < 5){
			reconnect();
			i++;
			Log.w("MQTT RECONNECT", "Inside Loop");
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) {
		Log.w("Pushed Message", arg1.toString());
		if (arg1.toString().equals("a"))
			return;
		try {
			JSONObject msg = new JSONObject(arg1.toString());
			int type = msg.getInt("type");
			String formattedMsg = "";
			Intent intent = null;
			PendingIntent pi = null;
			if (type == 0) {
				intent = new Intent(getApplicationContext(),
						DownloadActivity.class);
				intent.putExtra("url", msg.getString("presUrl"));
				intent.putExtra("name", msg.getString("pdfName"));
				pi = PendingIntent.getActivity(getApplicationContext(),
						NOTIFICATION_PRESCRIPTION, intent, START_NOT_STICKY);
				formattedMsg = msg.getString("patName") + " এর প্রেস্ক্রিপশান তৈরী আছে" ;
				NotificationManager manager = (NotificationManager) MIntel
						.getAppContext().getSystemService(
								Context.NOTIFICATION_SERVICE);
				Notification.Builder builder = new Notification.Builder(
						MIntel.getAppContext());
				builder.setContentTitle("প্রেস্ক্রিপশান তৈরী আছে" );
				builder.setContentText(formattedMsg);
				builder.setSmallIcon(R.drawable.ic_notification_call);
				builder.setContentIntent(pi);
				builder.setAutoCancel(true);
				Notification notification = builder.getNotification();
				notification.sound = Uri.parse(MIntel.METADATA_PATH
						+ "/pres_n.ogg");
				// notification.defaults |= Notification.DEFAULT_SOUND;
				manager.notify(NOTIFICATION_PRESCRIPTION, notification);
				Intent i = new Intent(MIntel.getAppContext(),
						NotificationReceiver.class);
				i.putExtra("title", "প্রেস্ক্রিপশান তৈরী আছে");
				i.putExtra("message", formattedMsg);
				i.putExtra("confirm", "প্রেস্ক্রিপশান Download করুন" );
				i.putExtra("intent", type);
				i.putExtra(
						"args",
						new String[] { msg.getString("presUrl"),
								msg.getString("pdfName") });
				getApplicationContext().sendBroadcast(i);
			} else {
				try {
					intent = new Intent("android.intent.action.MAIN");
					intent.setComponent(ComponentName
							.unflattenFromString("com.android.chrome/com.android.chrome.Main"));
					intent.addCategory("android.intent.category.LAUNCHER");
					String url = WebUtils.RMP_CALL_URL + msg.getString("rmpId") + "/"
							+ msg.getString("docId") + "/"
							+ msg.getString("frame");
					intent.setData(Uri.parse(url));
					pi = PendingIntent.getActivity(MIntel.getAppContext(),
							NOTIFICATION_CALL, intent, START_NOT_STICKY);
				} catch (ActivityNotFoundException e) {
					// Chrome is probably not installed
				}
				formattedMsg = msg.getString("docName")
						+ " কথা বলতে চান  "
						+ msg.getString("patName")+" এর সাথে। ";
				NotificationManager manager = (NotificationManager) MIntel
						.getAppContext().getSystemService(
								Context.NOTIFICATION_SERVICE);
				Notification.Builder builder = new Notification.Builder(
						MIntel.getAppContext());
				builder.setContentTitle("ডাক্তার কল করছেন। ");
				builder.setContentText(formattedMsg);
				builder.setSmallIcon(R.drawable.ic_notification_pres);
				builder.setContentIntent(pi);
				builder.setAutoCancel(true);
				Notification notification = builder.getNotification();
				notification.sound = Uri.parse(MIntel.METADATA_PATH
						+ "/call_n.ogg");

				manager.notify(NOTIFICATION_CALL, notification);
				new NotificationReceiver();
				Intent i = new Intent(MIntel.getAppContext(),
						NotificationReceiver.class);
				i.putExtra("title", "ডাক্তার কল করছেন। ");
				i.putExtra("message", formattedMsg);
				i.putExtra("confirm", "উত্তর দিন");
				i.putExtra("intent", type);
				i.putExtra(
						"args",
						new String[] { msg.getString("rmpId"),
								msg.getString("docId"), msg.getString("frame") });
				i.setAction("com.mpower.mintel.android.utilities");
				getApplicationContext().sendBroadcast(i);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Crash", "Shit Happened");
		}

	}
	
	void reconnect(){
		Log.w("MQTT RECONNECT", "Started");
		try{
			if (!client.isConnected()) {
				Thread.sleep(1000 * 5);
				IMqttToken connecting = client.connect();
				connecting.waitForCompletion();
				Log.w("MQTT STATUS", "Connection Done");
				connecting = client.subscribe(TOPIC + "/" + userId, 2);
				connecting.waitForCompletion();
				Log.w("MQTT STATUS", "Should have subscribed to " + TOPIC
						+ "/" + userId);
			}
			}catch (MqttException me){
				me.printStackTrace();
			}catch(InterruptedException ie){}
			
	}
	
	private void resetPersistence(){
		if (persistence != null){
			try {
				persistence.close();
			} catch (MqttPersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String serverURI = MQTT_HOST;
			String clientId = MIntel.getDeviceId();
			try {
				persistence.open(clientId, serverURI);
			} catch (MqttPersistenceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onDestroy() {
		try {
			if (persistence != null)
			persistence.close();
			Log.w("MQTT DESTROY", "Successfully Destroyed Persistence");
		} catch (MqttPersistenceException e) {
			Log.e("MQTT DESTROY", "Failed Error!");
		}
		super.onDestroy();
	}
	
	public class LocalBinder extends Binder {
        PushService getService() {
            return PushService.this;
        }
    }
}