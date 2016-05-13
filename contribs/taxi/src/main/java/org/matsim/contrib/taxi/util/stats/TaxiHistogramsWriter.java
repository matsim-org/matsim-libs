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

import org.matsim.contrib.util.*;
import org.matsim.contrib.util.histogram.*;
import org.matsim.core.utils.io.IOUtils;


public class TaxiHistogramsWriter
{
    private final Map<String, TaxiStats> taxiStats;


    public TaxiHistogramsWriter(Map<String, TaxiStats> taxiStats)
    {
        this.taxiStats = taxiStats;
    }


    public void write(String file)
    {
        try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
            writePassengerWaitTime(writer);
            writeVehicleEmptyDriveRatio(writer);
            writeVehicleStayRatio(writer);
            writeHourlyWaitRatioCounters(writer);
        }
    }


    private void writePassengerWaitTime(CompactCSVWriter writer)
    {
        writeHistogramHeader(writer, "Passenger Wait Time [min]", new UniformHistogram(2.5, 25));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    UniformHistogram.create(2.5 * 60, 25, s.passengerWaitTime.getValues()));
        }
        writer.writeNextEmpty();
    }


    private void writeVehicleEmptyDriveRatio(CompactCSVWriter writer)
    {
        writeHistogramHeader(writer, "Vehicle Empty Drive Ratio", new UniformHistogram(0.05, 20));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    UniformHistogram.create(0.05, 20, s.vehicleEmptyDriveRatio.getValues()));
        }
        writer.writeNextEmpty();
    }


    private void writeVehicleStayRatio(CompactCSVWriter writer)
    {
        writeHistogramHeader(writer, "Vehicle Wait Ratio", new UniformHistogram(0.05, 20));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    UniformHistogram.create(0.05, 20, s.vehicleStayRatio.getValues()));
        }
        writer.writeNextEmpty();
    }


    private void writeHourlyWaitRatioCounters(CompactCSVWriter writer)
    {
        // [0, 0.01) approx. "always busy"; 0pct is impossible due to 1-sec stay tasks
        // [0.01, 0.25) and [0.25, 0.5) both busy for most of the time
        // [0.5, 0.75) and [0.75, 1) both idle for most of the time 
        // [1, 1] //== always idle
        double[] bounds = { 0, 0.01, 0.25, 0.5, 0.75, 1, 1.00000001 };

        writeHistogramHeader(writer, "Vehicle Wait Ratio Counts", new BoundedHistogram(bounds));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    BoundedHistogram.create(bounds, s.vehicleStayRatio.getValues()));
        }
        writer.writeNextEmpty();
    }


    private void writeHistogramHeader(CompactCSVWriter writer, String header,
            Histogram<?> histogram)
    {
        writer.writeNext(header);
        CSVLineBuilder lineBuilder = new CSVLineBuilder().add("hour");
        for (int i = 0; i < histogram.getBinCount(); i++) {
            lineBuilder.addf("%.2f+", histogram.getBin(i));
        }
        writer.writeNext(lineBuilder.build());
    }


    private void writeHistogramValues(CompactCSVWriter writer, String hour, Histogram<?> histogram)
    {
        CSVLineBuilder lineBuilder = new CSVLineBuilder().add(hour + "");
        for (int i = 0; i < histogram.getBinCount(); i++) {
            lineBuilder.add(histogram.getCount(i) + "");
        }
        writer.writeNext(lineBuilder.build());
    }
}
