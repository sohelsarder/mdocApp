/**
 *
 */

package com.mpower.daktar.android.views;

import java.io.File;
import java.io.IOException;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.mpower.daktar.android.R;

/**
 * @author ctsims
 * @author carlhartung
 */
public class AudioButton extends ImageButton implements OnClickListener {
	private final static String t = "AudioButton";
	private final String URI;
	private MediaPlayer player;

	public AudioButton(final Context context, final String URI) {
		super(context);
		setOnClickListener(this);
		this.URI = URI;
		final Bitmap b = BitmapFactory.decodeResource(context.getResources(),
				android.R.drawable.ic_lock_silent_mode_off);
		setImageBitmap(b);
		player = null;
	}

	@Override
	public void onClick(final View v) {
		if (URI == null) {
			// No audio file specified
			Log.e(t, "No audio file was specified");
			Toast.makeText(getContext(),
					getContext().getString(R.string.audio_file_error),
					Toast.LENGTH_LONG).show();
			return;
		}

		String audioFilename = "";
		try {
			audioFilename = ReferenceManager._().DeriveReference(URI)
					.getLocalURI();
		} catch (final InvalidReferenceException e) {
			Log.e(t, "Invalid reference exception");
			e.printStackTrace();
		}

		final File audioFile = new File(audioFilename);
		if (!audioFile.exists()) {
			// We should have an audio clip, but the file doesn't exist.
			final String errorMsg = getContext().getString(
					R.string.file_missing, audioFile);
			Log.e(t, errorMsg);
			Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
			return;
		}

		// In case we're currently playing sounds.
		stopPlaying();

		player = new MediaPlayer();
		try {
			player.setDataSource(audioFilename);
			player.prepare();
			player.start();
			player.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(final MediaPlayer mediaPlayer) {
					mediaPlayer.release();
				}

			});
		} catch (final IOException e) {
			final String errorMsg = getContext().getString(
					R.string.audio_file_invalid);
			Log.e(t, errorMsg);
			Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

	}

	public void stopPlaying() {
		if (player != null) {
			player.release();
		}
	}
}
