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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles logging and visualization of partition activity for the parallel inserter.
 *
 * @author Steffen Axer
 */
class PartitionActivityLogger {
	private static final Logger LOG = LogManager.getLogger(PartitionActivityLogger.class);

	private final List<DataRecord> dataRecords = new ArrayList<>();
	private final MatsimServices matsimServices;
	private final String mode;
	private final int maxPartitions;
	private final boolean enabled;

	record DataRecord(double time, int requestsDensityPerMinute, int activePartitions) {
	}

	PartitionActivityLogger(MatsimServices matsimServices, String mode, DrtParallelInserterParams params) {
		this.matsimServices = matsimServices;
		this.mode = mode;
		this.maxPartitions = params.getMaxPartitions();
		this.enabled = params.isLogThreadActivity();
	}

	void record(double time, int requestCount, double collectionPeriod, int activePartitions) {
		if (!enabled) return;
		dataRecords.add(new DataRecord(time, (int) (requestCount / collectionPeriod * 60), activePartitions));
	}

	void writeOutputs() {
		if (!enabled) return;
		writePlot();
		writeCsv();
	}

	private void writePlot() {
		XYSeries densitySeries = new XYSeries("Requests Density");
		XYSeries partitionsSeries = new XYSeries("Active Partitions");

		for (DataRecord record : dataRecords) {
			densitySeries.add(record.time, record.requestsDensityPerMinute);
			partitionsSeries.add(record.time, record.activePartitions);
		}

		XYSeriesCollection densityDataset = new XYSeriesCollection(densitySeries);
		NumberAxis densityAxis = new NumberAxis("Requests Density [req/min]");
		XYPlot densityPlot = new XYPlot(densityDataset, null, densityAxis, null);
		densityPlot.setRenderer(new XYLineAndShapeRenderer(true, false));

		XYSeriesCollection partitionsDataset = new XYSeriesCollection(partitionsSeries);
		NumberAxis partitionsAxis = new NumberAxis("Active Partitions");
		partitionsAxis.setAutoRangeIncludesZero(false);
		partitionsAxis.setLowerBound(1);
		partitionsAxis.setUpperBound(maxPartitions + 1);

		XYPlot partitionsPlot = new XYPlot(partitionsDataset, null, partitionsAxis, null);
		partitionsPlot.setRenderer(new XYLineAndShapeRenderer(true, false));

		NumberAxis timeAxis = new NumberAxis("Time [s]");
		CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(timeAxis);
		combinedPlot.add(densityPlot, 1);
		combinedPlot.add(partitionsPlot, 1);

		var chart = new JFreeChart("Active Partitions Over Time", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);

		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_partitionActivity.png"
		);

		try {
			ChartUtils.saveChartAsPNG(new File(filename), chart, 900, 600);
		} catch (IOException e) {
			LOG.error("Failed to write chart image", e);
		}
	}

	private void writeCsv() {
		String sep = matsimServices.getConfig().global().getDefaultDelimiter();
		String header = String.join(sep, "time", "requestsDensityPerMinute", "activePartitions");
		String filename = matsimServices.getControllerIO().getIterationFilename(
			matsimServices.getIterationNumber(), mode + "_dataRecordsLog.csv.gz"
		);

		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			writer.write(header);
			writer.newLine();

			for (DataRecord record : dataRecords) {
				writer.write(String.join(sep,
					String.valueOf(record.time),
					String.valueOf(record.requestsDensityPerMinute),
					String.valueOf(record.activePartitions)
				));
				writer.newLine();
			}
		} catch (IOException ex) {
			LOG.error("Failed to write dataRecordsLog", ex);
		}
	}
}
