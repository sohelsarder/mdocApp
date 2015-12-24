package com.mpower.daktar.android.services;

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
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.UrlUtils;
import com.mpower.daktar.android.R;

public class MqttPushService extends Service implements MqttCallback {

	private MqttAsyncClient client;
	private boolean initialized = false;

	private final String MQTT_PROTO = "tcp://";
	private String MQTT_HOST;
	private final String mqtt_topic = "amdoc/not/";
	private String mqtt_topic_id;

	@Override
	public IBinder onBind(final Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		final File f = new File(MIntel.METADATA_PATH + "/session.tmp");
		Scanner sc;
		try {
			sc = new Scanner(f);
			mqtt_topic_id = sc.next();
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		initialize();
		connect();

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void connectionLost(final Throwable arg0) {
		initialize();
		connect();
	}

	@Override
	public void deliveryComplete(final IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageArrived(final String arg0, final MqttMessage arg1)
			throws Exception {
		// TODO Auto-generated method stub

	}

	private void initialize() {
		if (initialized)
			return;

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		final String mosquitto = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));
		final String port = settings.getString(
				PreferencesActivity.KEY_NOTIFICATION_PORT, MIntel.getInstance()
						.getString(R.string.default_notif_port));
		MQTT_HOST = mosquitto.replace("http://", MQTT_PROTO);
		MQTT_HOST = MQTT_HOST.replace(UrlUtils.getPortNumber(MQTT_HOST), port);

		File f = null;
		try {
			f = new File(MIntel.METADATA_PATH + "/mqttPersist.db");
			f.createNewFile();
		} catch (final IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (f != null) {
			final MqttClientPersistence persistence = new MqttDefaultFilePersistence(
					f.getAbsolutePath());
			try {
				final String clientId = MIntel.getDeviceId();
				persistence.open(clientId, MQTT_HOST);
				client = new MqttAsyncClient(MQTT_HOST, clientId, persistence);
				client.setCallback(this);
			} catch (final MqttPersistenceException pe) {
				Log.e("MQTT_PERSIST_EXCEPTION", pe.getMessage());
			} catch (final MqttException e) {
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
		} catch (final MqttException e) {
			Log.e("MQTT EXCEPTION CONNECTING", e.getMessage());
		}
	}

	private void ping() {
		final ScheduledExecutorService scheduler = Executors
				.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				IMqttDeliveryToken delivering;
				try {
					delivering = client.publish(mqtt_topic + mqtt_topic_id,
							"a".getBytes(), 2, true);
					delivering.waitForCompletion();
				} catch (final MqttPersistenceException e) {
					// TODO Auto-generated catch block
					Log.e("MQTT PERSIST EXCEPTION", e.getMessage());
				} catch (final MqttException e) {
					// TODO Auto-generated catch block
					Log.e("MQTT EXCEPTION", e.getMessage());
				}
				Log.w("MQTT STATUS", "Should have delivered to " + mqtt_topic
						+ mqtt_topic_id);
			}
		}, 0, 30, TimeUnit.MINUTES);

	}

}
