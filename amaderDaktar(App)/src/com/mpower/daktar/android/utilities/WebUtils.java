/*
 * Copyright (C) 2011 University of Washington
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.preferences.PreferencesActivity;

/**
 * Common utility methods for managing the credentials associated with the
 * request context and constructing http context, client and request with the
 * proper parameters and OpenRosa headers.
 *
 * @author mitchellsundt@gmail.com
 */
public final class WebUtils {
	public static final String t = "WebUtils";

	public static final String OPEN_ROSA_VERSION_HEADER = "X-OpenRosa-Version";
	public static final String OPEN_ROSA_VERSION = "1.0";
	private static final String DATE_HEADER = "Date";

	public static final String HTTP_CONTENT_TYPE_TEXT_XML = "text/xml";
	public static final int CONNECTION_TIMEOUT = 60000;

	public static final String URL_PART_LOGIN = "/m/login";
	public static final String URL_PART_SUBMISSION = "/m/submission";
	public static final String URL_PART_FORMLIST = "/m/formList";

	private static final GregorianCalendar g = new GregorianCalendar(
			TimeZone.getTimeZone("GMT"));

	public static final String URL_PART_PRESCRIPTIONLIST = "/m/prescriptionList/";
	public static final String URL_PART_PATIENTLOGIN = "/m/patLogin/";
	public static final String URL_PART_PENDINGLIST = "/m/pendingPres/?rmpId=";

	// ##### DEBUG Need to change server address
	public static final String URL_PART_FOLLOWUP = "http://27.147.138.55/prescription/Prescriptions/followUpList/";
	public static final String URL_PART_DOCTORLIST = "http://27.147.138.55/prescription/Users/getDoctorList";
	public static final String URL_PART_FOLLOWUPCHECK = "http://27.147.138.55/prescription/Prescriptions/followUpCheck?id=";
	public static final String URL_PART_UNITCOSTS = "http://27.147.138.55/prescription/UnitCosts/getUnitCost/";

	public static final String URL_PART_MISSEDCALLS = "http://27.147.138.55/prescription/prescriptions/callcount/";
	public static final String URL_PART_MISSEDCALLS_DELETE = "http://27.147.138.55/prescription/prescriptions/deletecallcount/";
	public static final String RMP_CALL_URL = "http://27.147.138.55/prescription/Videos/call/";

	public static final String URL_PART_ACCOUNTS = "/b/validate/?";
	public static final String URL_PART_BALANCE = "/b/getCurrAcc/?";
	
	public static final String URL_PART_UPLOADPIC = "/m/pictureUpload";

	public static final AuthScope buildAuthScopes(final String host) {

		AuthScope a;
		// allow digest auth on any port...
		a = new AuthScope(host, -1, null, AuthPolicy.DIGEST);

		return a;
	}

	public static void refreshCredential() {
		if (User.getInstance().isLoggedin()) {
			clearAllCredentials();
			addCredentials(User.getInstance().getUserData().getUsername(), User
					.getInstance().getUserData().getPassword());
		}
	}

	public static final void clearAllCredentials() {
		final HttpContext localContext = MIntel.getInstance().getHttpContext();
		final CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);
		credsProvider.clear();
	}

	public static final boolean hasCredentials(final String userEmail,
			final String host) {
		final HttpContext localContext = MIntel.getInstance().getHttpContext();
		final CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);

		final AuthScope as = buildAuthScopes(host);
		boolean hasCreds = true;

		final Credentials c = credsProvider.getCredentials(as);
		if (c == null) {
			hasCreds = false;
		}
		return hasCreds;
	}

	public static void addCredentials(final String username,
			final String password) {

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getAppContext());
		final String scheduleUrl = prefs.getString(
				PreferencesActivity.KEY_SERVER_URL, null);

		String host = "";

		try {
			host = new URL(URLDecoder.decode(scheduleUrl, "utf-8")).getHost();
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		addCredentials(username, password, host);
	}

	public static final void addCredentials(final String userEmail,
			final String password, final String host) {
		final HttpContext localContext = MIntel.getInstance().getHttpContext();
		final Credentials c = new UsernamePasswordCredentials(userEmail,
				password);
		addCredentials(localContext, c, host);
	}

	private static final void addCredentials(final HttpContext localContext,
			final Credentials c, final String host) {
		final CredentialsProvider credsProvider = (CredentialsProvider) localContext
				.getAttribute(ClientContext.CREDS_PROVIDER);

		final AuthScope as = buildAuthScopes(host);
		credsProvider.setCredentials(as, c);
	}

	private static final void setOpenRosaHeaders(final HttpRequest req) {
		req.setHeader(OPEN_ROSA_VERSION_HEADER, OPEN_ROSA_VERSION);
		g.setTime(new Date());
		req.setHeader(DATE_HEADER,
				DateFormat.format("E, dd MMM yyyy hh:mm:ss zz", g).toString());
	}

	public static final HttpHead createOpenRosaHttpHead(final URI uri) {
		final HttpHead req = new HttpHead(uri);
		setOpenRosaHeaders(req);
		return req;
	}

	public static final HttpGet createOpenRosaHttpGet(final URI uri) {
		return createOpenRosaHttpGet(uri, "");
	}

	public static final HttpGet createOpenRosaHttpGet(final URI uri,
			final String auth) {
		final HttpGet req = new HttpGet();
		setOpenRosaHeaders(req);
		setGoogleHeaders(req, auth);
		req.setURI(uri);
		return req;
	}

	public static final void setGoogleHeaders(final HttpRequest req,
			final String auth) {
		if (auth != null && auth.length() > 0) {
			req.setHeader("Authorization", "GoogleLogin auth=" + auth);
		}
	}

	public static final HttpPost createOpenRosaHttpPost(final URI uri) {
		return createOpenRosaHttpPost(uri, "");
	}

	public static final HttpPost createOpenRosaHttpPost(final URI uri,
			final String auth) {
		final HttpPost req = new HttpPost(uri);
		setOpenRosaHeaders(req);
		setGoogleHeaders(req, auth);
		return req;
	}

	public static final HttpClient createHttpClient(final int timeout) {
		// configure connection
		final HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, timeout);
		HttpConnectionParams.setSoTimeout(params, timeout);
		// support redirecting to handle http: => https: transition
		HttpClientParams.setRedirecting(params, true);
		// support authenticating
		HttpClientParams.setAuthenticating(params, true);
		// if possible, bias toward digest auth (may not be in 4.0 beta 2)
		final List<String> authPref = new ArrayList<String>();
		authPref.add(AuthPolicy.DIGEST);
		authPref.add(AuthPolicy.BASIC);
		// does this work in Google's 4.0 beta 2 snapshot?
		params.setParameter("http.auth-target.scheme-pref", authPref);

		// setup client
		final HttpClient httpclient = new DefaultHttpClient(params);
		httpclient.getParams().setParameter(ClientPNames.MAX_REDIRECTS, 1);
		httpclient.getParams().setParameter(
				ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);

		return httpclient;
	}

	/**
	 * Common method for returning a parsed xml document given a url and the
	 * http context and client objects involved in the web connection.
	 *
	 * @param urlString
	 * @param localContext
	 * @param httpclient
	 * @return
	 */
	public static DocumentFetchResult getXmlDocument(final String urlString,
			final HttpContext localContext, final HttpClient httpclient,
			final String auth) {
		URI u = null;
		try {
			final URL url = new URL(URLDecoder.decode(urlString, "utf-8"));
			u = url.toURI();
		} catch (final Exception e) {
			e.printStackTrace();
			return new DocumentFetchResult(e.getLocalizedMessage()
			// + app.getString(R.string.while_accessing) + urlString);
					+ "while accessing" + urlString, 0);
		}

		// set up request...
		final HttpGet req = WebUtils.createOpenRosaHttpGet(u, auth);

		HttpResponse response = null;
		try {
			response = httpclient.execute(req, localContext);
			final int statusCode = response.getStatusLine().getStatusCode();

			final HttpEntity entity = response.getEntity();

			if (entity != null
					&& (statusCode != 200 || !entity.getContentType()
							.getValue().toLowerCase()
							.contains(WebUtils.HTTP_CONTENT_TYPE_TEXT_XML))) {
				try {
					// don't really care about the stream...
					final InputStream is = response.getEntity().getContent();
					// read to end of stream...
					final long count = 1024L;
					while (is.skip(count) == count) {
						;
					}
					is.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			if (statusCode != 200) {
				final String webError = response.getStatusLine()
						.getReasonPhrase() + " (" + statusCode + ")";

				return new DocumentFetchResult(u.toString()
						+ " responded with: " + webError, statusCode);
			}

			if (entity == null) {
				final String error = "No entity body returned from: "
						+ u.toString();
				Log.e(t, error);
				return new DocumentFetchResult(error, 0);
			}

			if (!entity.getContentType().getValue().toLowerCase()
					.contains(WebUtils.HTTP_CONTENT_TYPE_TEXT_XML)) {
				final String error = "ContentType: "
						+ entity.getContentType().getValue()
						+ " returned from: "
						+ u.toString()
						+ " is not text/xml.  This is often caused a network proxy.  Do you need to login to your network?";
				Log.e(t, error);
				return new DocumentFetchResult(error, 0);
			}

			// parse response
			Document doc = null;
			try {
				InputStream is = null;
				InputStreamReader isr = null;
				try {
					is = entity.getContent();
					isr = new InputStreamReader(is, "UTF-8");
					doc = new Document();
					final KXmlParser parser = new KXmlParser();
					parser.setInput(isr);
					parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
							true);
					doc.parse(parser);
					isr.close();
				} finally {
					if (isr != null) {
						try {
							isr.close();
						} catch (final Exception e) {
							// no-op
						}
					}
					if (is != null) {
						try {
							is.close();
						} catch (final Exception e) {
							// no-op
						}
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
				final String error = "Parsing failed with " + e.getMessage()
						+ "while accessing " + u.toString();
				Log.e(t, error);
				return new DocumentFetchResult(error, 0);
			}

			boolean isOR = false;
			final Header[] fields = response
					.getHeaders(WebUtils.OPEN_ROSA_VERSION_HEADER);
			if (fields != null && fields.length >= 1) {
				isOR = true;
				boolean versionMatch = false;
				boolean first = true;
				final StringBuilder b = new StringBuilder();
				for (final Header h : fields) {
					if (WebUtils.OPEN_ROSA_VERSION.equals(h.getValue())) {
						versionMatch = true;
						break;
					}
					if (!first) {
						b.append("; ");
					}
					first = false;
					b.append(h.getValue());
				}
				if (!versionMatch) {
					Log.w(t, WebUtils.OPEN_ROSA_VERSION_HEADER
							+ " unrecognized version(s): " + b.toString());
				}
			}
			return new DocumentFetchResult(doc, isOR);
		} catch (final Exception e) {
			e.printStackTrace();
			String cause;
			if (e.getCause() != null) {
				cause = e.getCause().getMessage();
			} else {
				cause = e.getMessage();
			}
			final String error = "Error: " + cause + " while accessing "
					+ u.toString();

			Log.w(t, error);
			return new DocumentFetchResult(error, 0);
		}
	}

	public static HttpResponse stringResponseGet(final String urlString,
			final HttpContext localContext, final HttpClient httpclient)
					throws Exception {

		final URL url = new URL(URLDecoder.decode(urlString, "utf-8"));
		final URI u = url.toURI();

		final HttpGet req = new HttpGet();
		req.setURI(u);

		HttpResponse response = null;
		response = httpclient.execute(req, localContext);

		return response;
	}

	public static boolean isConnected(final Context context) {
		final ConnectivityManager conMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
		if (activeNetwork != null
				&& activeNetwork.getState() == NetworkInfo.State.CONNECTED)
			return true;
		else
			return false;
	}

	public static String getSHA512(final String input) {
		String retval = "";
		try {
			final MessageDigest m = MessageDigest.getInstance("SHA-512");
			final byte[] out = m.digest(input.getBytes());
			retval = Base64.encodeToString(out, Base64.NO_WRAP);
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return retval;
	}
	
	public static String getStringFromResponse(HttpResponse response){
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
		
		final StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
		} catch (Exception e) {
		}
		return null;
	}

}