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

package org.matsim.core.utils.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;


/**
 * A simple, abstract helper class to open files for writing with support for
 * gzip-compression.
 *
 * @author mrieser
 */
public abstract class AbstractMatsimWriter {

	/** The Unix newline character. */
	protected static final String NL = "\n";

	/** The writer output can be written to. */
	protected BufferedWriter writer = null;

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
	public final void useCompression(final boolean useCompression) {
		this.useCompression = useCompression;
	}

	/**
	 * Opens the specified file for writing.
	 *
	 * @param filename
	 * @throws UncheckedIOException
	 */
	protected final void openFile(final String filename) throws UncheckedIOException {
		assertNotAlreadyOpen();
		if (this.useCompression == null) {
			this.writer = IOUtils.getBufferedWriter(filename);
		} else {
			this.writer = IOUtils.getBufferedWriter(filename + ".gz");
		}
	}

	/**
	 * Uses the specified OutputStream for writing.
	 *
	 */
	protected final void openOutputStream(OutputStream outputStream) {
		assertNotAlreadyOpen();
		try {
			if (this.useCompression == null || this.useCompression) {
				this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
			} else {
				this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(outputStream), StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void assertNotAlreadyOpen() {
		if (this.writer != null) {
			throw new RuntimeException("File already open.");
		}
	}

	/**
	 * Closes the file if it is still open.
	 *
	 * @throws UncheckedIOException
	 */
	protected final void close() throws UncheckedIOException {
		if (this.writer != null) {
			try {
				this.writer.flush();
				this.writer.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
				this.writer = null;
			}
		}
	}

}
