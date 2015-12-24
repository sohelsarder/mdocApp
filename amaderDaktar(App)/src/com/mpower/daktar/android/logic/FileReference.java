/**
 *
 */

package com.mpower.daktar.android.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.javarosa.core.reference.Reference;

/**
 * @author ctsims
 */
public class FileReference implements Reference {
	String localPart;
	String referencePart;

	public FileReference(final String localPart, final String referencePart) {
		this.localPart = localPart;
		this.referencePart = referencePart;
	}

	private String getInternalURI() {
		return "/" + localPart + referencePart;
	}

	@Override
	public boolean doesBinaryExist() {
		return new File(getInternalURI()).exists();
	}

	@Override
	public InputStream getStream() throws IOException {
		return new FileInputStream(getInternalURI());
	}

	@Override
	public String getURI() {
		return "jr://file" + referencePart;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new FileOutputStream(getInternalURI());
	}

	@Override
	public void remove() {
		// TODO bad practice to ignore return values
		new File(getInternalURI()).delete();
	}

	@Override
	public String getLocalURI() {
		return getInternalURI();
	}

}