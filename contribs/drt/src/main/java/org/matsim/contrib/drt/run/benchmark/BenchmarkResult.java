/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.run.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;

/**
 * Timing result for a benchmark run.
 *
 * @author Steffen Axer
 */
public record BenchmarkResult(
	String name,
	int iterations,
	long warmupTimeMs,
	long totalTimeMs,
	long minTimeMs,
	long maxTimeMs,
	double avgTimeMs,
	double stdDevMs,
	List<Long> individualTimes
) {
	private static final Logger LOG = LogManager.getLogger(BenchmarkResult.class);

	public static BenchmarkResult run(String name, int warmupRuns, int measuredRuns, Runnable runnable) {
		LOG.info("Benchmark: {}", name);

		long warmupStart = System.currentTimeMillis();
		for (int i = 0; i < warmupRuns; i++) {
			LOG.info("  Warmup {}/{}", i + 1, warmupRuns);
			runnable.run();
			System.gc();
		}
		long warmupTime = System.currentTimeMillis() - warmupStart;

		List<Long> times = new ArrayList<>(measuredRuns);
		long totalStart = System.currentTimeMillis();
		for (int i = 0; i < measuredRuns; i++) {
			LOG.info("  Run {}/{}", i + 1, measuredRuns);
			Instant start = Instant.now();
			runnable.run();
			times.add(Duration.between(start, Instant.now()).toMillis());
			System.gc();
		}
		long totalTime = System.currentTimeMillis() - totalStart;

		LongSummaryStatistics stats = times.stream().mapToLong(Long::longValue).summaryStatistics();
		double avg = stats.getAverage();
		double stdDev = Math.sqrt(times.stream().mapToDouble(t -> Math.pow(t - avg, 2)).average().orElse(0.0));

		return new BenchmarkResult(name, measuredRuns, warmupTime, totalTime,
			stats.getMin(), stats.getMax(), avg, stdDev, times);
	}

	public String summary() {
		return String.format("%s: avg=%.0f ms, min=%d ms, max=%d ms (n=%d)", name, avgTimeMs, minTimeMs, maxTimeMs, iterations);
	}
}
