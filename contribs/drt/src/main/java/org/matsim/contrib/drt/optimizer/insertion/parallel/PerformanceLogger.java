/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.parallel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.utils.io.IOUtils;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handles comprehensive performance logging for the parallel inserter.
 * <p>
 * Captures detailed metrics including:
 * <ul>
 *   <li>Worker utilization and processing times per partition</li>
 *   <li>Conflict resolution timing and rates</li>
 *   <li>Scheduling phase timing</li>
 *   <li>Iteration statistics within processing rounds</li>
 *   <li>Active vs. inactive partition distribution</li>
 * </ul>
 *
 * @author Steffen Axer
 */
class PerformanceLogger {
	private static final Logger LOG = LogManager.getLogger(PerformanceLogger.class);

	private final List<ProcessingCycleRecord> cycleRecords = new ArrayList<>();
	private final MatsimServices matsimServices;
	private final String mode;
	private final int maxPartitions;
	private final boolean enabled;

	/**
	 * Detailed record of a single processing cycle (one call to processRequests).
	 */
	record ProcessingCycleRecord(
		double simTime,
		int totalRequests,
		int totalVehicles,
		int activePartitions,
		int maxPartitions,
		List<IterationRecord> iterations,
		long vehicleEntryCalcTimeNanos,
		long totalCycleTimeNanos,
		int totalScheduled,
		int totalRejected
	) {
		double vehicleEntryCalcTimeMs() {
			return vehicleEntryCalcTimeNanos / 1_000_000.0;
		}

		double totalCycleTimeMs() {
			return totalCycleTimeNanos / 1_000_000.0;
		}

		double getPartitionUtilization() {
			return maxPartitions > 0 ? (double) activePartitions / maxPartitions : 0.0;
		}

		/**
		 * @return Throughput in requests per second (scheduled requests / cycle time)
		 */
		double getThroughputRequestsPerSecond() {
			double cycleTimeSeconds = totalCycleTimeNanos / 1_000_000_000.0;
			return cycleTimeSeconds > 0 ? totalScheduled / cycleTimeSeconds : 0.0;
		}

		/**
		 * @return Processing time per request in milliseconds
		 */
		double getTimePerRequestMs() {
			return totalScheduled > 0 ? totalCycleTimeMs() / totalScheduled : 0.0;
		}
	}

	/**
	 * Record of a single iteration within a processing cycle.
	 */
	record IterationRecord(
		int iterationNumber,
		long solveTimeNanos,
		long conflictResolutionTimeNanos,
		long schedulingTimeNanos,
		int[] requestsPerWorker,
		int[] vehiclesPerWorker,
		long[] workerProcessingTimesNanos,
		int conflicts,
		int noSolutions,
		int scheduled
	) {
		double solveTimeMs() {
			return solveTimeNanos / 1_000_000.0;
		}

		double conflictResolutionTimeMs() {
			return conflictResolutionTimeNanos / 1_000_000.0;
		}

		double schedulingTimeMs() {
			return schedulingTimeNanos / 1_000_000.0;
		}

		double[] workerProcessingTimesMs() {
			double[] result = new double[workerProcessingTimesNanos.length];
			for (int i = 0; i < workerProcessingTimesNanos.length; i++) {
				result[i] = workerProcessingTimesNanos[i] / 1_000_000.0;
			}
			return result;
		}

		double maxWorkerTimeMs() {
			long max = 0;
			for (long time : workerProcessingTimesNanos) {
				max = Math.max(max, time);
			}
			return max / 1_000_000.0;
		}

		double minWorkerTimeMs() {
			long min = Long.MAX_VALUE;
			for (long time : workerProcessingTimesNanos) {
				if (time > 0) {
					min = Math.min(min, time);
				}
			}
			return min == Long.MAX_VALUE ? 0 : min / 1_000_000.0;
		}

		double avgWorkerTimeMs() {
			int count = 0;
			long sum = 0;
			for (long time : workerProcessingTimesNanos) {
				if (time > 0) {
					sum += time;
					count++;
				}
			}
			return count > 0 ? (sum / 1_000_000.0) / count : 0;
		}

		/**
		 * Load imbalance ratio: max/avg worker time.
		 * A ratio of 1.0 means perfectly balanced, higher means imbalanced.
		 */
		double loadImbalanceRatio() {
			double avg = avgWorkerTimeMs();
			return avg > 0 ? maxWorkerTimeMs() / avg : 1.0;
		}

		int activeWorkerCount() {
			int count = 0;
			for (long time : workerProcessingTimesNanos) {
				if (time > 0) count++;
			}
			return count;
		}
	}

	/**
	 * Builder for constructing IterationRecord with timing measurements.
	 */
	static class IterationRecordBuilder {
		private int iterationNumber;
		private long solveStartNanos;
		private long solveTimeNanos;
		private long conflictResolutionStartNanos;
		private long conflictResolutionTimeNanos;
		private long schedulingStartNanos;
		private long schedulingTimeNanos;
		private int[] requestsPerWorker;
		private int[] vehiclesPerWorker;
		private long[] workerProcessingTimesNanos;
		private int conflicts;
		private int noSolutions;
		private int scheduled;

		IterationRecordBuilder iterationNumber(int n) {
			this.iterationNumber = n;
			return this;
		}

		IterationRecordBuilder startSolve() {
			this.solveStartNanos = System.nanoTime();
			return this;
		}

		IterationRecordBuilder endSolve() {
			this.solveTimeNanos = System.nanoTime() - solveStartNanos;
			return this;
		}

		IterationRecordBuilder startConflictResolution() {
			this.conflictResolutionStartNanos = System.nanoTime();
			return this;
		}

		IterationRecordBuilder endConflictResolution() {
			this.conflictResolutionTimeNanos = System.nanoTime() - conflictResolutionStartNanos;
			return this;
		}

		IterationRecordBuilder startScheduling() {
			this.schedulingStartNanos = System.nanoTime();
			return this;
		}

		IterationRecordBuilder endScheduling() {
			this.schedulingTimeNanos = System.nanoTime() - schedulingStartNanos;
			return this;
		}

		IterationRecordBuilder requestsPerWorker(int[] counts) {
			this.requestsPerWorker = counts;
			return this;
		}

		IterationRecordBuilder vehiclesPerWorker(int[] counts) {
			this.vehiclesPerWorker = counts;
			return this;
		}

		IterationRecordBuilder workerProcessingTimes(long[] times) {
			this.workerProcessingTimesNanos = times;
			return this;
		}

		IterationRecordBuilder conflicts(int n) {
			this.conflicts = n;
			return this;
		}

		IterationRecordBuilder noSolutions(int n) {
			this.noSolutions = n;
			return this;
		}

		IterationRecordBuilder scheduled(int n) {
			this.scheduled = n;
			return this;
		}

		IterationRecord build() {
			return new IterationRecord(
				iterationNumber,
				solveTimeNanos,
				conflictResolutionTimeNanos,
				schedulingTimeNanos,
				requestsPerWorker != null ? requestsPerWorker : new int[0],
				vehiclesPerWorker != null ? vehiclesPerWorker : new int[0],
				workerProcessingTimesNanos != null ? workerProcessingTimesNanos : new long[0],
				conflicts,
				noSolutions,
				scheduled
			);
		}
	}

	/**
	 * Builder for constructing ProcessingCycleRecord.
	 */
	static class CycleRecordBuilder {
		private double simTime;
		private int totalRequests;
		private int totalVehicles;
		private int activePartitions;
		private int maxPartitions;
		private final List<IterationRecord> iterations = new ArrayList<>();
		private long vehicleEntryCalcStartNanos;
		private long vehicleEntryCalcTimeNanos;
		private long cycleStartNanos;
		private long totalCycleTimeNanos;
		private int totalScheduled;
		private int totalRejected;

		CycleRecordBuilder simTime(double time) {
			this.simTime = time;
			return this;
		}

		CycleRecordBuilder totalRequests(int n) {
			this.totalRequests = n;
			return this;
		}

		CycleRecordBuilder totalVehicles(int n) {
			this.totalVehicles = n;
			return this;
		}

		CycleRecordBuilder activePartitions(int n) {
			this.activePartitions = n;
			return this;
		}

		CycleRecordBuilder maxPartitions(int n) {
			this.maxPartitions = n;
			return this;
		}

		CycleRecordBuilder addIteration(IterationRecord record) {
			this.iterations.add(record);
			return this;
		}

		CycleRecordBuilder startVehicleEntryCalc() {
			this.vehicleEntryCalcStartNanos = System.nanoTime();
			return this;
		}

		CycleRecordBuilder endVehicleEntryCalc() {
			this.vehicleEntryCalcTimeNanos = System.nanoTime() - vehicleEntryCalcStartNanos;
			return this;
		}

		CycleRecordBuilder startCycle() {
			this.cycleStartNanos = System.nanoTime();
			return this;
		}

		CycleRecordBuilder endCycle() {
			this.totalCycleTimeNanos = System.nanoTime() - cycleStartNanos;
			return this;
		}

		CycleRecordBuilder totalScheduled(int n) {
			this.totalScheduled = n;
			return this;
		}

		CycleRecordBuilder totalRejected(int n) {
			this.totalRejected = n;
			return this;
		}

		ProcessingCycleRecord build() {
			return new ProcessingCycleRecord(
				simTime,
				totalRequests,
				totalVehicles,
				activePartitions,
				maxPartitions,
				new ArrayList<>(iterations),
				vehicleEntryCalcTimeNanos,
				totalCycleTimeNanos,
				totalScheduled,
				totalRejected
			);
		}
	}

	PerformanceLogger(MatsimServices matsimServices, String mode, DrtParallelInserterParams params) {
		this.matsimServices = matsimServices;
		this.mode = mode;
		this.maxPartitions = params.getMaxPartitions();
		this.enabled = params.isLogPerformanceStats();
	}

	boolean isEnabled() {
		return enabled;
	}

	void recordCycle(ProcessingCycleRecord record) {
		if (!enabled) return;
		cycleRecords.add(record);
	}

	void writeOutputs() {
		if (!enabled || cycleRecords.isEmpty()) return;

		writeDetailedCsv();
		writeWorkerStatsCsv();
		writeSummaryCsv();
		writeTimingPlot();
		writeWorkerLoadPlot();
		writeStackedHourlyTimingPlot();

		logSummaryToConsole();
	}

	private void writeDetailedCsv() {
		String sep = matsimServices.getConfig().global().getDefaultDelimiter();
		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_parallelInserter_detailed.csv.gz"
		);

		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			// Header
			writer.write(String.join(sep,
				"simTime", "cycleTimeMs", "vehicleEntryCalcTimeMs",
				"totalRequests", "totalVehicles", "activePartitions", "maxPartitions", "partitionUtilization",
				"throughputReqPerSec", "timePerRequestMs",
				"iteration", "solveTimeMs", "conflictResolutionTimeMs", "schedulingTimeMs",
				"conflicts", "noSolutions", "scheduled",
				"activeWorkers", "minWorkerTimeMs", "maxWorkerTimeMs", "avgWorkerTimeMs", "loadImbalanceRatio"
			));
			writer.newLine();

			for (ProcessingCycleRecord cycle : cycleRecords) {
				for (IterationRecord iter : cycle.iterations) {
					writer.write(String.join(sep,
						String.valueOf(cycle.simTime),
						String.format("%.3f", cycle.totalCycleTimeMs()),
						String.format("%.3f", cycle.vehicleEntryCalcTimeMs()),
						String.valueOf(cycle.totalRequests),
						String.valueOf(cycle.totalVehicles),
						String.valueOf(cycle.activePartitions),
						String.valueOf(cycle.maxPartitions),
						String.format("%.3f", cycle.getPartitionUtilization()),
						String.format("%.1f", cycle.getThroughputRequestsPerSecond()),
						String.format("%.3f", cycle.getTimePerRequestMs()),
						String.valueOf(iter.iterationNumber),
						String.format("%.3f", iter.solveTimeMs()),
						String.format("%.3f", iter.conflictResolutionTimeMs()),
						String.format("%.3f", iter.schedulingTimeMs()),
						String.valueOf(iter.conflicts),
						String.valueOf(iter.noSolutions),
						String.valueOf(iter.scheduled),
						String.valueOf(iter.activeWorkerCount()),
						String.format("%.3f", iter.minWorkerTimeMs()),
						String.format("%.3f", iter.maxWorkerTimeMs()),
						String.format("%.3f", iter.avgWorkerTimeMs()),
						String.format("%.3f", iter.loadImbalanceRatio())
					));
					writer.newLine();
				}
			}
		} catch (IOException ex) {
			LOG.error("Failed to write detailed performance log", ex);
		}
	}

	private void writeWorkerStatsCsv() {
		String sep = matsimServices.getConfig().global().getDefaultDelimiter();
		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_parallelInserter_workerStats.csv.gz"
		);

		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			// Header
			StringBuilder header = new StringBuilder("simTime" + sep + "iteration");
			for (int i = 0; i < maxPartitions; i++) {
				header.append(sep).append("worker").append(i).append("_requests");
				header.append(sep).append("worker").append(i).append("_vehicles");
				header.append(sep).append("worker").append(i).append("_timeMs");
			}
			writer.write(header.toString());
			writer.newLine();

			for (ProcessingCycleRecord cycle : cycleRecords) {
				for (IterationRecord iter : cycle.iterations) {
					StringBuilder line = new StringBuilder();
					line.append(cycle.simTime).append(sep).append(iter.iterationNumber);

					for (int i = 0; i < maxPartitions; i++) {
						int requests = i < iter.requestsPerWorker.length ? iter.requestsPerWorker[i] : 0;
						int vehicles = i < iter.vehiclesPerWorker.length ? iter.vehiclesPerWorker[i] : 0;
						double timeMs = i < iter.workerProcessingTimesNanos.length ?
							iter.workerProcessingTimesNanos[i] / 1_000_000.0 : 0;

						line.append(sep).append(requests);
						line.append(sep).append(vehicles);
						line.append(sep).append(String.format("%.3f", timeMs));
					}

					writer.write(line.toString());
					writer.newLine();
				}
			}
		} catch (IOException ex) {
			LOG.error("Failed to write worker stats log", ex);
		}
	}

	private void writeSummaryCsv() {
		String sep = matsimServices.getConfig().global().getDefaultDelimiter();
		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_parallelInserter_summary.csv"
		);

		// Calculate aggregated statistics
		DoubleSummaryStatistics cycleTimeStats = cycleRecords.stream()
			.mapToDouble(ProcessingCycleRecord::totalCycleTimeMs)
			.summaryStatistics();

		DoubleSummaryStatistics solveTimeStats = cycleRecords.stream()
			.flatMap(c -> c.iterations.stream())
			.mapToDouble(IterationRecord::solveTimeMs)
			.summaryStatistics();

		DoubleSummaryStatistics conflictResolutionStats = cycleRecords.stream()
			.flatMap(c -> c.iterations.stream())
			.mapToDouble(IterationRecord::conflictResolutionTimeMs)
			.summaryStatistics();

		DoubleSummaryStatistics schedulingStats = cycleRecords.stream()
			.flatMap(c -> c.iterations.stream())
			.mapToDouble(IterationRecord::schedulingTimeMs)
			.summaryStatistics();

		DoubleSummaryStatistics loadImbalanceStats = cycleRecords.stream()
			.flatMap(c -> c.iterations.stream())
			.mapToDouble(IterationRecord::loadImbalanceRatio)
			.summaryStatistics();

		DoubleSummaryStatistics partitionUtilStats = cycleRecords.stream()
			.mapToDouble(ProcessingCycleRecord::getPartitionUtilization)
			.summaryStatistics();

		// Throughput statistics (only for cycles with scheduled requests)
		DoubleSummaryStatistics throughputStats = cycleRecords.stream()
			.filter(c -> c.totalScheduled > 0)
			.mapToDouble(ProcessingCycleRecord::getThroughputRequestsPerSecond)
			.summaryStatistics();

		DoubleSummaryStatistics timePerRequestStats = cycleRecords.stream()
			.filter(c -> c.totalScheduled > 0)
			.mapToDouble(ProcessingCycleRecord::getTimePerRequestMs)
			.summaryStatistics();

		long totalConflicts = cycleRecords.stream()
			.flatMap(c -> c.iterations.stream())
			.mapToLong(IterationRecord::conflicts)
			.sum();

		long totalScheduled = cycleRecords.stream()
			.mapToLong(ProcessingCycleRecord::totalScheduled)
			.sum();

		long totalRejected = cycleRecords.stream()
			.mapToLong(ProcessingCycleRecord::totalRejected)
			.sum();

		// Overall throughput: total scheduled / total processing time
		double totalProcessingTimeSeconds = cycleRecords.stream()
			.mapToDouble(c -> c.totalCycleTimeNanos / 1_000_000_000.0)
			.sum();
		double overallThroughput = totalProcessingTimeSeconds > 0 ? totalScheduled / totalProcessingTimeSeconds : 0;

		// Worker utilization per partition
		long[] totalRequestsPerWorker = new long[maxPartitions];
		long[] totalActiveCountPerWorker = new long[maxPartitions];

		for (ProcessingCycleRecord cycle : cycleRecords) {
			for (IterationRecord iter : cycle.iterations) {
				for (int i = 0; i < Math.min(maxPartitions, iter.requestsPerWorker.length); i++) {
					totalRequestsPerWorker[i] += iter.requestsPerWorker[i];
					if (iter.requestsPerWorker[i] > 0) {
						totalActiveCountPerWorker[i]++;
					}
				}
			}
		}

		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			writer.write("metric" + sep + "value");
			writer.newLine();

			writer.write("totalCycles" + sep + cycleRecords.size());
			writer.newLine();
			writer.write("maxPartitions" + sep + maxPartitions);
			writer.newLine();

			// Cycle time stats
			writer.write("cycleTime_avg_ms" + sep + String.format("%.3f", cycleTimeStats.getAverage()));
			writer.newLine();
			writer.write("cycleTime_min_ms" + sep + String.format("%.3f", cycleTimeStats.getMin()));
			writer.newLine();
			writer.write("cycleTime_max_ms" + sep + String.format("%.3f", cycleTimeStats.getMax()));
			writer.newLine();

			// Throughput stats
			writer.write("throughput_overall_reqPerSec" + sep + String.format("%.1f", overallThroughput));
			writer.newLine();
			writer.write("throughput_avg_reqPerSec" + sep + String.format("%.1f", throughputStats.getAverage()));
			writer.newLine();
			writer.write("throughput_min_reqPerSec" + sep + String.format("%.1f", throughputStats.getMin()));
			writer.newLine();
			writer.write("throughput_max_reqPerSec" + sep + String.format("%.1f", throughputStats.getMax()));
			writer.newLine();
			writer.write("timePerRequest_avg_ms" + sep + String.format("%.3f", timePerRequestStats.getAverage()));
			writer.newLine();
			writer.write("timePerRequest_min_ms" + sep + String.format("%.3f", timePerRequestStats.getMin()));
			writer.newLine();
			writer.write("timePerRequest_max_ms" + sep + String.format("%.3f", timePerRequestStats.getMax()));
			writer.newLine();

			// Solve time stats
			writer.write("solveTime_avg_ms" + sep + String.format("%.3f", solveTimeStats.getAverage()));
			writer.newLine();
			writer.write("solveTime_sum_ms" + sep + String.format("%.3f", solveTimeStats.getSum()));
			writer.newLine();

			// Conflict resolution stats
			writer.write("conflictResolution_avg_ms" + sep + String.format("%.3f", conflictResolutionStats.getAverage()));
			writer.newLine();
			writer.write("conflictResolution_sum_ms" + sep + String.format("%.3f", conflictResolutionStats.getSum()));
			writer.newLine();

			// Scheduling stats
			writer.write("scheduling_avg_ms" + sep + String.format("%.3f", schedulingStats.getAverage()));
			writer.newLine();
			writer.write("scheduling_sum_ms" + sep + String.format("%.3f", schedulingStats.getSum()));
			writer.newLine();

			// Load imbalance
			writer.write("loadImbalance_avg" + sep + String.format("%.3f", loadImbalanceStats.getAverage()));
			writer.newLine();
			writer.write("loadImbalance_max" + sep + String.format("%.3f", loadImbalanceStats.getMax()));
			writer.newLine();

			// Partition utilization
			writer.write("partitionUtilization_avg" + sep + String.format("%.3f", partitionUtilStats.getAverage()));
			writer.newLine();

			// Totals
			writer.write("totalConflicts" + sep + totalConflicts);
			writer.newLine();
			writer.write("totalScheduled" + sep + totalScheduled);
			writer.newLine();
			writer.write("totalRejected" + sep + totalRejected);
			writer.newLine();

			// Per-worker stats
			long totalIterations = cycleRecords.stream().mapToLong(c -> c.iterations.size()).sum();
			for (int i = 0; i < maxPartitions; i++) {
				writer.write("worker" + i + "_totalRequests" + sep + totalRequestsPerWorker[i]);
				writer.newLine();
				double activeRate = totalIterations > 0 ? (double) totalActiveCountPerWorker[i] / totalIterations : 0;
				writer.write("worker" + i + "_activeRate" + sep + String.format("%.3f", activeRate));
				writer.newLine();
			}

		} catch (IOException ex) {
			LOG.error("Failed to write summary log", ex);
		}
	}

	private void writeTimingPlot() {
		XYSeries cycleTimeSeries = new XYSeries("Cycle Time (ms)");
		XYSeries solveTimeSeries = new XYSeries("Solve Time (ms)");
		XYSeries conflictTimeSeries = new XYSeries("Conflict Resolution (ms)");
		XYSeries schedulingTimeSeries = new XYSeries("Scheduling (ms)");

		for (ProcessingCycleRecord cycle : cycleRecords) {
			cycleTimeSeries.add(cycle.simTime, cycle.totalCycleTimeMs());

			double totalSolve = 0, totalConflict = 0, totalScheduling = 0;
			for (IterationRecord iter : cycle.iterations) {
				totalSolve += iter.solveTimeMs();
				totalConflict += iter.conflictResolutionTimeMs();
				totalScheduling += iter.schedulingTimeMs();
			}
			solveTimeSeries.add(cycle.simTime, totalSolve);
			conflictTimeSeries.add(cycle.simTime, totalConflict);
			schedulingTimeSeries.add(cycle.simTime, totalScheduling);
		}

		XYSeriesCollection timingDataset = new XYSeriesCollection();
		timingDataset.addSeries(cycleTimeSeries);
		timingDataset.addSeries(solveTimeSeries);
		timingDataset.addSeries(conflictTimeSeries);
		timingDataset.addSeries(schedulingTimeSeries);

		NumberAxis timeAxis = new NumberAxis("Time [s]");
		NumberAxis timingAxis = new NumberAxis("Duration [ms]");

		XYPlot timingPlot = new XYPlot(timingDataset, timeAxis, timingAxis, new XYLineAndShapeRenderer(true, false));

		JFreeChart chart = new JFreeChart("Parallel Inserter Timing", JFreeChart.DEFAULT_TITLE_FONT, timingPlot, true);

		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_parallelInserter_timing.png"
		);

		try {
			ChartUtils.saveChartAsPNG(new File(filename), chart, 1200, 600);
		} catch (IOException e) {
			LOG.error("Failed to write timing chart", e);
		}
	}

	private void writeWorkerLoadPlot() {
		// Create box-and-whisker data for worker load distribution
		DefaultBoxAndWhiskerCategoryDataset workerTimeDataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (int workerIdx = 0; workerIdx < maxPartitions; workerIdx++) {
			List<Double> workerTimes = new ArrayList<>();
			for (ProcessingCycleRecord cycle : cycleRecords) {
				for (IterationRecord iter : cycle.iterations) {
					if (workerIdx < iter.workerProcessingTimesNanos.length) {
						double timeMs = iter.workerProcessingTimesNanos[workerIdx] / 1_000_000.0;
						if (timeMs > 0) {
							workerTimes.add(timeMs);
						}
					}
				}
			}
			if (!workerTimes.isEmpty()) {
				workerTimeDataset.add(workerTimes, "Processing Time", "Worker " + workerIdx);
			}
		}

		CategoryAxis xAxis = new CategoryAxis("Worker");
		NumberAxis yAxis = new NumberAxis("Processing Time [ms]");

		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(true);
		renderer.setMeanVisible(true);

		CategoryPlot plot = new CategoryPlot(workerTimeDataset, xAxis, yAxis, renderer);

		JFreeChart chart = new JFreeChart("Worker Load Distribution", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_parallelInserter_workerLoad.png"
		);

		try {
			ChartUtils.saveChartAsPNG(new File(filename), chart, 800, 600);
		} catch (IOException e) {
			LOG.error("Failed to write worker load chart", e);
		}
	}

	/**
	 * Creates a stacked area chart showing the time breakdown per simulation hour.
	 * Shows how VehicleEntryCalc, Solve, ConflictResolution, and Scheduling times
	 * evolve over the simulation period.
	 */
	private void writeStackedHourlyTimingPlot() {
		// Aggregate data per simulation hour
		Map<Integer, HourlyAggregation> hourlyData = new TreeMap<>();

		for (ProcessingCycleRecord cycle : cycleRecords) {
			int hour = (int) (cycle.simTime / 3600);

			HourlyAggregation agg = hourlyData.computeIfAbsent(hour, k -> new HourlyAggregation());
			agg.vehicleEntryCalcMs += cycle.vehicleEntryCalcTimeMs();

			for (IterationRecord iter : cycle.iterations) {
				agg.solveMs += iter.solveTimeMs();
				agg.conflictResolutionMs += iter.conflictResolutionTimeMs();
				agg.schedulingMs += iter.schedulingTimeMs();
			}

			agg.totalRequests += cycle.totalScheduled;
			agg.cycleCount++;
		}

		// Create dataset for stacked area chart
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		// Define series names
		String vehicleEntrySeries = "Vehicle Entry Calc";
		String solveSeries = "Solve (Parallel)";
		String conflictSeries = "Conflict Resolution";
		String schedulingSeries = "Scheduling";

		for (Map.Entry<Integer, HourlyAggregation> entry : hourlyData.entrySet()) {
			String hourLabel = String.format("%02d:00", entry.getKey());
			HourlyAggregation agg = entry.getValue();

			// Add values in stacking order (bottom to top)
			dataset.addValue(agg.vehicleEntryCalcMs, vehicleEntrySeries, hourLabel);
			dataset.addValue(agg.solveMs, solveSeries, hourLabel);
			dataset.addValue(agg.conflictResolutionMs, conflictSeries, hourLabel);
			dataset.addValue(agg.schedulingMs, schedulingSeries, hourLabel);
		}

		// Create the chart
		CategoryAxis domainAxis = new CategoryAxis("Simulation Hour");
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		NumberAxis rangeAxis = new NumberAxis("Total Time [ms]");
		rangeAxis.setAutoRangeIncludesZero(true);

		StackedAreaRenderer renderer = new StackedAreaRenderer();
		renderer.setSeriesPaint(0, new Color(66, 133, 244));   // Blue - Vehicle Entry
		renderer.setSeriesPaint(1, new Color(52, 168, 83));    // Green - Solve
		renderer.setSeriesPaint(2, new Color(251, 188, 4));    // Yellow - Conflict Resolution
		renderer.setSeriesPaint(3, new Color(234, 67, 53));    // Red - Scheduling

		CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
		plot.setOrientation(PlotOrientation.VERTICAL);

		JFreeChart chart = new JFreeChart(
			"Processing Time Breakdown per Hour",
			JFreeChart.DEFAULT_TITLE_FONT,
			plot,
			true
		);

		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_parallelInserter_hourlyTiming.png"
		);

		try {
			ChartUtils.saveChartAsPNG(new File(filename), chart, 1200, 600);
		} catch (IOException e) {
			LOG.error("Failed to write hourly timing chart", e);
		}

		// Also write a second chart with throughput overlay
		writeStackedHourlyTimingWithThroughputPlot(hourlyData);
	}

	/**
	 * Creates a combined chart: stacked timing + throughput line overlay
	 */
	private void writeStackedHourlyTimingWithThroughputPlot(Map<Integer, HourlyAggregation> hourlyData) {
		// Create dataset for stacked area chart
		DefaultCategoryDataset timingDataset = new DefaultCategoryDataset();
		DefaultCategoryDataset throughputDataset = new DefaultCategoryDataset();

		String vehicleEntrySeries = "Vehicle Entry Calc";
		String solveSeries = "Solve (Parallel)";
		String conflictSeries = "Conflict Resolution";
		String schedulingSeries = "Scheduling";
		String throughputSeries = "Throughput [req/s]";

		for (Map.Entry<Integer, HourlyAggregation> entry : hourlyData.entrySet()) {
			String hourLabel = String.format("%02d:00", entry.getKey());
			HourlyAggregation agg = entry.getValue();

			// Timing data
			timingDataset.addValue(agg.vehicleEntryCalcMs, vehicleEntrySeries, hourLabel);
			timingDataset.addValue(agg.solveMs, solveSeries, hourLabel);
			timingDataset.addValue(agg.conflictResolutionMs, conflictSeries, hourLabel);
			timingDataset.addValue(agg.schedulingMs, schedulingSeries, hourLabel);

			// Calculate throughput for this hour
			double totalTimeSeconds = (agg.vehicleEntryCalcMs + agg.solveMs + agg.conflictResolutionMs + agg.schedulingMs) / 1000.0;
			double throughput = totalTimeSeconds > 0 ? agg.totalRequests / totalTimeSeconds : 0;
			throughputDataset.addValue(throughput, throughputSeries, hourLabel);
		}

		// Create the chart with dual axis
		CategoryAxis domainAxis = new CategoryAxis("Simulation Hour");
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		NumberAxis rangeAxis = new NumberAxis("Total Time [ms]");
		rangeAxis.setAutoRangeIncludesZero(true);

		StackedAreaRenderer areaRenderer = new StackedAreaRenderer();
		areaRenderer.setSeriesPaint(0, new Color(66, 133, 244, 180));
		areaRenderer.setSeriesPaint(1, new Color(52, 168, 83, 180));
		areaRenderer.setSeriesPaint(2, new Color(251, 188, 4, 180));
		areaRenderer.setSeriesPaint(3, new Color(234, 67, 53, 180));

		CategoryPlot plot = new CategoryPlot(timingDataset, domainAxis, rangeAxis, areaRenderer);
		plot.setOrientation(PlotOrientation.VERTICAL);

		// Add throughput as secondary dataset with line renderer
		NumberAxis throughputAxis = new NumberAxis("Throughput [req/s]");
		plot.setRangeAxis(1, throughputAxis);
		plot.setDataset(1, throughputDataset);
		plot.mapDatasetToRangeAxis(1, 1);

		org.jfree.chart.renderer.category.LineAndShapeRenderer lineRenderer =
			new org.jfree.chart.renderer.category.LineAndShapeRenderer(true, true);
		lineRenderer.setSeriesPaint(0, Color.BLACK);
		lineRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
		plot.setRenderer(1, lineRenderer);

		JFreeChart chart = new JFreeChart(
			"Processing Time & Throughput per Hour",
			JFreeChart.DEFAULT_TITLE_FONT,
			plot,
			true
		);

		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_parallelInserter_hourlyTimingWithThroughput.png"
		);

		try {
			ChartUtils.saveChartAsPNG(new File(filename), chart, 1200, 600);
		} catch (IOException e) {
			LOG.error("Failed to write hourly timing with throughput chart", e);
		}
	}

	/**
	 * Helper class for hourly aggregation of timing data.
	 */
	private static class HourlyAggregation {
		double vehicleEntryCalcMs = 0;
		double solveMs = 0;
		double conflictResolutionMs = 0;
		double schedulingMs = 0;
		int totalRequests = 0;
		int cycleCount = 0;
	}

	private void logSummaryToConsole() {
		if (cycleRecords.isEmpty()) return;

		DoubleSummaryStatistics cycleStats = cycleRecords.stream()
			.mapToDouble(ProcessingCycleRecord::totalCycleTimeMs)
			.summaryStatistics();

		DoubleSummaryStatistics utilStats = cycleRecords.stream()
			.mapToDouble(ProcessingCycleRecord::getPartitionUtilization)
			.summaryStatistics();

		DoubleSummaryStatistics imbalanceStats = cycleRecords.stream()
			.flatMap(c -> c.iterations.stream())
			.mapToDouble(IterationRecord::loadImbalanceRatio)
			.summaryStatistics();

		long totalConflicts = cycleRecords.stream()
			.flatMap(c -> c.iterations.stream())
			.mapToLong(IterationRecord::conflicts)
			.sum();

		long totalScheduled = cycleRecords.stream()
			.mapToLong(ProcessingCycleRecord::totalScheduled)
			.sum();

		// Overall throughput calculation
		double totalProcessingTimeSeconds = cycleRecords.stream()
			.mapToDouble(c -> c.totalCycleTimeNanos / 1_000_000_000.0)
			.sum();
		double overallThroughput = totalProcessingTimeSeconds > 0 ? totalScheduled / totalProcessingTimeSeconds : 0;

		DoubleSummaryStatistics throughputStats = cycleRecords.stream()
			.filter(c -> c.totalScheduled > 0)
			.mapToDouble(ProcessingCycleRecord::getThroughputRequestsPerSecond)
			.summaryStatistics();

		LOG.info("=== Parallel Inserter Performance Summary [{}] ===", mode);
		LOG.info("  Cycles: {}", cycleRecords.size());
		LOG.info("  Avg cycle time: {} ms (min: {}, max: {})",
			String.format("%.2f", cycleStats.getAverage()),
			String.format("%.2f", cycleStats.getMin()),
			String.format("%.2f", cycleStats.getMax()));
		LOG.info("  Throughput: {} req/s overall, avg {} req/s per cycle (min: {}, max: {})",
			String.format("%.1f", overallThroughput),
			String.format("%.1f", throughputStats.getAverage()),
			String.format("%.1f", throughputStats.getMin()),
			String.format("%.1f", throughputStats.getMax()));
		LOG.info("  Avg partition utilization: {}% (max partitions: {})",
			String.format("%.1f", utilStats.getAverage() * 100), maxPartitions);
		LOG.info("  Avg load imbalance ratio: {} (1.0 = perfect balance)",
			String.format("%.2f", imbalanceStats.getAverage()));
		LOG.info("  Total scheduled: {}, Total conflicts: {}", totalScheduled, totalConflicts);
	}

	void clear() {
		cycleRecords.clear();
	}
}
