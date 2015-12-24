package com.mpower.daktar.android.utilities;

import org.kxml2.kdom.Document;

public class DocumentFetchResult {
	public final String errorMessage;
	public final int responseCode;
	public final Document doc;
	public final boolean isOpenRosaResponse;

	public DocumentFetchResult(final String msg, final int response) {
		responseCode = response;
		errorMessage = msg;
		doc = null;
		isOpenRosaResponse = false;
	}

	public DocumentFetchResult(final Document doc,
			final boolean isOpenRosaResponse) {
		responseCode = 0;
		errorMessage = null;
		this.doc = doc;
		this.isOpenRosaResponse = isOpenRosaResponse;
	}
}