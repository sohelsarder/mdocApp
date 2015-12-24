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

package com.mpower.daktar.android.tasks;

import java.util.HashMap;

import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;
import org.javarosa.xform.parse.XFormParser;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mpower.daktar.android.application.MIntel;
import com.mpower.daktar.android.listeners.FormListDownloaderListener;
import com.mpower.daktar.android.logic.FormDetails;
import com.mpower.daktar.android.preferences.PreferencesActivity;
import com.mpower.daktar.android.utilities.DocumentFetchResult;
import com.mpower.daktar.android.utilities.WebUtils;
import com.mpower.daktar.android.R;

/**
 * Background task for downloading forms from urls or a formlist from a url. We
 * overload this task a bit so that we don't have to keep track of two separate
 * downloading tasks and it simplifies interfaces. If LIST_URL is passed to
 * doInBackground(), we fetch a form list. If a hashmap containing form/url
 * pairs is passed, we download those forms.
 *
 * @author carlhartung
 */
public class DownloadFormListTask extends
		AsyncTask<Void, String, HashMap<String, FormDetails>> {
	private static final String t = "DownloadFormsTask";

	// used to store error message if one occurs
	public static final String DL_ERROR_MSG = "dlerrormessage";
	public static final String DL_AUTH_REQUIRED = "dlauthrequired";

	private FormListDownloaderListener mStateListener;

	private static final String NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_LIST = "http://openrosa.org/xforms/xformsList";

	private boolean isXformsListNamespacedElement(final Element e) {
		return e.getNamespace().equalsIgnoreCase(
				NAMESPACE_OPENROSA_ORG_XFORMS_XFORMS_LIST);
	}

	@Override
	protected HashMap<String, FormDetails> doInBackground(final Void... values) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(MIntel.getInstance()
						.getBaseContext());
		String downloadListUrl = settings.getString(
				PreferencesActivity.KEY_SERVER_URL, MIntel.getInstance()
						.getString(R.string.default_server_url));

		downloadListUrl += WebUtils.URL_PART_FORMLIST;
		final String auth = "";

		// We populate this with available forms from the specified server.
		// <formname, details>
		final HashMap<String, FormDetails> formList = new HashMap<String, FormDetails>();

		// get shared HttpContext so that authentication and cookies are
		// retained.
		final HttpContext localContext = MIntel.getInstance().getHttpContext();
		final HttpClient httpclient = WebUtils
				.createHttpClient(WebUtils.CONNECTION_TIMEOUT);

		final DocumentFetchResult result = WebUtils.getXmlDocument(
				downloadListUrl, localContext, httpclient, auth);

		// If we can't get the document, return the error, cancel the task
		if (result.errorMessage != null) {
			if (result.responseCode == 401) {
				formList.put(DL_AUTH_REQUIRED, new FormDetails(
						result.errorMessage));
			} else {
				formList.put(DL_ERROR_MSG, new FormDetails(result.errorMessage));
			}
			return formList;
		}

		if (result.isOpenRosaResponse) {
			// Attempt OpenRosa 1.0 parsing
			final Element xformsElement = result.doc.getRootElement();
			if (!xformsElement.getName().equals("xforms")) {
				final String error = "root element is not <xforms> : "
						+ xformsElement.getName();
				Log.e(t, "Parsing OpenRosa reply -- " + error);
				formList.put(
						DL_ERROR_MSG,
						new FormDetails(MIntel.getInstance().getString(
								R.string.parse_openrosa_formlist_failed, error)));
				return formList;
			}
			final String namespace = xformsElement.getNamespace();
			if (!isXformsListNamespacedElement(xformsElement)) {
				final String error = "root element namespace is incorrect:"
						+ namespace;
				Log.e(t, "Parsing OpenRosa reply -- " + error);
				formList.put(
						DL_ERROR_MSG,
						new FormDetails(MIntel.getInstance().getString(
								R.string.parse_openrosa_formlist_failed, error)));
				return formList;
			}
			final int nElements = xformsElement.getChildCount();
			for (int i = 0; i < nElements; ++i) {
				if (xformsElement.getType(i) != Node.ELEMENT) {
					// e.g., whitespace (text)
					continue;
				}
				final Element xformElement = xformsElement.getElement(i);
				if (!isXformsListNamespacedElement(xformElement)) {
					// someone else's extension?
					continue;
				}
				final String name = xformElement.getName();
				if (!name.equalsIgnoreCase("xform")) {
					// someone else's extension?
					continue;
				}

				// this is something we know how to interpret
				String formId = null;
				String formName = null;
				String majorMinorVersion = null;
				String description = null;
				String downloadUrl = null;
				String manifestUrl = null;
				// don't process descriptionUrl
				final int fieldCount = xformElement.getChildCount();
				for (int j = 0; j < fieldCount; ++j) {
					if (xformElement.getType(j) != Node.ELEMENT) {
						// whitespace
						continue;
					}
					final Element child = xformElement.getElement(j);
					if (!isXformsListNamespacedElement(child)) {
						// someone else's extension?
						continue;
					}
					final String tag = child.getName();
					if (tag.equals("formID")) {
						formId = XFormParser.getXMLText(child, true);
						if (formId != null && formId.length() == 0) {
							formId = null;
						}
					} else if (tag.equals("name")) {
						formName = XFormParser.getXMLText(child, true);
						if (formName != null && formName.length() == 0) {
							formName = null;
						}
					} else if (tag.equals("majorMinorVersion")) {
						majorMinorVersion = XFormParser.getXMLText(child, true);
						if (majorMinorVersion != null
								&& majorMinorVersion.length() == 0) {
							majorMinorVersion = null;
						}
					} else if (tag.equals("descriptionText")) {
						description = XFormParser.getXMLText(child, true);
						if (description != null && description.length() == 0) {
							description = null;
						}
					} else if (tag.equals("downloadUrl")) {
						downloadUrl = XFormParser.getXMLText(child, true);
						if (downloadUrl != null && downloadUrl.length() == 0) {
							downloadUrl = null;
						}
					} else if (tag.equals("manifestUrl")) {
						manifestUrl = XFormParser.getXMLText(child, true);
						if (manifestUrl != null && manifestUrl.length() == 0) {
							manifestUrl = null;
						}
					}
				}
				if (formId == null || downloadUrl == null || formName == null) {
					final String error = "Forms list entry "
							+ Integer.toString(i)
							+ " is missing one or more tags: formId, name, or downloadUrl";
					Log.e(t, "Parsing OpenRosa reply -- " + error);
					formList.clear();
					formList.put(
							DL_ERROR_MSG,
							new FormDetails(MIntel.getInstance().getString(
									R.string.parse_openrosa_formlist_failed,
									error)));
					return formList;
				}
				/*
				 * TODO: We currently don't care about major/minor version.
				 * maybe someday we will.
				 */
				// Integer modelVersion = null;
				// Integer uiVersion = null;
				// try {
				// if (majorMinorVersion == null || majorMinorVersion.length()
				// == 0) {
				// modelVersion = null;
				// uiVersion = null;
				// } else {
				// int idx = majorMinorVersion.indexOf(".");
				// if (idx == -1) {
				// modelVersion = Integer.parseInt(majorMinorVersion);
				// uiVersion = null;
				// } else {
				// modelVersion =
				// Integer.parseInt(majorMinorVersion.substring(0, idx));
				// uiVersion =
				// (idx == majorMinorVersion.length() - 1) ? null : Integer
				// .parseInt(majorMinorVersion.substring(idx + 1));
				// }
				// }
				// } catch (Exception e) {
				// e.printStackTrace();
				// String error = "Forms list entry " + Integer.toString(i) +
				// " has an invalid majorMinorVersion: " + majorMinorVersion;
				// Log.e(t, "Parsing OpenRosa reply -- " + error);
				// formList.clear();
				// formList.put(DL_ERROR_MSG, new FormDetails(
				// MIntel.getInstance().getString(R.string.parse_openrosa_formlist_failed,
				// error)));
				// return formList;
				// }
				formList.put(formId, new FormDetails(formName, downloadUrl,
						manifestUrl, formId));
			}
		} else {
			// Aggregate 0.9.x mode...
			// populate HashMap with form names and urls
			final Element formsElement = result.doc.getRootElement();
			final int formsCount = formsElement.getChildCount();
			for (int i = 0; i < formsCount; ++i) {
				if (formsElement.getType(i) != Node.ELEMENT) {
					// whitespace
					continue;
				}
				final Element child = formsElement.getElement(i);
				final String tag = child.getName();
				String formId = null;
				if (tag.equals("formID")) {
					formId = XFormParser.getXMLText(child, true);
					if (formId != null && formId.length() == 0) {
						formId = null;
					}
				}
				if (tag.equalsIgnoreCase("form")) {
					String formName = XFormParser.getXMLText(child, true);
					if (formName != null && formName.length() == 0) {
						formName = null;
					}
					String downloadUrl = child.getAttributeValue(null, "url");
					downloadUrl = downloadUrl.trim();
					if (downloadUrl != null && downloadUrl.length() == 0) {
						downloadUrl = null;
					}
					if (downloadUrl == null || formName == null) {
						final String error = "Forms list entry "
								+ Integer.toString(i)
								+ " is missing form name or url attribute";
						Log.e(t, "Parsing OpenRosa reply -- " + error);
						formList.clear();
						formList.put(
								DL_ERROR_MSG,
								new FormDetails(MIntel.getInstance().getString(
										R.string.parse_legacy_formlist_failed,
										error)));
						return formList;
					}
					formList.put(formName, new FormDetails(formName,
							downloadUrl, null, formName));
				}
			}
		}
		return formList;
	}

	@Override
	protected void onPostExecute(final HashMap<String, FormDetails> value) {
		synchronized (this) {
			if (mStateListener != null) {
				mStateListener.formListDownloadingComplete(value);
			}
		}
	}

	public void setDownloaderListener(final FormListDownloaderListener sl) {
		synchronized (this) {
			mStateListener = sl;
		}
	}

}
