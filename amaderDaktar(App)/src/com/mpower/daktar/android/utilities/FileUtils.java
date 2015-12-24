/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mpower.daktar.android.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.javarosa.xform.parse.XFormParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Static methods used for common file operations.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class FileUtils {
	private final static String t = "FileUtils";

	// Used to validate and display valid form names.
	public static final String VALID_FILENAME = "[ _\\-A-Za-z0-9]*.x[ht]*ml";

	public static boolean createFolder(final String path) {
		boolean made = true;
		final File dir = new File(path);
		if (!dir.exists()) {
			made = dir.mkdirs();
		}
		return made;
	}

	public static byte[] getFileAsBytes(final File file) {
		byte[] bytes = null;
		InputStream is = null;
		try {
			is = new FileInputStream(file);

			// Get the size of the file
			final long length = file.length();
			if (length > Integer.MAX_VALUE) {
				Log.e(t, "File " + file.getName() + "is too large");
				return null;
			}

			// Create the byte array to hold the data
			bytes = new byte[(int) length];

			// Read in the bytes
			int offset = 0;
			int read = 0;
			try {
				while (offset < bytes.length && read >= 0) {
					read = is.read(bytes, offset, bytes.length - offset);
					offset += read;
				}
			} catch (final IOException e) {
				Log.e(t, "Cannot read " + file.getName());
				e.printStackTrace();
				return null;
			}

			// Ensure all the bytes have been read in
			if (offset < bytes.length) {
				try {
					throw new IOException("Could not completely read file "
							+ file.getName());
				} catch (final IOException e) {
					e.printStackTrace();
					return null;
				}
			}

			return bytes;

		} catch (final FileNotFoundException e) {
			Log.e(t, "Cannot find " + file.getName());
			e.printStackTrace();
			return null;

		} finally {
			// Close the input stream
			try {
				is.close();
			} catch (final IOException e) {
				Log.e(t, "Cannot close input stream for " + file.getName());
				e.printStackTrace();
				return null;
			}
		}
	}

	public static String getMd5Hash(final File file) {
		try {
			// CTS (6/15/2010) : stream file through digest instead of handing
			// it the byte[]
			final MessageDigest md = MessageDigest.getInstance("MD5");
			final int chunkSize = 256;

			final byte[] chunk = new byte[chunkSize];

			// Get the size of the file
			final long lLength = file.length();

			if (lLength > Integer.MAX_VALUE) {
				Log.e(t, "File " + file.getName() + "is too large");
				return null;
			}

			final int length = (int) lLength;

			InputStream is = null;
			is = new FileInputStream(file);

			int l = 0;
			for (l = 0; l + chunkSize < length; l += chunkSize) {
				is.read(chunk, 0, chunkSize);
				md.update(chunk, 0, chunkSize);
			}

			final int remaining = length - l;
			if (remaining > 0) {
				is.read(chunk, 0, remaining);
				md.update(chunk, 0, remaining);
			}
			final byte[] messageDigest = md.digest();

			final BigInteger number = new BigInteger(1, messageDigest);
			String md5 = number.toString(16);
			while (md5.length() < 32) {
				md5 = "0" + md5;
			}
			is.close();
			return md5;

		} catch (final NoSuchAlgorithmException e) {
			Log.e("MD5", e.getMessage());
			return null;

		} catch (final FileNotFoundException e) {
			Log.e("No Cache File", e.getMessage());
			return null;
		} catch (final IOException e) {
			Log.e("Problem reading from file", e.getMessage());
			return null;
		}

	}

	public static Bitmap getBitmapScaledToDisplay(final File f,
			final int screenHeight, final int screenWidth) {
		// Determine image size of f
		final BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(f.getAbsolutePath(), o);

		final int heightScale = o.outHeight / screenHeight;
		final int widthScale = o.outWidth / screenWidth;

		// Powers of 2 work faster, sometimes, according to the doc.
		// We're just doing closest size that still fills the screen.
		final int scale = Math.max(widthScale, heightScale);

		// get bitmap with scale ( < 1 is the same as 1)
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = scale;
		final Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
		if (b != null) {
			Log.i(t,
					"Screen is " + screenHeight + "x" + screenWidth
					+ ".  Image has been scaled down by " + scale
					+ " to " + b.getHeight() + "x" + b.getWidth());
		}
		return b;
	}

	public static void copyFile(final File sourceFile, final File destFile) {
		if (sourceFile.exists()) {
			FileChannel src;
			try {
				src = new FileInputStream(sourceFile).getChannel();
				final FileChannel dst = new FileOutputStream(destFile)
				.getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			} catch (final FileNotFoundException e) {
				Log.e(t, "FileNotFoundExeception while copying audio");
				e.printStackTrace();
			} catch (final IOException e) {
				Log.e(t, "IOExeception while copying audio");
				e.printStackTrace();
			}
		} else {
			Log.e(t,
					"Source file does not exist: "
							+ sourceFile.getAbsolutePath());
		}

	}

	public static String FORMID = "formid";
	public static String UI = "uiversion";
	public static String MODEL = "modelversion";
	public static String TITLE = "title";
	public static String SUBMISSIONURI = "submission";

	public static HashMap<String, String> parseXML(final File xmlFile) {
		final HashMap<String, String> fields = new HashMap<String, String>();
		InputStream is;
		try {
			is = new FileInputStream(xmlFile);
		} catch (final FileNotFoundException e1) {
			throw new IllegalStateException(e1);
		}

		InputStreamReader isr;
		try {
			isr = new InputStreamReader(is, "UTF-8");
		} catch (final UnsupportedEncodingException uee) {
			Log.w(t, "UTF 8 encoding unavailable, trying default encoding");
			isr = new InputStreamReader(is);
		}

		if (isr != null) {

			Document doc;
			try {
				doc = XFormParser.getXMLDocument(isr);
			} finally {
				try {
					isr.close();
				} catch (final IOException e) {
					Log.w(t, xmlFile.getAbsolutePath()
							+ " Error closing form reader");
					e.printStackTrace();
				}
			}

			final String xforms = "http://www.w3.org/2002/xforms";
			final String html = doc.getRootElement().getNamespace();

			final Element head = doc.getRootElement().getElement(html, "head");
			final Element title = head.getElement(html, "title");
			if (title != null) {
				fields.put(TITLE, XFormParser.getXMLText(title, true));
			}

			final Element model = getChildElement(head, "model");
			Element cur = getChildElement(model, "instance");

			final int idx = cur.getChildCount();
			int i;
			for (i = 0; i < idx; ++i) {
				if (cur.isText(i)) {
					continue;
				}
				if (cur.getType(i) == Node.ELEMENT) {
					break;
				}
			}

			if (i < idx) {
				cur = cur.getElement(i); // this is the first data element
				final String id = cur.getAttributeValue(null, "id");
				final String xmlns = cur.getNamespace();
				final String modelVersion = cur.getAttributeValue(null,
						"version");
				final String uiVersion = cur.getAttributeValue(null,
						"uiVersion");

				fields.put(FORMID, id == null ? xmlns : id);
				fields.put(MODEL, modelVersion == null ? null : modelVersion);
				fields.put(UI, uiVersion == null ? null : uiVersion);
			} else
				throw new IllegalStateException(xmlFile.getAbsolutePath()
						+ " could not be parsed");
			try {
				final Element submission = model.getElement(xforms,
						"submission");
				final String submissionUri = submission.getAttributeValue(null,
						"action");
				fields.put(SUBMISSIONURI, submissionUri == null ? null
						: submissionUri);
			} catch (final Exception e) {
				Log.i(t, xmlFile.getAbsolutePath()
						+ " does not have a submission element");
				// and that's totally fine.
			}

		}
		return fields;
	}

	// needed because element.getelement fails when there are attributes
	private static Element getChildElement(final Element parent,
			final String childName) {
		final Element e = null;
		final int c = parent.getChildCount();
		int i = 0;
		for (i = 0; i < c; i++) {
			if (parent.getType(i) == Node.ELEMENT) {
				if (parent.getElement(i).getName().equalsIgnoreCase(childName))
					return parent.getElement(i);
			}
		}
		return e;
	}
}
