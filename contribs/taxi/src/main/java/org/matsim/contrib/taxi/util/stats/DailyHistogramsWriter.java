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

import org.matsim.core.utils.io.*;

import com.opencsv.CSVWriter;


public class DailyHistogramsWriter
{
    private final DailyHistograms dailyHistograms;


    public DailyHistogramsWriter(DailyHistograms dailyHistograms)
    {
        this.dailyHistograms = dailyHistograms;
    }


    public void write(String file)
    {
        try (CSVWriter writer = new CSVWriter(IOUtils.getBufferedWriter(file), '\t',
                CSVWriter.NO_QUOTE_CHARACTER)) {
            writeDailyHistogram(writer, "Empty Drive Ratio [%]", dailyHistograms.emptyDriveRatio);
            writeDailyHistogram(writer, "Vehicle Wait Ratio [%]", dailyHistograms.stayRatio);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    private void writeDailyHistogram(CSVWriter writer, String header, Histogram histogram)
    {
        writer.writeNext(CSVLines.line(header));
        writer.writeNext(CSVHistogramUtils.createBinsLine(histogram, 100.));
        writer.writeNext(CSVHistogramUtils.createValuesLine(histogram));
        writer.writeNext(CSVLines.EMPTY_LINE);
    }
}
