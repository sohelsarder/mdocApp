/**
 *
 */

package com.mpower.daktar.android.logic;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;

/**
 * @author ctsims
 */
public class FileReferenceFactory extends PrefixedRootFactory {

	String localRoot;

	public FileReferenceFactory(final String localRoot) {
		super(new String[] { "file" });
		this.localRoot = localRoot;
	}

	@Override
	protected Reference factory(final String terminal, final String URI) {
		return new FileReference(localRoot, terminal);
	}

}
