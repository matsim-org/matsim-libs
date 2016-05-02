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

import java.io.IOException;
import java.util.EnumMap;

import org.matsim.contrib.taxi.util.stats.HourlyHistograms.Type;
import org.matsim.core.utils.io.*;

import com.opencsv.CSVWriter;


public class HourlyHistogramsWriter
{
    private final HourlyHistograms[] hourlyHistograms;
    private final EnumMap<HourlyHistograms.Type, Double> subHeaderScalingMap = new EnumMap<>(
            HourlyHistograms.Type.class);


    public HourlyHistogramsWriter(HourlyHistograms[] hourlyHistograms)
    {
        this.hourlyHistograms = hourlyHistograms;
        initSubHeaderScalingMap();
    }


    private void initSubHeaderScalingMap()
    {
        subHeaderScalingMap.put(HourlyHistograms.Type.passengerWaitTime, 1. / 60);// s => min
        subHeaderScalingMap.put(HourlyHistograms.Type.emptyDriveTime, 1. / 60);
        subHeaderScalingMap.put(HourlyHistograms.Type.occupiedDriveTime, 1. / 60);
        subHeaderScalingMap.put(HourlyHistograms.Type.emptyDriveRatio, 100.);// fraction => %
        subHeaderScalingMap.put(HourlyHistograms.Type.stayRatio, 100.);
    }


    public void write(String file)
    {
        try (CSVWriter writer = new CSVWriter(IOUtils.getBufferedWriter(file), '\t',
                CSVWriter.NO_QUOTE_CHARACTER)) {
            writeHourlyHistograms(writer, "Passenger Wait Time [min]", Type.passengerWaitTime);
            writeHourlyHistograms(writer, "Empty Drive Time [min]", Type.emptyDriveTime);
            writeHourlyHistograms(writer, "Occupied Drive Time [min]", Type.occupiedDriveTime);
            writeHourlyHistograms(writer, "Empty Drive Ratio [%]", Type.emptyDriveRatio);
            writeHourlyHistograms(writer, "Vehicle Wait Ratio [%]", Type.stayRatio);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private void writeHourlyHistograms(CSVWriter writer, String header, HourlyHistograms.Type type)
    {
        writer.writeNext(CSVLines.line(header));

        if (hourlyHistograms.length > 0) {
            CSVLineBuilder binsLineBuilder = new CSVLineBuilder().add("hour");
            CSVHistogramUtils.addBinsToBuilder(binsLineBuilder,
                    hourlyHistograms[0].histogramMap.get(type), subHeaderScalingMap.get(type));
            writer.writeNext(binsLineBuilder.build());

            for (HourlyHistograms hh : hourlyHistograms) {
                CSVLineBuilder valuesLineBuilder = new CSVLineBuilder().add(hh.hour + "");
                CSVHistogramUtils.addValuesToBuilder(valuesLineBuilder, hh.histogramMap.get(type));
                writer.writeNext(valuesLineBuilder.build());
            }
        }

        writer.writeNext(CSVLines.EMPTY_LINE);
    }

}
