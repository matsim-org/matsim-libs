/* *********************************************************************** *
 * project: org.matsim.*
 * FastBufferedWriterBenchmark.java
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * A dependency-free A/B microbenchmark comparing {@link FastBufferedWriter} against {@link java.io.BufferedWriter} on a
 * workload that mirrors how MATSim's XML writers serialize a scenario: a great many tiny {@code write(String)} calls,
 * draining into a real character-encoding {@link OutputStreamWriter}. The downstream stream discards its bytes so that
 * only the writer/encoding cost (not disk or compression) is measured.
 *
 * <p>This is not a unit test and is skipped by default. Run it explicitly with:</p>
 * <pre>
 * mvn -o -pl matsim test -Dtest=FastBufferedWriterBenchmark \
 *     -Dsurefire.failIfNoSpecifiedTests=false -Dmatsim.benchmark=true
 * </pre>
 *
 * @author Hannes Rewald
 */
class FastBufferedWriterBenchmark {

	/** Number of "elements" written; each element issues {@link #WRITES_PER_ELEMENT} tiny writes. */
	private static final int ELEMENTS = 2_000_000;

	/** Tiny writes per element, mimicking how a person/household is emitted as dozens of small fragments. */
	private static final int WRITES_PER_ELEMENT = 25;

	private static final int WARMUP_ROUNDS = 3;
	private static final int MEASURED_ROUNDS = 5;

	/** Representative small XML fragments, like attribute names/values and tags emitted per element. */
	private static final String[] FRAGMENTS = {
			"\t\t<person id=\"", "12345678", "\" sex=\"m\" age=\"", "42", "\">",
			"\n\t\t\t<attributes>", "\n\t\t\t\t<attribute name=\"", "homeX", "\" class=\"java.lang.Double\">",
			"678910.123", "</attribute>", "\n\t\t\t</attributes>", "\n\t\t\t<plan selected=\"yes\">",
			"\n\t\t\t\t<activity type=\"", "home", "\" x=\"", "678910.1", "\" y=\"", "245678.9",
			"\" end_time=\"", "08:00:00", "\" />", "\n\t\t\t</plan>", "\n\t\t</person>", "\n"
	};

	@Test
	void benchmarkTinyWrites() throws IOException {
		Assumptions.assumeTrue(Boolean.getBoolean("matsim.benchmark"),
				"microbenchmark - run with -Dmatsim.benchmark=true to enable");

		System.out.println();
		System.out.println("FastBufferedWriter A/B benchmark");
		System.out.printf("  workload: %,d elements x %d tiny write(String) calls = %,d writes per round%n",
				ELEMENTS, WRITES_PER_ELEMENT, (long) ELEMENTS * WRITES_PER_ELEMENT);
		System.out.printf("  warmup rounds: %d, measured rounds: %d%n", WARMUP_ROUNDS, MEASURED_ROUNDS);
		System.out.println();

		// BEFORE: the JDK's synchronized BufferedWriter (8 KB default buffer).
		final Function<Writer, Writer> baseline = out -> new java.io.BufferedWriter(out, 8192);
		// AFTER: the unsynchronized FastBufferedWriter (64 KB buffer).
		final Function<Writer, Writer> candidate = out -> new FastBufferedWriter(out);

		final double baselineMs = measure("java.io.BufferedWriter (before)", baseline);
		final double candidateMs = measure("FastBufferedWriter      (after)", candidate);

		System.out.println();
		System.out.printf("  before: %8.1f ms/round (median)%n", baselineMs);
		System.out.printf("  after:  %8.1f ms/round (median)%n", candidateMs);
		System.out.printf("  speedup: %.2fx  (%.1f%% less time)%n",
				baselineMs / candidateMs, 100.0 * (baselineMs - candidateMs) / baselineMs);
		System.out.println();
	}

	private static double measure(final String label, final Function<Writer, Writer> wrapperFactory) throws IOException {
		final double[] timings = new double[MEASURED_ROUNDS];

		for (int round = 0; round < WARMUP_ROUNDS + MEASURED_ROUNDS; round++) {
			final long start = System.nanoTime();
			runWorkload(wrapperFactory);
			final long elapsedNs = System.nanoTime() - start;

			if (round >= WARMUP_ROUNDS) {
				timings[round - WARMUP_ROUNDS] = elapsedNs / 1_000_000.0;
			}
		}

		Arrays.sort(timings);
		final double median = timings[timings.length / 2];
		System.out.printf("  %-34s median %8.1f ms  (rounds %s ms)%n", label, median, Arrays.toString(timings));
		return median;
	}

	private static void runWorkload(final Function<Writer, Writer> wrapperFactory) throws IOException {
		// A real encoding writer onto a discarding stream: same StreamEncoder path as production, no disk/compression.
		final Writer encoding = new OutputStreamWriter(OutputStream.nullOutputStream(), StandardCharsets.UTF_8);
		final Writer writer = wrapperFactory.apply(encoding);
		try {
			for (int element = 0; element < ELEMENTS; element++) {
				for (int w = 0; w < WRITES_PER_ELEMENT; w++) {
					writer.write(FRAGMENTS[w]);
				}
			}
		} finally {
			writer.close();
		}
	}
}
