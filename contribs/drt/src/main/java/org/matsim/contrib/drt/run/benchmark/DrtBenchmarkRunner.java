/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.run.benchmark;

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
 * Runner for executing DRT benchmarks.
 * <p>
 * Example:
 * <pre>
 * DrtBenchmarkRunner.create()
 *     .addScenario("Default", () -> defaultControler.run())
 *     .addScenario("Parallel", () -> parallelControler.run())
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

	private final List<BenchmarkScenario> scenarios = new ArrayList<>();
	private int warmupRuns = 1;
	private int measuredRuns = 3;
	private Path reportPath;

	public static DrtBenchmarkRunner create() {
		return new DrtBenchmarkRunner();
	}

	public DrtBenchmarkRunner addScenario(String name, Runnable runnable) {
		scenarios.add(new BenchmarkScenario(name, runnable));
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

		List<BenchmarkResult> results = new ArrayList<>();

		for (int i = 0; i < scenarios.size(); i++) {
			BenchmarkScenario scenario = scenarios.get(i);
			LOG.info("\n[{}/{}] {}", i + 1, scenarios.size(), scenario.name);

			BenchmarkResult result = BenchmarkResult.run(scenario.name, warmupRuns, measuredRuns, scenario.runnable);
			results.add(result);
			LOG.info("  Result: {}", result.summary());
		}

		printSummary(results);
		if (reportPath != null) {
			writeReport(results);
		}

		return results;
	}

	private void printSummary(List<BenchmarkResult> results) {
		LOG.info("\n=== SUMMARY ===");
		results.forEach(r -> LOG.info("  {}", r.summary()));

		if (results.size() > 1) {
			BenchmarkResult fastest = results.stream()
				.min(Comparator.comparingDouble(BenchmarkResult::avgTimeMs))
				.orElseThrow();
			LOG.info("Fastest: {}", fastest.name());
		}
	}

	private void writeReport(List<BenchmarkResult> results) {
		try {
			Files.createDirectories(reportPath.getParent());
			try (BufferedWriter w = Files.newBufferedWriter(reportPath)) {
				w.write("name,iterations,min_ms,max_ms,avg_ms,stddev_ms\n");
				for (BenchmarkResult r : results) {
					w.write(String.format("%s,%d,%d,%d,%.0f,%.0f%n",
						r.name(), r.iterations(), r.minTimeMs(), r.maxTimeMs(), r.avgTimeMs(), r.stdDevMs()));
				}
			}
			LOG.info("Report: {}", reportPath.toAbsolutePath());
		} catch (IOException e) {
			LOG.error("Failed to write report", e);
		}
	}

	private record BenchmarkScenario(String name, Runnable runnable) {}
}
