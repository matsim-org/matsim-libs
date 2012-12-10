/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

// based on code from http://stackoverflow.com/questions/1835430

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * The <code>UnicodeBOMInputStream</code> class wraps any
 * <code>InputStream</code> and detects the presence of any Unicode BOM (Byte
 * Order Mark) at its beginning, as defined by <a
 * href="http://www.faqs.org/rfcs/rfc3629.html">RFC 3629 - UTF-8, a
 * transformation format of ISO 10646</a>
 * 
 * <p>
 * The <a href="http://www.unicode.org/unicode/faq/utf_bom.html">Unicode FAQ</a>
 * defines 5 types of BOMs:
 * <ul>
 * <li>
 * 
 * <pre>
 * 00 00 FE FF  = UTF-32, big-endian
 * </pre>
 * 
 * </li>
 * <li>
 * 
 * <pre>
 * FF FE 00 00  = UTF-32, little-endian
 * </pre>
 * 
 * </li>
 * <li>
 * 
 * <pre>
 * FE FF        = UTF-16, big-endian
 * </pre>
 * 
 * </li>
 * <li>
 * 
 * <pre>
 * FF FE        = UTF-16, little-endian
 * </pre>
 * 
 * </li>
 * <li>
 * 
 * <pre>
 * EF BB BF     = UTF-8
 * </pre>
 * 
 * </li>
 * </ul>
 * </p>
 * 
 * <p>
 * Use the {@link #getBOM()} method to know whether a BOM has been detected or
 * not.
 * </p>
 */
public class UnicodeInputStream extends InputStream {

	private final PushbackInputStream in;
	private final BOM bom;

	/**
	 * Wraps the existing inputStream and tries to detect if it contains a Byte Order Mark (BOM). 
	 * If it does, the BOM is skipped by default.
	 * 
	 * @param inputStream
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public UnicodeInputStream(final InputStream inputStream) throws NullPointerException, IOException {
		this(inputStream, true);
	}
	
	public UnicodeInputStream(final InputStream inputStream, final boolean skipBom) throws NullPointerException, IOException {
		if (inputStream == null) {
			throw new NullPointerException("invalid input stream: null is not allowed");
		}

		in = new PushbackInputStream(inputStream, 4);

		final byte bytes[] = new byte[4];
		final int read = in.read(bytes);

		switch (read) {
		case 4:
			if ((bytes[0] == (byte) 0xFF) && (bytes[1] == (byte) 0xFE)
					&& (bytes[2] == (byte) 0x00) && (bytes[3] == (byte) 0x00)) {
				bom = BOM.UTF_32_LE;
				break;
			} else if ((bytes[0] == (byte) 0x00) && (bytes[1] == (byte) 0x00)
					&& (bytes[2] == (byte) 0xFE) && (bytes[3] == (byte) 0xFF)) {
				bom = BOM.UTF_32_BE;
				break;
			}

		case 3:
			if ((bytes[0] == (byte) 0xEF) && (bytes[1] == (byte) 0xBB)
					&& (bytes[2] == (byte) 0xBF)) {
				bom = BOM.UTF_8;
				break;
			}

		case 2:
			if ((bytes[0] == (byte) 0xFF) && (bytes[1] == (byte) 0xFE)) {
				bom = BOM.UTF_16_LE;
				break;
			} else if ((bytes[0] == (byte) 0xFE) && (bytes[1] == (byte) 0xFF)) {
				bom = BOM.UTF_16_BE;
				break;
			}

		default:
			bom = BOM.NONE;
			break;
		}

		if (read > 0) {
			in.unread(bytes, 0, read);
		}
		if (skipBom) {
			in.skip(bom.bytes.length);
		}
	}

	public final BOM getBOM() {
		return bom;
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public int read(final byte b[]) throws IOException, NullPointerException {
		return in.read(b, 0, b.length);
	}

	@Override
	public int read(final byte b[], final int off, final int len)
			throws IOException, NullPointerException {
		return in.read(b, off, len);
	}

	@Override
	public long skip(final long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public synchronized void mark(final int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	public static final class BOM {
		public static final BOM NONE = new BOM(new byte[] {}, "NONE");
		public static final BOM UTF_8 = new BOM(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, "UTF-8");
		public static final BOM UTF_16_LE = new BOM(new byte[] { (byte) 0xFF, (byte) 0xFE }, "UTF-16 little-endian");
		public static final BOM UTF_16_BE = new BOM(new byte[] { (byte) 0xFE, (byte) 0xFF }, "UTF-16 big-endian");
		public static final BOM UTF_32_LE = new BOM(new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00 }, "UTF-32 little-endian");
		public static final BOM UTF_32_BE = new BOM(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF }, "UTF-32 big-endian");

		final byte bytes[];
		private final String description;

		@Override
		public final String toString() {
			return description;
		}

		public final byte[] getBytes() {
			final int length = bytes.length;
			final byte[] result = new byte[length];

			// Make a defensive copy
			System.arraycopy(bytes, 0, result, 0, length);

			return result;
		}

		private BOM(final byte bom[], final String description) {
			bytes = bom;
			this.description = description;
		}
	}

}