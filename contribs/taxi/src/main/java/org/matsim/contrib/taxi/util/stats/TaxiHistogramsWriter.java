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

import java.io.*;
import java.util.Map;

import org.matsim.contrib.util.CSVLineBuilder;
import org.matsim.contrib.util.histogram.*;
import org.matsim.core.utils.io.IOUtils;

import com.opencsv.CSVWriter;


public class TaxiHistogramsWriter
{
    private final Map<String, TaxiStats> taxiStats;


    public TaxiHistogramsWriter(Map<String, TaxiStats> taxiStats)
    {
        this.taxiStats = taxiStats;
    }


    public void write(String file)
    {
        try (CSVWriter writer = new CSVWriter(IOUtils.getBufferedWriter(file), '\t',
                CSVWriter.NO_QUOTE_CHARACTER)) {
            writePassengerWaitTime(writer);
            writeVehicleEmptyDriveRatio(writer);
            writeVehicleStayRatio(writer);
            writeHourlyWaitRatioCounters(writer);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private void writePassengerWaitTime(CSVWriter writer)
    {
        writeHistogramHeader(writer, "Passenger Wait Time [min]", new UniformHistogram(2.5, 25));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    UniformHistogram.create(2.5 * 60, 25, s.passengerWaitTime.getValues()));
        }
        writer.writeNext(CSVLineBuilder.EMPTY_LINE);
    }


    private void writeVehicleEmptyDriveRatio(CSVWriter writer)
    {
        writeHistogramHeader(writer, "Vehicle Empty Drive Ratio [%]", new UniformHistogram(5, 20));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    UniformHistogram.create(0.05, 20, s.vehicleEmptyDriveRatio.getValues()));
        }
        writer.writeNext(CSVLineBuilder.EMPTY_LINE);
    }


    private void writeVehicleStayRatio(CSVWriter writer)
    {
        writeHistogramHeader(writer, "Vehicle Stay Ratio [%]", new UniformHistogram(5, 20));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    UniformHistogram.create(0.05, 20, s.vehicleStayRatio.getValues()));
        }
        writer.writeNext(CSVLineBuilder.EMPTY_LINE);
    }


    private void writeHourlyWaitRatioCounters(CSVWriter writer)
    {
        // [0, 0.01) approx. "always busy"; 0pct is impossible due to 1-sec stay tasks
        // [0.01, 0.25) and [0.25, 0.5) both busy for most of the time
        // [0.5, 0.75) and [0.75, 1) both idle for most of the time 
        // [1, 1] //== always idle
        double[] bounds = { 0, 0.01, 0.25, 0.5, 0.75, 1, 1.0000001 };

        writeHistogramHeader(writer, "Vehicle Wait Ratio Counts", new BoundedHistogram(bounds));
        for (TaxiStats s : taxiStats.values()) {
            writeHistogramValues(writer, s.id,
                    BoundedHistogram.create(bounds, s.vehicleStayRatio.getValues()));
        }
        writer.writeNext(CSVLineBuilder.EMPTY_LINE);
    }


    private void writeHistogramHeader(CSVWriter writer, String header, Histogram<?> histogram)
    {
        writer.writeNext(CSVLineBuilder.line(header));
        CSVLineBuilder lineBuilder = new CSVLineBuilder().add("hour");
        for (int i = 0; i < histogram.getBinCount(); i++) {
            lineBuilder.add(histogram.getBin(i) + "");
        }
        writer.writeNext(lineBuilder.build());
    }


    private void writeHistogramValues(CSVWriter writer, String hour, Histogram<?> histogram)
    {
        CSVLineBuilder lineBuilder = new CSVLineBuilder().add(hour + "");
        for (int i = 0; i < histogram.getBinCount(); i++) {
            lineBuilder.add(histogram.getCount(i) + "");
        }
        writer.writeNext(lineBuilder.build());
    }
}
