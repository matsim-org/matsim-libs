/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;

/**
 * Timing and quality result for a benchmark run.
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
	List<Long> individualTimes,
	DrtQualityStats qualityStats
) {
	private static final Logger LOG = LogManager.getLogger(BenchmarkResult.class);

	/**
	 * DRT quality statistics from the simulation output.
	 */
	public record DrtQualityStats(
		double rejectionRate,
		int rejections,
		int rides,
		double waitAverage,
		double waitP95,
		double inVehicleTravelTimeMean,
		double totalTravelTimeMean
	) {
		public static DrtQualityStats EMPTY = new DrtQualityStats(Double.NaN, 0, 0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

		public static DrtQualityStats fromOutputDirectory(String outputDir, String mode) {
			Path statsPath = Path.of(outputDir, "drt_customer_stats_" + mode + ".csv");
			if (!Files.exists(statsPath)) {
				LOG.warn("DRT stats file not found: {}", statsPath);
				return EMPTY;
			}

			try {
				List<String> lines = Files.readAllLines(statsPath);
				if (lines.size() < 2) {
					return EMPTY;
				}

				// Auto-detect delimiter (semicolon is MATSim default, but could be comma)
				String headerLine = lines.get(0);
				String delimiter = headerLine.contains(";") ? ";" : ",";

				String[] headers = headerLine.split(delimiter);
				String[] values = lines.get(lines.size() - 1).split(delimiter);

				Map<String, String> params = new HashMap<>();
				for (int i = 0; i < headers.length && i < values.length; i++) {
					params.put(headers[i].trim(), values[i].trim());
				}

				LOG.info("Parsed DRT stats from {}: rejectionRate={}, rejections={}, rides={}",
					statsPath, params.get("rejectionRate"), params.get("rejections"), params.get("rides"));

				return new DrtQualityStats(
					parseDouble(params.get("rejectionRate")),
					(int) parseDouble(params.get("rejections")),
					(int) parseDouble(params.get("rides")),
					parseDouble(params.get("wait_average")),
					parseDouble(params.get("wait_p95")),
					parseDouble(params.get("inVehicleTravelTime_mean")),
					parseDouble(params.get("totalTravelTime_mean"))
				);
			} catch (IOException e) {
				LOG.error("Failed to read DRT stats from {}", statsPath, e);
				return EMPTY;
			}
		}

		private static double parseDouble(String value) {
			if (value == null || value.isEmpty() || value.equalsIgnoreCase("NaN")) {
				return Double.NaN;
			}
			// Handle localized number formats (e.g., German uses comma as decimal separator)
			String normalized = value.replace(",", ".");
			try {
				return Double.parseDouble(normalized);
			} catch (NumberFormatException e) {
				LOG.warn("Failed to parse double value: '{}', returning NaN", value);
				return Double.NaN;
			}
		}

		public String summary() {
			return String.format("rides=%d, rejections=%d (%.1f%%), waitAvg=%.0fs, waitP95=%.0fs",
				rides, rejections, rejectionRate * 100, waitAverage, waitP95);
		}
	}

	public static BenchmarkResult run(String name, int warmupRuns, int measuredRuns,
									  BenchmarkScenario scenario) {
		LOG.info("Benchmark: {}", name);

		long warmupStart = System.currentTimeMillis();
		for (int i = 0; i < warmupRuns; i++) {
			LOG.info("  Warmup {}/{}", i + 1, warmupRuns);
			scenario.run();
			System.gc();
		}
		long warmupTime = System.currentTimeMillis() - warmupStart;

		List<Long> times = new ArrayList<>(measuredRuns);
		long totalStart = System.currentTimeMillis();
		String lastOutputDir = null;
		for (int i = 0; i < measuredRuns; i++) {
			LOG.info("  Run {}/{}", i + 1, measuredRuns);
			Instant start = Instant.now();
			lastOutputDir = scenario.run();
			times.add(Duration.between(start, Instant.now()).toMillis());
			System.gc();
		}
		long totalTime = System.currentTimeMillis() - totalStart;

		LongSummaryStatistics stats = times.stream().mapToLong(Long::longValue).summaryStatistics();
		double avg = stats.getAverage();
		double stdDev = Math.sqrt(times.stream().mapToDouble(t -> Math.pow(t - avg, 2)).average().orElse(0.0));

		// Read DRT quality stats from the last run
		DrtQualityStats qualityStats = lastOutputDir != null
			? DrtQualityStats.fromOutputDirectory(lastOutputDir, "drt")
			: DrtQualityStats.EMPTY;

		return new BenchmarkResult(name, measuredRuns, warmupTime, totalTime,
			stats.getMin(), stats.getMax(), avg, stdDev, times, qualityStats);
	}

	public String summary() {
		String timeSummary = String.format("%s: avg=%.0f ms, min=%d ms, max=%d ms (n=%d)",
			name, avgTimeMs, minTimeMs, maxTimeMs, iterations);
		if (qualityStats != null && !Double.isNaN(qualityStats.rejectionRate())) {
			return timeSummary + " | " + qualityStats.summary();
		}
		return timeSummary;
	}

	/**
	 * Functional interface for benchmark scenarios that return their output directory.
	 */
	@FunctionalInterface
	public interface BenchmarkScenario {
		/**
		 * Runs the scenario and returns the output directory path.
		 */
		String run();
	}
}
