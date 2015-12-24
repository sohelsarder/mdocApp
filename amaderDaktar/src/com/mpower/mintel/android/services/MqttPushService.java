package com.mpower.mintel.android.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.mpower.mintel.android.R;
import com.mpower.mintel.android.application.MIntel;
import com.mpower.mintel.android.preferences.PreferencesActivity;
import com.mpower.mintel.android.utilities.UrlUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class MqttPushService extends Service implements MqttCallback {

	private MqttAsyncClient client;
	private boolean initialized = false;

	private String MQTT_PROTO = "tcp://";
	private String MQTT_HOST;
	private String mqtt_topic = "amdoc/not/";
	private String mqtt_topic_id;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		File f = new File(MIntel.METADATA_PATH + "/session.tmp");
		Scanner sc;
		try {
			sc = new Scanner(f);
			mqtt_topic_id = sc.next();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		initialize();
		connect();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void connectionLost(Throwable arg0) {
		initialize();
		connect();
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		// TODO Auto-generated method stub

	}

	private void initialize() {
		if (initialized)
			return;

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

		File f = null;
		try {
			f = new File(MIntel.METADATA_PATH+"/mqttPersist.db");
			f.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (f != null) {
			MqttClientPersistence persistence = new MqttDefaultFilePersistence(
					f.getAbsolutePath());
			try {
				String clientId = MIntel.getDeviceId();
				persistence.open(clientId, MQTT_HOST);
				client = new MqttAsyncClient(MQTT_HOST, clientId, persistence);
				client.setCallback(this);
			} catch (MqttPersistenceException pe){
				Log.e("MQTT_PERSIST_EXCEPTION", pe.getMessage());
			}catch (MqttException e) {
				Log.e("MQTT EXCEPTION", e.getMessage());
			}
		}
		initialized = true;

	}

	private void connect() {
		try {
			if (!client.isConnected()) {
				IMqttToken connecting = client.connect();
				connecting.waitForCompletion();
				Log.w("MQTT STATUS", "Connection Done");
				connecting = client.subscribe(mqtt_topic + mqtt_topic_id, 2);
				connecting.waitForCompletion();
				Log.w("MQTT STATUS", "Should have subscribed to " + mqtt_topic
						+ mqtt_topic_id);
			}
			ping();
		} catch (MqttException e) {
			Log.e("MQTT EXCEPTION CONNECTING", e.getMessage());
		}
	}

	private void ping() {
		ScheduledExecutorService scheduler = Executors
				.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				IMqttDeliveryToken delivering;
				try {
					delivering = client.publish(mqtt_topic + mqtt_topic_id,
							"a".getBytes(), 2, true);
					delivering.waitForCompletion();
				} catch (MqttPersistenceException e) {
					// TODO Auto-generated catch block
					Log.e("MQTT PERSIST EXCEPTION", e.getMessage());
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					Log.e("MQTT EXCEPTION", e.getMessage());
				}
				Log.w("MQTT STATUS", "Should have delivered to " + mqtt_topic
						+ mqtt_topic_id);
			}
		}, 0, 30, TimeUnit.MINUTES);

	}

}
