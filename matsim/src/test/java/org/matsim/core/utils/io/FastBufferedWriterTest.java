/* *********************************************************************** *
 * project: org.matsim.*
 * FastBufferedWriterTest.java
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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FastBufferedWriter}. The central property under test is that {@code FastBufferedWriter} produces
 * byte-for-byte the same output as {@link java.io.BufferedWriter} for every write path and at every buffer boundary,
 * while batching the data into far fewer calls to the underlying writer.
 *
 * @author Hannes Rewald
 */
class FastBufferedWriterTest {

	// ---------------------------------------------------------------------------------------------------------------
	// Equivalence with java.io.BufferedWriter
	// ---------------------------------------------------------------------------------------------------------------

	/** Applies the same operations to a FastBufferedWriter and a reference java.io.BufferedWriter and compares. */
	private static void assertEquivalent(final int bufferSize, final WriterScript script) throws IOException {
		final StringWriter fastTarget = new StringWriter();
		final StringWriter refTarget = new StringWriter();

		final FastBufferedWriter fast = new FastBufferedWriter(fastTarget, bufferSize);
		final java.io.BufferedWriter ref = new java.io.BufferedWriter(refTarget);

		script.run(fast);
		script.run(ref);

		fast.flush();
		ref.flush();

		assertEquals(refTarget.toString(), fastTarget.toString(),
				"FastBufferedWriter output must equal java.io.BufferedWriter output (bufferSize=" + bufferSize + ")");
	}

	@FunctionalInterface
	private interface WriterScript {
		void run(Writer writer) throws IOException;
	}

	@Test
	void writeSingleChars() throws IOException {
		for (int bufferSize : new int[] { 1, 2, 3, 8, 64 }) {
			assertEquivalent(bufferSize, w -> {
				for (int i = 0; i < 200; i++) {
					w.write('a' + (i % 26));
				}
			});
		}
	}

	@Test
	void writeFullStrings() throws IOException {
		for (int bufferSize : new int[] { 1, 4, 16, 64 }) {
			assertEquivalent(bufferSize, w -> {
				w.write("");
				w.write("a");
				w.write("hello world");
				w.write("a string that is clearly longer than the small buffers under test");
			});
		}
	}

	@Test
	void writeStringRegion() throws IOException {
		final String source = "0123456789abcdefghijklmnopqrstuvwxyz";
		for (int bufferSize : new int[] { 1, 3, 7, 16, 64 }) {
			assertEquivalent(bufferSize, w -> {
				w.write(source, 0, 0);   // empty region
				w.write(source, 0, 5);
				w.write(source, 10, 20); // region longer than small buffers
				w.write(source, source.length() - 1, 1);
			});
		}
	}

	@Test
	void writeCharArrayRegion() throws IOException {
		final char[] source = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
		for (int bufferSize : new int[] { 1, 3, 7, 16, 64 }) {
			assertEquivalent(bufferSize, w -> {
				w.write(source, 0, 0);   // empty region
				w.write(source, 0, 5);
				w.write(source, 10, 20); // region longer than small buffers
				w.write(source);         // whole array
			});
		}
	}

	@Test
	void appendVariants() throws IOException {
		for (int bufferSize : new int[] { 1, 4, 16, 64 }) {
			assertEquivalent(bufferSize, w -> {
				w.append('x');
				w.append("charsequence");
				w.append("subsequence-source", 3, 11);
				w.append((CharSequence) null);        // must yield "null"
				w.append((CharSequence) null, 1, 3);  // must yield "ul"
				w.append(new StringBuilder("from-builder"));
			});
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Buffer-boundary behaviour
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void exactlyFillsBufferThenWritesMore() throws IOException {
		final int bufferSize = 8;
		assertEquivalent(bufferSize, w -> {
			w.write("01234567"); // exactly fills the buffer
			w.write('8');          // forces a drain, then stores one char
			w.write("9abcdef0");   // exactly fills again
			w.write("X");          // drain + store
		});
	}

	@Test
	void writeLargerThanBufferBypassesBuffer() throws IOException {
		final int bufferSize = 4;
		assertEquivalent(bufferSize, w -> {
			w.write("ab");                 // partially fill
			w.write("LONG-PAYLOAD-LARGER"); // longer than buffer -> must drain "ab" first, then write payload directly
			w.write("cd");
		});
	}

	@Test
	void charArrayLargerThanBufferAfterPartialFill() throws IOException {
		final int bufferSize = 4;
		final char[] big = "LONG-PAYLOAD-LARGER".toCharArray();
		assertEquivalent(bufferSize, w -> {
			w.write("ab");
			w.write(big, 0, big.length);
			w.write("cd");
		});
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Randomized differential stress test
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void randomizedDifferentialAgainstBufferedWriter() throws IOException {
		final String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789 \t<>/\"=\n\u00e4\u00f6\u00fc\u20ac";
		for (int bufferSize : new int[] { 1, 2, 3, 5, 8, 13, 64, 1 << 16 }) {
			final long seed = 0xC0FFEE * (long) bufferSize + 17;
			assertEquivalent(bufferSize, w -> {
				final Random random = new Random(seed);
				for (int i = 0; i < 5000; i++) {
					final int choice = random.nextInt(6);
					switch (choice) {
						case 0:
							w.write(alphabet.charAt(random.nextInt(alphabet.length())));
							break;
						case 1:
							w.write(randomString(random, alphabet, random.nextInt(40)));
							break;
						case 2: {
							final String s = randomString(random, alphabet, 1 + random.nextInt(40));
							final int off = random.nextInt(s.length());
							final int len = random.nextInt(s.length() - off);
							w.write(s, off, len);
							break;
						}
						case 3: {
							final char[] c = randomString(random, alphabet, random.nextInt(40)).toCharArray();
							w.write(c);
							break;
						}
						case 4:
							w.append(randomString(random, alphabet, random.nextInt(40)));
							break;
						default:
							w.append(alphabet.charAt(random.nextInt(alphabet.length())));
							break;
					}
				}
			});
		}
	}

	private static String randomString(final Random random, final String alphabet, final int length) {
		final StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return builder.toString();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Batching, flush and close behaviour
	// ---------------------------------------------------------------------------------------------------------------

	@Test
	void batchesManySmallWritesIntoFewUnderlyingWrites() throws IOException {
		final CountingWriter underlying = new CountingWriter();
		final FastBufferedWriter writer = new FastBufferedWriter(underlying, 1024);

		for (int i = 0; i < 10000; i++) {
			writer.write("x"); // 10000 tiny writes, total 10000 chars
		}
		writer.flush();

		// 10000 chars through a 1024-char buffer: about 10 drains plus the flush, never one-per-write.
		assertTrue(underlying.writeCalls <= 12,
				"expected the buffer to coalesce small writes, but underlying received " + underlying.writeCalls + " writes");
		assertEquals(10000, underlying.charsWritten);
	}

	@Test
	void nothingReachesUnderlyingBeforeBufferFillsOrFlush() throws IOException {
		final CountingWriter underlying = new CountingWriter();
		final FastBufferedWriter writer = new FastBufferedWriter(underlying, 64);

		writer.write("small");
		assertEquals(0, underlying.charsWritten, "data must stay buffered until the buffer fills or flush is called");

		writer.flush();
		assertEquals(5, underlying.charsWritten);
	}

	@Test
	void flushDrainsBufferAndFlushesUnderlying() throws IOException {
		final CountingWriter underlying = new CountingWriter();
		final FastBufferedWriter writer = new FastBufferedWriter(underlying, 64);

		writer.write("payload");
		writer.flush();

		assertEquals("payload", underlying.builder.toString());
		assertTrue(underlying.flushed, "flush() must propagate to the underlying writer");
	}

	@Test
	void closeDrainsBufferAndClosesUnderlying() throws IOException {
		final CountingWriter underlying = new CountingWriter();
		final FastBufferedWriter writer = new FastBufferedWriter(underlying, 64);

		writer.write("to-be-flushed-on-close");
		writer.close();

		assertEquals("to-be-flushed-on-close", underlying.builder.toString(),
				"close() must drain remaining buffered data");
		assertTrue(underlying.closed, "close() must propagate to the underlying writer");
	}

	@Test
	void closeStillClosesUnderlyingWhenDrainFails() {
		final ThrowOnWriteWriter underlying = new ThrowOnWriteWriter();
		final FastBufferedWriter writer = new FastBufferedWriter(underlying, 64);

		try {
			writer.write("data that will fail to drain");
			writer.close();
		} catch (IOException expected) {
			// the drain inside close() is expected to throw
		}
		assertTrue(underlying.closed, "underlying writer must be closed even if draining fails");
	}

	@Test
	void appendReturnsSelfForChaining() throws IOException {
		final FastBufferedWriter writer = new FastBufferedWriter(new StringWriter(), 64);
		assertSame(writer, writer.append('a'));
		assertSame(writer, writer.append("b"));
		assertSame(writer, writer.append("cd", 0, 1));
		writer.close();
	}

	@Test
	void emptyOutputProducesEmptyFile() throws IOException {
		final CountingWriter underlying = new CountingWriter();
		final FastBufferedWriter writer = new FastBufferedWriter(underlying, 64);
		writer.flush();
		writer.close();
		assertEquals("", underlying.builder.toString());
		assertEquals(0, underlying.charsWritten);
		assertFalse(underlying.writeCalls > 0, "no characters were written, so the underlying writer must not be written to");
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Test doubles
	// ---------------------------------------------------------------------------------------------------------------

	/** A Writer that records what it receives, so batching, flush and close propagation can be asserted. */
	private static final class CountingWriter extends Writer {
		private final StringBuilder builder = new StringBuilder();
		private int writeCalls;
		private int charsWritten;
		private boolean flushed;
		private boolean closed;

		@Override
		public void write(final char[] cbuf, final int off, final int len) {
			this.writeCalls++;
			this.charsWritten += len;
			this.builder.append(cbuf, off, len);
		}

		@Override
		public void write(final String str, final int off, final int len) {
			this.writeCalls++;
			this.charsWritten += len;
			this.builder.append(str, off, off + len);
		}

		@Override
		public void flush() {
			this.flushed = true;
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}

	/** A Writer whose writes always fail, used to verify close() still closes the underlying writer. */
	private static final class ThrowOnWriteWriter extends Writer {
		private boolean closed;

		@Override
		public void write(final char[] cbuf, final int off, final int len) throws IOException {
			throw new IOException("write failure for test");
		}

		@Override
		public void write(final String str, final int off, final int len) throws IOException {
			throw new IOException("write failure for test");
		}

		@Override
		public void flush() throws IOException {
			throw new IOException("flush failure for test");
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}
}
