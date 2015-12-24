package com.mpower.daktar.android.fingerprint;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.support.v4.app.ActivityCompat;

import com.mpower.daktar.android.R;

public class BluetoothActivity extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;
	
	private static final String NAME = "nam";
	private static final String MODE = "mod";
	private static final String MODE_IDENTIFY = "CHK";
	private static final String MODE_ENROLL = "ENR";
	
	private String opMode;
	private String name;
	
//Enroll UID
	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_TEMPLATE = 6;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Layout Views + Buttons
	private ListView mConversationView;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothThreading mChatService = null;

	// Terrible global variables
	public static String Template = null;
	public TextView tempText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		Intent i = getIntent();
		opMode = i.getStringExtra(MODE);
		//name = i.getStringExtra(NAME);
		
		/*
		 * Test
		 * */
		name = "Patient";
		/*
		 * End  Test
		 * */
		
		// Set up the window layout
		setContentView(R.layout.activity_bluetooth);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
				// Otherwise, setup the chat session
			} else {
				if (mChatService == null)
					setupChat();
			}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothThreading.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);

		// SimPrints Buttons (temp)
		// Initialize the send button with a listener that for click events

		tempText = (TextView) findViewById(R.id.tempTextView);
		tempText.setBackgroundColor(Color.RED);

		Button lightsOn = (Button) findViewById(R.id.lights_on);
		lightsOn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String message = "{SPC_FLASH_LIGHTS:ON}\n";
				sendMessage(message);
			}
		});

		Button lightsOff = (Button) findViewById(R.id.lights_off);
		lightsOff.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String message = "{SPC_FLASH_LIGHTS:OFF}\n";
				sendMessage(message);
			}
		});

		Button getTemplate = (Button) findViewById(R.id.get_template);
		getTemplate.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String message = "{SPC_TEMPLATE:}\n";
				sendMessage(message);
			}
		});

		Button delTemplate = (Button) findViewById(R.id.deleteTmpBtn);
		delTemplate.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Template = null;
				tempText.setBackgroundColor(Color.RED);
				mConversationArrayAdapter.add("Template Deleted");
			}
		});
		
		Button enroll = (Button) findViewById(R.id.EnrollBtn);
		enroll.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ServerConnect sc = new ServerConnect();
				String tmp = sc.Enroll(Template, 0, name);
				Intent i = new Intent();
				i.putExtra("UID", tmp);
				setResult(RESULT_OK, i);
				finish();
				//mConversationArrayAdapter.add("Identify UID: " + tmp);
			}
		});

		Button identify = (Button) findViewById(R.id.IdentifyBtn);
		identify.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ServerConnect sc = new ServerConnect();
				String tmp = sc.Identify(Template, name);
				Intent i = new Intent();
				i.putExtra("UID", tmp);
				setResult(RESULT_OK, i);
				finish();
				//mConversationArrayAdapter.add("Identify UID: " + tmp);
			}
		});


		if (opMode.equals(MODE_ENROLL)){
			identify.setVisibility(View.GONE);
		}else if (opMode.equals(MODE_IDENTIFY)){
			enroll.setVisibility(View.GONE);
		}
		
		
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothThreading(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 *
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothThreading.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			byte[] send = message.getBytes();
			mChatService.write(send);
		}
	}

	/**
	 * Debugging tool
	 */
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	@SuppressLint("NewApi")
	private void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
			actionBar.setSubtitle(resId);
	}

	@SuppressLint("NewApi")
	private void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
			actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothChatService
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothThreading.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					mConversationArrayAdapter.clear();
					break;
				case BluetoothThreading.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothThreading.STATE_LISTEN:
				case BluetoothThreading.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(msg.obj.toString());
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				break;
			case MESSAGE_TEMPLATE:
				// construct a string from the valid bytes in the buffer
				String readTemplate = new String(msg.obj.toString());
				// assign to template a show user
				Template = readTemplate.trim();
				mConversationArrayAdapter.add("Template Acquired");
				tempText.setBackgroundColor(Color.GREEN);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode + data);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK && data != null) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK && data != null) {
				connectDevice(data, false);
			} else {
				Log.d(TAG, "Activity result break");
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		if (device != null) {
			mChatService.connect(device, secure);
		} else {
			Log.d(TAG, "device = null");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bluetooth, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.insecure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent,
					REQUEST_CONNECT_DEVICE_INSECURE);
			ensureDiscoverable();
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}
	
}
