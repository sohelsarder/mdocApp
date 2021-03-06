package com.mpower.daktar.android.utilities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mpower.daktar.android.activities.LoginActivity;
import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.models.UserData;
import com.mpower.daktar.android.preferences.PreferencesActivity;

public class User {

	public static final String BROADCAST_ACTION_AUTHENTICATION_DONE = "com.mpower.daktar.android.broadcast.AUTHENTICATION_DONE";
	public static final String BROADCAST_ACTION_AUTHENTICATION_NEEDED = "com.mpower.daktar.android.broadcast.AUTHENTICATION_NEEDED";
	public static final String BROADCAST_ACTION_SECTOR_CHANGED = "com.mpower.mcare.broadcast.SECTOR_CHANGED";

	public static final String LOGOUT_MESSAGE_NETWORK = "Internet not available.";
	public static final String LOGOUT_MESSAGE_NETWORK_SERVER = "Internet not available or server caused an error.";
	// Edited to change the error message
	// Original message = ID/password mismatch.
	public static final String LOGOUT_MESSAGE_ID_MISSMATCH = "Wrong QR Code entered. Please use the QR Code in your ID Card.";
	public static final String LOGOUT_MESSAGE_SERVER_ERROR = "Server unreachable or caused an error.";
	public static final String LOGOUT_MESSAGE_SESSION_EXPIRED = "Session expired.";
	public static final String LOGOUT_MESSAGE_INTERNAL_ERROR = "Internal application error.";
	public static final String LOGOUT_MESSAGE_USER_REQUEST = "Logged out.";
	public static final String LOGOUT_MESSAGE_UNKNOWN = "Unknown error.";

	private static boolean OFFLINE_LOGIN_DEFAULT = false;

	private static volatile User instance = null;

	private volatile UserData mUserData = null;
	private volatile boolean mLoggedin = false;

	private User() {
		mUserData = new UserData();
	}

	private void loginFinalCheck(final boolean initialLoginPassed,
			final String logoutMessage) {
		if (initialLoginPassed) {
			if (mUserData.getUsername().length() > 0
					&& mUserData.getPassword().length() > 0) {
				login();
			} else {
				logOff(LOGOUT_MESSAGE_INTERNAL_ERROR);
			}
		} else {
			logOff(logoutMessage);
		}
	}

	public boolean isLoggedin() {
		return mLoggedin;
	}

	public void setLoginResult(final boolean succeed, final UserData ld,
			final String logoutMessage) {
		if (succeed && ld != null) {
			mUserData = ld;
			loginFinalCheck(true, null);
		} else {
			loginFinalCheck(false, logoutMessage);
		}
	}

	public boolean checkOfflineLogin(final String username,
			final String password) throws Exception {
		boolean retVal = false;

		if ("".equals(username.trim()) || "".equals(password.trim()))
			return retVal;

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getAppContext());
		final String storedUsername = settings.getString(
				PreferencesActivity.KEY_USERNAME, null);
		final String storedPassword = settings.getString(
				PreferencesActivity.KEY_PASSWORD, null);

		if (username.equals(storedUsername) && password.equals(storedPassword)) {
			retVal = true;
		}

		return retVal;
	}

	public UserData extractOfflineLoginData() {

		final UserData lr = new UserData();

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getAppContext());
		final String storedUsername = settings.getString(
				PreferencesActivity.KEY_USERNAME, null);
		final String storedPassword = settings.getString(
				PreferencesActivity.KEY_PASSWORD, null);

		lr.setUsername(storedUsername);
		lr.setPassword(storedPassword);

		return lr;
	}

	public boolean offlineUserDataAvailable() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getAppContext());
		final String storedUsername = settings.getString(
				PreferencesActivity.KEY_USERNAME, null);
		final String storedPassword = settings.getString(
				PreferencesActivity.KEY_PASSWORD, null);

		if (storedUsername != null && storedPassword != null) {
			if (storedUsername.length() > 0 && storedPassword.length() > 0)
				return true;
			else
				return false;
		} else
			return false;
	}

	public void checkLogin(final String u, final String p,
			final LoginActivity activityInstance) throws Exception {

		final boolean offlineUserDataAvailable = offlineUserDataAvailable();

		if (OFFLINE_LOGIN_DEFAULT) {
			// Offline login default
			if (offlineUserDataAvailable) {
				// Login offline if data is available
				if (checkOfflineLogin(u, p)) {
					final UserData ld = extractOfflineLoginData();
					setLoginResult(true, ld, null);
				} else {
					// Login doesn't match with offline data, check online
					if (WebUtils.isConnected(MIntel.getAppContext())) {
						validateLoginFromServer(activityInstance);
					} else {
						loginFinalCheck(false, LOGOUT_MESSAGE_ID_MISSMATCH
								+ "\n" + LOGOUT_MESSAGE_NETWORK);
					}
				}
			} else {
				// User data is not available, proceed with regular login
				if (WebUtils.isConnected(MIntel.getAppContext())) {
					validateLoginFromServer(activityInstance);
				} else {
					loginFinalCheck(false, LOGOUT_MESSAGE_NETWORK);
				}
			}

		} else {
			// Online login default
			if (WebUtils.isConnected(MIntel.getAppContext())) {
				validateLoginFromServer(activityInstance);
			} else {
				// Network not available
				if (offlineUserDataAvailable) {
					if (checkOfflineLogin(u, p)) {
						final UserData ld = extractOfflineLoginData();
						setLoginResult(true, ld, null);
					} else {
						loginFinalCheck(false, LOGOUT_MESSAGE_ID_MISSMATCH
								+ "\n" + LOGOUT_MESSAGE_NETWORK);
					}
				} else {
					loginFinalCheck(false, LOGOUT_MESSAGE_NETWORK);
				}
			}
		}

	}

	private void validateLoginFromServer(final LoginActivity activityInstance) {
		activityInstance.checkLoginOnline();
	}

	public void logOff(final String logoutMessage) {
		mUserData.resetAll();
		mLoggedin = false;

		WebUtils.clearAllCredentials();

		final Intent broadcastIntent = new Intent();
		broadcastIntent.putExtra("message", logoutMessage);
		broadcastIntent.setAction(BROADCAST_ACTION_AUTHENTICATION_NEEDED);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		MIntel.getAppContext().sendBroadcast(broadcastIntent);
	}

	public void logOffAndClearCache() {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getAppContext());

		settings.edit().putString(PreferencesActivity.KEY_USERNAME, null)
		.commit();
		settings.edit().putString(PreferencesActivity.KEY_PASSWORD, null)
		.commit();

		logOff(LOGOUT_MESSAGE_SESSION_EXPIRED);
	}

	private void login() {

		mLoggedin = true;

		// Let all broadcast receivers know that we have logged in
		final Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(BROADCAST_ACTION_AUTHENTICATION_DONE);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		MIntel.getAppContext().sendBroadcast(broadcastIntent);

		WebUtils.clearAllCredentials();
		WebUtils.addCredentials(mUserData.getUsername(),
				mUserData.getPassword());

		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getAppContext());

		settings.edit()
		.putString(PreferencesActivity.KEY_USERNAME,
				mUserData.getUsername()).commit();
		settings.edit()
		.putString(PreferencesActivity.KEY_PASSWORD,
				mUserData.getPassword()).commit();
	}

	public UserData getUserData() {
		return mUserData;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public static User getInstance() {
		if (instance == null) {
			synchronized (User.class) {
				if (instance == null) {
					instance = new User();
				}
			}
		}
		return instance;
	}

}
