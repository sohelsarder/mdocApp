package com.mpower.daktar.android.activities;

import java.io.File;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.tasks.PictureUploadTask;
import com.mpower.daktar.android.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PictureUpload extends Activity {

	private Button uploadPicture;
	private ImageView imagePreview;
	
	private int PICTURE_REQUEST_CODE =  21;
	
	private String rmpId;
	private String patientId;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent callee = getIntent();
		if (callee != null){
			rmpId = callee.getStringExtra("rmpId");
			patientId = callee.getStringExtra("patId");
		}
		
		setContentView(R.layout.activity_picture_upload);
		imagePreview = (ImageView) findViewById(R.id.imagePreview);
		uploadPicture = (Button) findViewById(R.id.submit);
		uploadPicture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent pic = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
				pic.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(new File(MIntel.TMPFILE_PATH)));
				PictureUpload.this.startActivityForResult(pic, PICTURE_REQUEST_CODE);
		}
		});
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICTURE_REQUEST_CODE){
			if (resultCode == Activity.RESULT_OK){
				final File fi = new File(MIntel.TMPFILE_PATH);

				final String s = MIntel.METADATA_PATH + "/" + System.currentTimeMillis()
						+ ".jpg";

				final File nf = new File(s);
				if (!fi.renameTo(nf)) {
					Log.e("picture Upload", "Failed to rename " + fi.getAbsolutePath());
				} else {
					Log.i("Picture Upload",
							"renamed " + fi.getAbsolutePath() + " to "
									+ nf.getAbsolutePath());
					
					ContentValues values = new ContentValues(6);
					values.put(MediaColumns.TITLE, nf.getName());
					values.put(MediaColumns.DISPLAY_NAME, nf.getName());
					values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
					values.put(MediaColumns.MIME_TYPE, "image/jpeg");
					values.put(MediaColumns.DATA, nf.getAbsolutePath());

					Uri imageURI = getContentResolver().insert(
							Images.Media.EXTERNAL_CONTENT_URI, values);
					
					imagePreview.setImageURI(imageURI);
					Toast.makeText(getApplicationContext(), imageURI.toString(), 3000).show();
					PictureUploadTask picTask = new PictureUploadTask(this);
					picTask.execute(nf.getAbsolutePath(), rmpId, patientId);
					
				}
			}
		}
	}
}