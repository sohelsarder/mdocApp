package com.mpower.daktar.android.activities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.models.UserData;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.User;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

/**
 * LoginActivity - Login Activity
 *
 * @author Mehdi Hasan <mhasan@mpower-health.com> Sadat Sakif Ahmed
 *         <sadat@mpower-social.com>
 *
 */
public class LoginActivity extends Activity {

	/**
	 * Simple Dialog used to show the splash screen
	 */
	protected Dialog mSplashDialog;

	private ImageButton loginButton;

	private String username = "", password = "";

	private User user;

	private String qr_username = "";

	private String qr_password = "";

	// Stores the RMP Information from the server on login attempt
	private String rmp_name;
	private String rmp_id;
	private String rmp_role;
	private String rmp_phone;
	private String rmp_age;
	private String rmp_acc;

	private static final int SCAN_REQUEST_CODE_RMP = 86;

	// menu options
	private static final int MENU_PREFERENCES = Menu.FIRST;

	private final BroadcastReceiver authenticationDoneReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			updateData(null);
		}
	};

	private final BroadcastReceiver authenticationNeededReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String logoutMessage = intent.getStringExtra("message");
			updateData(logoutMessage);
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(authenticationDoneReceiver);
		unregisterReceiver(authenticationNeededReceiver);
	}

	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// FIX: Initial back button from homescreen causes problem logging in
		User.getInstance().logOff("");

		// Change the layout base custom to the chikitsha software
		setContentView(R.layout.login_rmp_screen);

		final IntentFilter authenticationDoneFilter = new IntentFilter(
				User.BROADCAST_ACTION_AUTHENTICATION_DONE);
		authenticationDoneFilter.addCategory(Intent.CATEGORY_DEFAULT);
		registerReceiver(authenticationDoneReceiver, authenticationDoneFilter);

		final IntentFilter authenticationNeededFilter = new IntentFilter(
				User.BROADCAST_ACTION_AUTHENTICATION_NEEDED);
		authenticationNeededFilter.addCategory(Intent.CATEGORY_DEFAULT);
		registerReceiver(authenticationNeededReceiver,
				authenticationNeededFilter);

		user = User.getInstance();

		// usernameEditText = (EditText) findViewById(R.id.login_username);
		// passwordEditText = (EditText) findViewById(R.id.login_password);
		// usernameEditText.setFilters(new InputFilter[] { getReturnFilter(),
		// getWhitespaceFilter() });
		// passwordEditText.setFilters(new InputFilter[] { getReturnFilter(),
		// getWhitespaceFilter() });

		loginButton = (ImageButton) findViewById(R.id.login_button);

		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				boolean isConnected = checkDataConnectivity();
				// ##### DEBUG
				//isConnected = true;
				if (!isConnected) {
					try {
						enableDataConnectivity(true);
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
				if (isConnected) {
					final Intent intent = new Intent(
							"com.google.zxing.client.android.SCAN");
					intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					intent.putExtra("PROMPT_MESSAGE",
							"আপনার কোড স্ক্যান করুন। কোডটি সম্পূর্ণভাবে বাক্সের ভিতরে রাখুন।");
					startActivityForResult(intent, SCAN_REQUEST_CODE_RMP);
					// checkLogin();
				} else {
					/*
					 * Intent intent = new Intent(
					 * "com.google.zxing.client.android.SCAN");
					 * intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					 * intent.putExtra("PROMPT_MESSAGE",
					 * "আপনার কোড স্ক্যান করুন।"); PendingIntent pi =
					 * PendingIntent.getActivity(getApplicationContext(),
					 * SCAN_REQUEST_CODE_RMP, intent, 0); AlarmManager alarm =
					 * (AlarmManager) getSystemService(Context.ALARM_SERVICE);
					 * alarm.set(AlarmManager.ELAPSED_REALTIME,
					 * SystemClock.elapsedRealtime()+(1000 * 10), pi);
					 */
					final Handler delayedLogin = new Handler();
					final Runnable delayedRun = new Runnable() {

						@Override
						public void run() {
							final Intent intent = new Intent(
									"com.google.zxing.client.android.SCAN");
							intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
							intent.putExtra("PROMPT_MESSAGE",
									"আপনার কোড স্ক্যান করুন।");
							LoginActivity.this.startActivityForResult(intent,
									SCAN_REQUEST_CODE_RMP);
						}
					};
					delayedLogin.postDelayed(delayedRun, 10 * 1000);
					createCustomToast(-1,
							"কিছুক্ষণ অপেক্ষা করুন। Data Connection চালু করা হচ্ছে। ");
					createCustomToast(-1, "৫ ");
					createCustomToast(-1, "৪ ");
					createCustomToast(-1, "৩ ");
					createCustomToast(-1, "২ ");
					createCustomToast(-1, "১ ");

				}
			}
		});

	}

	private void enableDataConnectivity(final boolean enabled) throws Exception {
		final ConnectivityManager conman = (ConnectivityManager) LoginActivity.this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final Class conmanClass = Class.forName(conman.getClass().getName());
		final Field iConnectivityManagerField = conmanClass
				.getDeclaredField("mService");
		iConnectivityManagerField.setAccessible(true);
		final Object iConnectivityManager = iConnectivityManagerField
				.get(conman);
		final Class iConnectivityManagerClass = Class
				.forName(iConnectivityManager.getClass().getName());
		final Method setMobileDataEnabledMethod = iConnectivityManagerClass
				.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
		setMobileDataEnabledMethod.setAccessible(true);

		setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
		final WifiManager wifiManager = (WifiManager) LoginActivity.this
				.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled() == true) {
			wifiManager.setWifiEnabled(false);
		}
	}

	private boolean checkDataConnectivity() {
		final WifiManager wifiManager = (WifiManager) LoginActivity.this
				.getSystemService(Context.WIFI_SERVICE);
		final boolean wifiStatus = wifiManager.isWifiEnabled();
		boolean mobileDataEnabled = false; // Assume disabled
		final ConnectivityManager cm = (ConnectivityManager) LoginActivity.this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			final Class cmClass = Class.forName(cm.getClass().getName());
			final Method method = cmClass
					.getDeclaredMethod("getMobileDataEnabled");
			method.setAccessible(true); // Make the method callable
			// get the setting for "mobile data"
			mobileDataEnabled = (Boolean) method.invoke(cm);
		} catch (final Exception e) {
			// Some problem accessible private API
			// TODO do whatever error handling you want here
		} finally {
			return mobileDataEnabled && !wifiStatus;
		}
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SCAN_REQUEST_CODE_RMP) {
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");
				contents = contents.replace("http://", "");
				final String[] elements = contents.split(":");
				// ##### DEBUG
				// TelephonyManager telephonyManager = (TelephonyManager)
				// getSystemService(Context.TELEPHONY_SERVICE);
				// String deviceId = telephonyManager.getDeviceId();

				if (elements.length == 2) {
					qr_username = elements[1];
					qr_password = elements[0];
					checkLogin();
				} else {

					createCustomToast(R.drawable.ic_exclamation,
							"সঠিক QR Code দেখান। ");
					return;
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_PREFERENCES, 0,
				getString(R.string.general_preferences)).setIcon(
				android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			final Intent ig = new Intent(LoginActivity.this,
					PreferencesActivity.class);
			startActivity(ig);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateData(final String logoutMessage) {

		if (user.isLoggedin()) {
			processValidLogin();
		} else {
			processInvalidLogin(logoutMessage);
		}
	}

	private void checkLogin() {
		// Change the input mechanism
		// username = usernameEditText.getText().toString().trim();
		// password =
		// WebUtils.getSHA512(passwordEditText.getText().toString().trim());

		username = qr_username;
		System.out.println(password);
		password = WebUtils.getSHA512(qr_password);
		System.out.println(username);
		System.out.println(password);
		
		if (!(username.length() > 0)) {
			Toast.makeText(LoginActivity.this, "Please enter User ID",
					Toast.LENGTH_LONG).show();
			return;
		}

		if (!(password.length() > 0)) {
			Toast.makeText(LoginActivity.this, "Please enter password",
					Toast.LENGTH_LONG).show();
			return;
		}

		try {
			user.checkLogin(username, password, LoginActivity.this);
		} catch (final Exception e) {
			showAlertDialog("আপনার লগইন সঠিক হয় নি।", e.getMessage());
			e.printStackTrace();
		}

	}

	private void processInvalidLogin(String logoutMessage) {
		if (logoutMessage == null || "".equals(logoutMessage)) {
			logoutMessage = User.LOGOUT_MESSAGE_UNKNOWN;
		}
		showAlertDialog("আপনার লগইন সঠিক হয় নি।", "যেই কারনে হয় নি :" + "\n\n"
				+ logoutMessage);
		// passwordEditText.setText("");
	}

	private void processValidLogin() {
		startNextActivity();
	}

	private void startNextActivity() {
		final Intent i = new Intent(LoginActivity.this, RmpHome.class);
		i.putExtra("rmp_name", rmp_name);
		i.putExtra("rmp_id", rmp_id);
		i.putExtra("rmp_role", rmp_role);
		i.putExtra("rmp_age", rmp_age);
		i.putExtra("rmp_phone", rmp_phone);
		i.putExtra("rmp_acc", rmp_acc);
		startActivity(i);
		finish();
	}

	private void showAlertDialog(final String title, final String message) {

		final AlertDialog.Builder adb = new AlertDialog.Builder(
				LoginActivity.this);

		adb.setTitle(title);
		adb.setMessage(message);

		adb.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
					final int id) {
						dialog.dismiss();
					}
				});

		adb.show();
	}

	public void checkLoginOnline() {
		new LoginTask().execute();
	}

	protected void removeSplashScreen() {
		if (mSplashDialog != null) {
			mSplashDialog.dismiss();
			mSplashDialog = null;
		}
	}

	/**
	 * LoginTask - AsyncTask for logging in to server
	 *
	 * @author Mehdi Hasan <mhasan@mpower-health.com>
	 *
	 */
	class LoginTask extends AsyncTask<Void, Void, Void> {

		private String loginUrl;
		private int timeOut;
		private int loginStatus = 0;
		private UserData onlineLd = null;

		private ProgressDialog pbarDialog;

		private void initPrefs() {
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(LoginActivity.this);
			loginUrl = prefs
					.getString(PreferencesActivity.KEY_SERVER_URL, null)
					+ WebUtils.URL_PART_LOGIN;
			timeOut = WebUtils.CONNECTION_TIMEOUT;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			pbarDialog = new ProgressDialog(LoginActivity.this);
			pbarDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pbarDialog.setTitle(getString(R.string.please_wait));
			pbarDialog.setMessage("লগ ইন হচ্ছে...");
			pbarDialog.setCancelable(false);
			pbarDialog.show();
		}

		@Override
		protected Void doInBackground(final Void... params) {
			if ("".equals(username) || "".equals(password))
				return null;

			WebUtils.clearAllCredentials();
			WebUtils.addCredentials(username, password);

			initPrefs();
			login();

			return null;
		}

		private void login() {
			HttpResponse response;
			try {
				response = WebUtils.stringResponseGet(loginUrl, MIntel
						.getInstance().getHttpContext(), WebUtils
						.createHttpClient(timeOut));
				loginStatus = response.getStatusLine().getStatusCode();
				final HttpEntity entity = response.getEntity();

				Log.d("Login Status", "" + loginStatus);
				if (entity != null && loginStatus == 200) {
					final InputStream stream = entity.getContent();
					final BufferedReader br = new BufferedReader(
							new InputStreamReader(stream));
					String line;
					while ((line = br.readLine()) != null) {
						final JSONObject json = new JSONObject(line);
						rmp_name = json.getString("name");
						rmp_id = json.getString("id");
						rmp_role = json.getString("role");
						rmp_phone = json.getString("phone");
						rmp_age = json.getString("age");
						rmp_acc = json.getString("currBalance");
						writeToSessionFile(json);
						Log.d("Login Entity", "" + line);
					}
					onlineLd = new UserData();
				}

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void onPostExecute(final Void result) {

			if (pbarDialog != null && pbarDialog.isShowing()) {
				pbarDialog.dismiss();
			}

			if (loginStatus == 200 && onlineLd != null) {

				onlineLd.setUsername(username);
				onlineLd.setPassword(password);

				User.getInstance().setLoginResult(true, onlineLd, null);
			} else {
				if (loginStatus != 401) {
					// There was an error checking login online, but we are not
					// explicitly denied, let's proceed with offline login if
					// possible

					try {
						final boolean offlineUserDataAvailable = User
								.getInstance().offlineUserDataAvailable();

						if (offlineUserDataAvailable) {
							if (User.getInstance().checkOfflineLogin(username,
									password)) {
								final UserData offlineLd = User.getInstance()
										.extractOfflineLoginData();
								User.getInstance().setLoginResult(true,
										offlineLd, null);
							} else {
								// Offline login username/password mismatch
								User.getInstance().setLoginResult(false, null,
										User.LOGOUT_MESSAGE_ID_MISSMATCH);
							}
						} else {
							// Offline login data not available
							User.getInstance().setLoginResult(false, null,
									User.LOGOUT_MESSAGE_NETWORK_SERVER);
						}
					} catch (final Exception e) {
						User.getInstance().setLoginResult(false, null,
								User.LOGOUT_MESSAGE_INTERNAL_ERROR);
					}

				} else {
					// Login failed for sure, server returned 401
					User.getInstance().setLoginResult(false, null,
							User.LOGOUT_MESSAGE_ID_MISSMATCH);
				}
			}
		}
	}

	public void writeToSessionFile(final JSONObject json) throws JSONException,
			IOException {
		final File f = new File(MIntel.METADATA_PATH + "/session.tmp");
		if (!f.exists()) {
			f.createNewFile();
		} else {
			f.delete();
			f.createNewFile();
		}
		if (f.canWrite()) {
			final PrintWriter out = new PrintWriter(f);
			out.write(json.getString("id"));
			out.close();
		}
	}

	private void createCustomToast(final int iconId, final String text) {

		final LayoutInflater inflater = getLayoutInflater();
		final View layout = inflater.inflate(R.layout.custom_toast,
				(ViewGroup) findViewById(R.id.toast_layout_root));

		final ImageView icon = (ImageView) layout.findViewById(R.id.toast_icon);
		if (iconId != -1) {
			icon.setImageDrawable(getApplicationContext().getResources()
					.getDrawable(iconId));
		} else {
			icon.setVisibility(View.GONE);
		}
		final TextView msg = (TextView) layout.findViewById(R.id.toast_text);
		if (text != null) {
			msg.setText(text);
		} else {
			msg.setVisibility(View.GONE);
		}

		final Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
		toast.setDuration(500);
		toast.setView(layout);
		toast.show();
	}
}
