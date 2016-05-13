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

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.util.*;
import org.matsim.core.utils.io.IOUtils;


public class TaxiStatsWriter
{
    private final Map<String, TaxiStats> taxiStats;


    public TaxiStatsWriter(Map<String, TaxiStats> taxiStats)
    {
        this.taxiStats = taxiStats;
    }


    public void write(String file)
    {
        try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
            writePassengerWaitTimeStats(writer);
            writeVehicleEmptyDriveRatioStats(writer);
            writeVehicleWaitRatioStats(writer);
            writeTaskTypeSums(writer);
        }
    }


    private void writePassengerWaitTimeStats(CompactCSVWriter writer)
    {
        writer.writeNext("Passenger Wait Time [s]");
        writer.writeNext(getStatsSubheader("n"));

        for (TaxiStats s : taxiStats.values()) {
            CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id)
                    .add(s.passengerWaitTime.getN() + "");
            addStats(lineBuilder, "%.1f", "%.0f", s.passengerWaitTime);
            writer.writeNext(lineBuilder);
        }
        writer.writeNextEmpty();
    }


    private void writeVehicleEmptyDriveRatioStats(CompactCSVWriter writer)
    {
        writer.writeNext("Vehicle Empty Drive Ratio");
        writer.writeNext(getStatsSubheader("fleet"));

        for (TaxiStats s : taxiStats.values()) {
            CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id).//
                    addf("%.4f", s.getFleetEmptyDriveRatio());
            addStats(lineBuilder, "%.4f", "%.3f", s.vehicleEmptyDriveRatio);
            writer.writeNext(lineBuilder);
        }
        writer.writeNextEmpty();
    }


    private void writeVehicleWaitRatioStats(CompactCSVWriter writer)
    {
        writer.writeNext("Vehicle Wait Ratio");
        writer.writeNext(getStatsSubheader("fleet"));

        for (TaxiStats s : taxiStats.values()) {
            CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id).//
                    addf("%.4f", s.getFleetStayRatio());
            addStats(lineBuilder, "%.4f", "%.3f", s.vehicleStayRatio);
            writer.writeNext(lineBuilder);
        }
        writer.writeNextEmpty();
    }


    private String[] getStatsSubheader(String header2)
    {
        return new String[] { "hour", header2, "mean", "sd", null, //
                "min", "2%ile", "5%ile", "25%ile", "50%ile", "75%ile", "95%ile", "98%ile", "max" };
    }


    private void addStats(CSVLineBuilder lineBuilder, String format1, String format2,
            DescriptiveStatistics stats)
    {
        lineBuilder.addf(format1, stats.getMean()).//
                addf(format1, stats.getStandardDeviation()).//
                add(null).//
                addf(format2, stats.getMin()). //
                addf(format2, stats.getPercentile(2)). //
                addf(format2, stats.getPercentile(5)). //
                addf(format2, stats.getPercentile(25)). //
                addf(format2, stats.getPercentile(50)). //
                addf(format2, stats.getPercentile(75)). //
                addf(format2, stats.getPercentile(95)). //
                addf(format2, stats.getPercentile(98)). //
                addf(format2, stats.getMax());
    }


    private void writeTaskTypeSums(CompactCSVWriter writer)
    {
        writer.writeNext("Total duration of tasks by type [h]");
        CSVLineBuilder headerBuilder = new CSVLineBuilder().add("hour");
        for (TaxiTask.TaxiTaskType t : TaxiTask.TaxiTaskType.values()) {
            headerBuilder.add(t.name());
        }
        writer.writeNext(headerBuilder.add("all"));

        for (TaxiStats s : taxiStats.values()) {
            CSVLineBuilder lineBuilder = new CSVLineBuilder().add(s.id);
            for (TaxiTask.TaxiTaskType t : TaxiTask.TaxiTaskType.values()) {
                lineBuilder.addf("%.2f", s.taskTimeSumsByType.get(t).doubleValue() / 3600);
            }
            lineBuilder.addf("%.2f", s.taskTimeSumsByType.getTotal().doubleValue() / 3600);
            writer.writeNext(lineBuilder);
        }
        writer.writeNextEmpty();
    }
}