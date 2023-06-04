/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util.stats;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.collect.ImmutableList;

public class TaxiStatsWriter {
	private final List<TaxiStats> taxiStats;

	public TaxiStatsWriter(List<TaxiStats> taxiStats) {
		this.taxiStats = taxiStats;
	}

	public void write(String file) {
		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
			writePassengerWaitTimeStats(writer);
			writeVehicleEmptyDriveRatioStats(writer);
			writeVehicleWaitRatioStats(writer);
			writeTaskTypeSums(writer);
		}
	}

	private void writePassengerWaitTimeStats(CompactCSVWriter writer) {
		writer.writeNext("Passenger Wait Time [s]");
		writer.writeNext(getStatsSubheader("n"));

		for (TaxiStats s : taxiStats) {
			CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id).add(s.passengerWaitTime.getN() + "");
			addStats(lineBuilder, "%.1f", "%.0f", s.passengerWaitTime);
			writer.writeNext(lineBuilder);
		}
		writer.writeNextEmpty();
	}

	private void writeVehicleEmptyDriveRatioStats(CompactCSVWriter writer) {
		writer.writeNext("Vehicle Empty Drive Ratio");
		writer.writeNext(getStatsSubheader("fleetAvg"));

		for (TaxiStats s : taxiStats) {
			CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id).addf("%.4f", s.calculateFleetEmptyDriveRatio().orElse(Double.NaN));
			addStats(lineBuilder, "%.4f", "%.3f", s.vehicleEmptyDriveRatio);
			writer.writeNext(lineBuilder);
		}
		writer.writeNextEmpty();
	}

	private void writeVehicleWaitRatioStats(CompactCSVWriter writer) {
		writer.writeNext("Vehicle Wait Ratio");
		writer.writeNext(getStatsSubheader("fleetAvg"));

		for (TaxiStats s : taxiStats) {
			CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id).addf("%.4f", s.calculateFleetStayRatio().orElse(Double.NaN));
			addStats(lineBuilder, "%.4f", "%.3f", s.vehicleStayRatio);
			writer.writeNext(lineBuilder);
		}
		writer.writeNextEmpty();
	}

	private String[] getStatsSubheader(String header2) {
		return new String[] { "hour", header2, "avg", "sd", null, //
				"min", "2%ile", "5%ile", "25%ile", "50%ile", "75%ile", "95%ile", "98%ile", "max" };
	}

	private void addStats(CSVLineBuilder lineBuilder, String format1, String format2, DescriptiveStatistics stats) {
		lineBuilder.addf(format1, stats.getMean())
				.addf(format1, stats.getStandardDeviation())
				.addEmpty()
				.addf(format2, stats.getMin())
				.addf(format2, stats.getPercentile(2))
				.addf(format2, stats.getPercentile(5))
				.addf(format2, stats.getPercentile(25))
				.addf(format2, stats.getPercentile(50))
				.addf(format2, stats.getPercentile(75))
				.addf(format2, stats.getPercentile(95))
				.addf(format2, stats.getPercentile(98))
				.addf(format2, stats.getMax());
	}

	private void writeTaskTypeSums(CompactCSVWriter writer) {
		ImmutableList<Task.TaskType> taskTypes = gtAllCollectedTaskTypes();

		writer.writeNext("Total duration of tasks by type [h]");
		CSVLineBuilder headerBuilder = new CSVLineBuilder().add("hour");
		for (Task.TaskType t : taskTypes) {
			headerBuilder.add(t.name());
		}
		writer.writeNext(headerBuilder.add("all"));

		for (TaxiStats s : taxiStats) {
			CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id);
			for (Task.TaskType t : taskTypes) {
				lineBuilder.addf("%.2f", s.taskTypeDurations.getOrDefault(t, 0.) / 3600);
			}
			lineBuilder.addf("%.2f", s.calculateTotalDuration() / 3600);
			writer.writeNext(lineBuilder);
		}
		writer.writeNextEmpty();
	}

	private ImmutableList<Task.TaskType> gtAllCollectedTaskTypes() {
		var defaultHeader = List.<Task.TaskType>of(TaxiEmptyDriveTask.TYPE, TaxiPickupTask.TYPE, TaxiOccupiedDriveTask.TYPE, TaxiDropoffTask.TYPE,
				TaxiStayTask.TYPE);
		var additionalColumns = taxiStats.stream()
				.flatMap(s -> s.taskTypeDurations.keySet().stream())
				.filter(Predicate.not(defaultHeader::contains))
				.distinct()
				.sorted(Comparator.comparing(Task.TaskType::name))
				.collect(Collectors.toList());
		return ImmutableList.<Task.TaskType>builder().addAll(defaultHeader).addAll(additionalColumns).build();
	}

}
