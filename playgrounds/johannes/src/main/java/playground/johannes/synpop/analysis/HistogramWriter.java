/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.synpop.analysis;

import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author jillenberger
 */
public class HistogramWriter {

    private final FileIOContext ioContext;

    private final List<DiscretizerBuilder> builders;

    public HistogramWriter(FileIOContext ioContext, DiscretizerBuilder builder) {
        this.ioContext = ioContext;
        this.builders = new ArrayList<>();
        addBuilder(builder);
    }

    public void addBuilder(DiscretizerBuilder builder) {
        builders.add(builder);
    }

    public void writeHistograms(double[] values, String name) {
        double[] weights = new double[values.length];
        Arrays.fill(weights, 1.0);
        writeHistograms(values, weights, name);
    }

    public void writeHistograms(double[] values, double[] weights, String name) {
        for (int i = 0; i < builders.size(); i++) {
            Discretizer discretizer = builders.get(i).build(values);
            String type = builders.get(i).getName();

            try {
                TDoubleDoubleHashMap hist = Histogram.createHistogram(values, weights, discretizer, false);
                StatsWriter.writeHistogram(hist, name, "frequency", String.format("%s/%s.%s.txt", ioContext.getPath(), name, type));

                hist = Histogram.normalize(hist);
                StatsWriter.writeHistogram(hist, name, "probability", String.format("%s/%s.%s.norm.txt", ioContext.getPath(), name, type));

                hist = Histogram.createHistogram(values, weights, discretizer, true);
                StatsWriter.writeHistogram(hist, name, "frequency", String.format("%s/%s.%s.w.txt", ioContext
                        .getPath(), name, type));

                hist = Histogram.normalize(hist);
                StatsWriter.writeHistogram(hist, name, "probability", String.format("%s/%s.%s.w.norm.txt", ioContext
                        .getPath(), name, type));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
