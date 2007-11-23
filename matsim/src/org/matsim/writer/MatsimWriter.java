/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.writer;

import java.io.IOException;
import java.io.Writer;

import org.matsim.utils.io.IOUtils;

/**
 * A simple, abstract helper class to open files for writing with support for
 * gzip-compression.
 *
 * @author mrieser
 */
public abstract class MatsimWriter {

	/** The Unix newline character. */
	protected static final String NL = "\n";

	/** The writer output can be written to. */
	protected Writer writer = null;

	/** Whether or not the output is gzip-compressed. If <code>null</code>, the
	 * usage of compression is decided by the filename (whether it ends with .gz
	 * or not). */
	protected Boolean useCompression = null;

	/**
	 * Sets whether the file should be gzip-compressed or not. Must be set before
	 * the file is opened for writing. If not set explicitly, the usage of
	 * compression is defined by the ending of the filename.
	 *
	 * @param useCompression
	 */
	public void useCompression(final boolean useCompression) {
		this.useCompression = Boolean.valueOf(useCompression);
	}

	/**
	 * Opens the specified file for writing.
	 *
	 * @param filename
	 * @throws IOException
	 */
	protected void openFile(final String filename)  throws IOException {
		if (this.useCompression == null) {
			this.writer = IOUtils.getBufferedWriter(filename);
		} else {
			this.writer = IOUtils.getBufferedWriter(filename, this.useCompression.booleanValue());
		}
	}

	/**
	 * Closes the file if it is still open.
	 *
	 * @throws IOException
	 */
	protected void close() throws IOException {
		if (this.writer != null) {
			this.writer.close();
			this.writer = null;
		}
	}
}
