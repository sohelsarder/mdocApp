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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import android.util.Log;

public class UrlUtils {

	public static boolean isValidUrl(final String url) {

		try {
			new URL(URLDecoder.decode(url, "utf-8"));
			return true;
		} catch (final MalformedURLException e) {
			return false;
		} catch (final UnsupportedEncodingException e) {
			return false;
		}

	}

	public static String getPortNumber(final String url) {
		final String[] segments = url.split(":");
		Log.e("1st Pass: ", segments[0] + segments[1] + segments[2]);
		final String[] port = segments[2].split("/");
		Log.e("2nd Pass: ", port[0]);
		return port[0];
	}

}
