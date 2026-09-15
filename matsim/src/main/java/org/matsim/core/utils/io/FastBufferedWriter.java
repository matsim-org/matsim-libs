/* *********************************************************************** *
 * project: org.matsim.*
 * FastBufferedWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.io.Writer;

/**
 * A drop-in replacement for {@link java.io.BufferedWriter} intended for single-threaded use. It removes the per-call
 * synchronization of {@link java.io.BufferedWriter} and uses a large character buffer. MATSim's XML writers emit a
 * large file as a great many tiny {@code write(...)} calls; with the JDK writer each of those takes an uncontended
 * monitor, which dominates the serialization cost for large scenarios (populations, households, ...). This class makes
 * each small write a plain unsynchronized array copy and only touches the underlying writer once per full buffer.
 *
 * <p>The produced bytes are identical to those of {@link java.io.BufferedWriter}. Unlike {@code BufferedWriter} it is
 * not synchronized, so it must only be used by a single thread &ndash; which is always the case for MATSim file
 * writing.</p>
 *
 * @author Hannes Rewald
 */
final class FastBufferedWriter extends Writer {

	private static final int DEFAULT_BUFFER_SIZE = 1 << 16; // 64k chars

	private final Writer out;
	private final char[] buffer;
	private int count;

	FastBufferedWriter(final Writer out) {
		this(out, DEFAULT_BUFFER_SIZE);
	}

	FastBufferedWriter(final Writer out, final int bufferSize) {
		this.out = out;
		this.buffer = new char[bufferSize];
	}

	private void drainBuffer() throws IOException {
		if (this.count > 0) {
			this.out.write(this.buffer, 0, this.count);
			this.count = 0;
		}
	}

	@Override
	public void write(final int c) throws IOException {
		if (this.count >= this.buffer.length) {
			drainBuffer();
		}
		this.buffer[this.count++] = (char) c;
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {
		if (len >= this.buffer.length) {
			drainBuffer();
			this.out.write(cbuf, off, len);
			return;
		}
		if (this.count + len > this.buffer.length) {
			drainBuffer();
		}
		System.arraycopy(cbuf, off, this.buffer, this.count, len);
		this.count += len;
	}

	@Override
	public void write(final String s, final int off, final int len) throws IOException {
		if (len >= this.buffer.length) {
			drainBuffer();
			this.out.write(s, off, len);
			return;
		}
		if (this.count + len > this.buffer.length) {
			drainBuffer();
		}
		s.getChars(off, off + len, this.buffer, this.count);
		this.count += len;
	}

	@Override
	public Writer append(final CharSequence csq) throws IOException {
		write(String.valueOf(csq));
		return this;
	}

	@Override
	public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
		write((csq == null ? "null" : csq).subSequence(start, end).toString());
		return this;
	}

	@Override
	public Writer append(final char c) throws IOException {
		write(c);
		return this;
	}

	@Override
	public void flush() throws IOException {
		drainBuffer();
		this.out.flush();
	}

	@Override
	public void close() throws IOException {
		try {
			drainBuffer();
		} finally {
			this.out.close();
		}
	}
}
