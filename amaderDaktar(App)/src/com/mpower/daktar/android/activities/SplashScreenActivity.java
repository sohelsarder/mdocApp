package com.mpower.daktar.android.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.R;

public class SplashScreenActivity extends Activity {

	private int mImageMaxWidth;
	private final int mSplashTimeout = 2000; // milliseconds

	private AlertDialog mAlertDialog;
	private static final boolean EXIT = true;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// must be at the beginning of any activity that can be called from an
		// external intent
		try {
			MIntel.createMIntelDirs();
		} catch (final RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		mImageMaxWidth = getWindowManager().getDefaultDisplay().getWidth();

		// this splash screen should be a blank slate
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash_screen);

		// get the shared preferences object
		final SharedPreferences mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		final Editor editor = mSharedPreferences.edit();

		// get the package info object with version number
		PackageInfo packageInfo = null;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(),
					PackageManager.GET_META_DATA);
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}

		boolean firstRun = mSharedPreferences.getBoolean(
				PreferencesActivity.KEY_FIRST_RUN, true);
		// boolean showSplash =
		// mSharedPreferences.getBoolean(PreferencesActivity.KEY_SHOW_SPLASH,
		// false);
		final boolean showSplash = true;
		// String splashPath =
		// mSharedPreferences.getString(PreferencesActivity.KEY_SPLASH_PATH,
		// getString(R.string.default_splash_path));
		final String splashPath = getString(R.string.default_splash_path);

		// if you've increased version code, then update the version number and
		// set firstRun to true
		if (mSharedPreferences.getLong(PreferencesActivity.KEY_LAST_VERSION, 0) < packageInfo.versionCode) {
			editor.putLong(PreferencesActivity.KEY_LAST_VERSION,
					packageInfo.versionCode);
			editor.commit();

			firstRun = true;
		}

		// do all the first run things
		if (firstRun || showSplash) {
			editor.putBoolean(PreferencesActivity.KEY_FIRST_RUN, false);
			editor.commit();
			startSplashScreen(splashPath);
		} else {
			endSplashScreen();
		}

	}

	private void endSplashScreen() {

		// launch new activity and close splash screen
		startActivity(new Intent(SplashScreenActivity.this, LoginActivity.class));
		finish();
	}

	// decodes image and scales it to reduce memory consumption
	private Bitmap decodeFile(final File f) {
		Bitmap b = null;
		try {
			// Decode image size
			final BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			FileInputStream fis = new FileInputStream(f);
			BitmapFactory.decodeStream(fis, null, o);
			try {
				fis.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			int scale = 1;
			if (o.outHeight > mImageMaxWidth || o.outWidth > mImageMaxWidth) {
				scale = (int) Math.pow(
						2,
						(int) Math.round(Math.log(mImageMaxWidth
								/ (double) Math.max(o.outHeight, o.outWidth))
								/ Math.log(0.5)));
			}

			// Decode with inSampleSize
			final BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			fis = new FileInputStream(f);
			b = BitmapFactory.decodeStream(fis, null, o2);
			try {
				fis.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (final FileNotFoundException e) {
		}
		return b;
	}

	private void startSplashScreen(final String path) {

		// add items to the splash screen here. makes things less distracting.
		final ImageView iv = (ImageView) findViewById(R.id.splash);
		final LinearLayout ll = (LinearLayout) findViewById(R.id.splash_default);

		final File f = new File(path);
		if (f.exists()) {
			iv.setImageBitmap(decodeFile(f));
			ll.setVisibility(View.GONE);
			iv.setVisibility(View.VISIBLE);
		}

		// create a thread that counts up to the timeout
		final Thread t = new Thread() {
			int count = 0;

			@Override
			public void run() {
				try {
					super.run();
					while (count < mSplashTimeout) {
						sleep(100);
						count += 100;
					}
				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					endSplashScreen();
				}
			}
		};
		t.start();
	}

	private void createErrorDialog(final String errorMsg,
			final boolean shouldExit) {
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		final DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int i) {
				switch (i) {
				case DialogInterface.BUTTON1:
					if (shouldExit) {
						finish();
					}
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		mAlertDialog.show();
	}

}
