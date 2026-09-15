/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runner for executing DRT benchmarks with timing and quality metrics.
 * <p>
 * Example:
 * <pre>
 * DrtBenchmarkRunner.create()
 *     .addScenario("Default", () -> { controler.run(); return outputDir; })
 *     .addScenario("Parallel", () -> { parallelControler.run(); return outputDir; })
 *     .warmupRuns(1)
 *     .measuredRuns(3)
 *     .reportTo(Path.of("results.csv"))
 *     .run();
 * </pre>
 *
 * @author Steffen Axer
 */
public class DrtBenchmarkRunner {
	private static final Logger LOG = LogManager.getLogger(DrtBenchmarkRunner.class);

	private static final String CSV_HEADER = "name,iterations,min_ms,max_ms,avg_ms,stddev_ms," +
		"rides,rejections,rejection_rate,wait_avg_s,wait_p95_s,in_vehicle_time_s,total_travel_time_s";

	private final List<NamedScenario> scenarios = new ArrayList<>();
	private int warmupRuns = 1;
	private int measuredRuns = 3;
	private Path reportPath;
	private boolean headerWritten = false;

	public static DrtBenchmarkRunner create() {
		return new DrtBenchmarkRunner();
	}

	/**
	 * Adds a scenario that returns its output directory for quality metrics extraction.
	 */
	public DrtBenchmarkRunner addScenario(String name, BenchmarkResult.BenchmarkScenario scenario) {
		scenarios.add(new NamedScenario(name, scenario));
		return this;
	}

	public DrtBenchmarkRunner warmupRuns(int n) {
		this.warmupRuns = n;
		return this;
	}

	public DrtBenchmarkRunner measuredRuns(int n) {
		this.measuredRuns = n;
		return this;
	}

	public DrtBenchmarkRunner reportTo(Path path) {
		this.reportPath = path;
		return this;
	}

	public List<BenchmarkResult> run() {
		LOG.info("=== DRT Benchmark Suite ===");
		LOG.info("Scenarios: {}, Warmup: {}, Measured: {}", scenarios.size(), warmupRuns, measuredRuns);

		// Initialize report file with header
		if (reportPath != null) {
			initializeReportFile();
		}

		List<BenchmarkResult> results = new ArrayList<>();

		for (int i = 0; i < scenarios.size(); i++) {
			NamedScenario scenario = scenarios.get(i);
			LOG.info("\n[{}/{}] {}", i + 1, scenarios.size(), scenario.name);

			BenchmarkResult result = BenchmarkResult.run(scenario.name, warmupRuns, measuredRuns, scenario.scenario);
			results.add(result);
			LOG.info("  Result: {}", result.summary());

			// Write result immediately after each scenario
			if (reportPath != null) {
				appendResultToReport(result);
			}
		}

		printSummary(results);
		LOG.info("Report written to: {}", reportPath != null ? reportPath.toAbsolutePath() : "N/A");

		return results;
	}

	private void initializeReportFile() {
		try {
			Files.createDirectories(reportPath.getParent());
			try (BufferedWriter w = Files.newBufferedWriter(reportPath)) {
				w.write(CSV_HEADER);
				w.newLine();
			}
			headerWritten = true;
			LOG.info("Initialized report file: {}", reportPath.toAbsolutePath());
		} catch (IOException e) {
			LOG.error("Failed to initialize report file", e);
		}
	}

	private void appendResultToReport(BenchmarkResult r) {
		try (BufferedWriter w = Files.newBufferedWriter(reportPath,
				java.nio.file.StandardOpenOption.APPEND)) {
			var q = r.qualityStats() != null ? r.qualityStats() : BenchmarkResult.DrtQualityStats.EMPTY;
			w.write(String.format(java.util.Locale.US, "%s,%d,%d,%d,%.0f,%.0f,%d,%d,%.4f,%.0f,%.0f,%.0f,%.0f",
				r.name(), r.iterations(), r.minTimeMs(), r.maxTimeMs(), r.avgTimeMs(), r.stdDevMs(),
				q.rides(), q.rejections(), q.rejectionRate(),
				q.waitAverage(), q.waitP95(), q.inVehicleTravelTimeMean(), q.totalTravelTimeMean()));
			w.newLine();
			LOG.info("  Written to report: {} | rejection_rate={}", r.name(), q.rejectionRate());
		} catch (IOException e) {
			LOG.error("Failed to append result to report", e);
		}
	}

	private void printSummary(List<BenchmarkResult> results) {
		LOG.info("\n=== SUMMARY ===");
		results.forEach(r -> LOG.info("  {}", r.summary()));

		if (results.size() > 1) {
			BenchmarkResult fastest = results.stream()
				.min(Comparator.comparingDouble(BenchmarkResult::avgTimeMs))
				.orElseThrow();
			LOG.info("Fastest: {}", fastest.name());

			// Also report best quality (lowest rejection rate)
			results.stream()
				.filter(r -> r.qualityStats() != null && !Double.isNaN(r.qualityStats().rejectionRate()))
				.min(Comparator.comparingDouble(r -> r.qualityStats().rejectionRate()))
				.ifPresent(best -> LOG.info("Lowest rejection rate: {} ({}%)",
					best.name(), String.format("%.2f", best.qualityStats().rejectionRate() * 100)));
		}
	}

	private record NamedScenario(String name, BenchmarkResult.BenchmarkScenario scenario) {}
}
