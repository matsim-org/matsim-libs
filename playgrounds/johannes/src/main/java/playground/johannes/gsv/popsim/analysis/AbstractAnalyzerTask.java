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
package playground.johannes.gsv.popsim.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author jillenberger
 */
public abstract class AbstractAnalyzerTask<T> implements AnalyzerTask<T> {

    protected String output;

    private List<Discretizer> discretizers;

    private List<String> discretizerTypes;

    private List<Boolean> discretizerFlags;

    public void setOutput(String output) {
        this.output = output;
    }

    public void addDiscretizer(Discretizer discretizer, String type, boolean reweight) {
        if (discretizers == null) {
            discretizers = new LinkedList<>();
            discretizerTypes = new LinkedList<>();
            discretizerFlags = new LinkedList<>();
        }

        discretizers.add(discretizer);
        discretizerTypes.add(type);
        discretizerFlags.add(reweight);
    }

    protected void writeHistograms(double[] values, String name) {
        double[] weights = new double[values.length];
        Arrays.fill(weights, 1.0);
        writeHistograms(values, weights, name);
    }

    protected void writeHistograms(double[] values, double[] weights, String name) {
        if (output != null && discretizers != null) {
            for (int i = 0; i < discretizers.size(); i++) {
                Discretizer discretizer = discretizers.get(i);
                String type = discretizerTypes.get(i);
                boolean reweight = discretizerFlags.get(i);

                try {
                    TDoubleDoubleHashMap hist = Histogram.createHistogram(values, weights, discretizer, reweight);
                    StatsWriter.writeHistogram(hist, name, "frequency", String.format("%s/%s.%s.txt", output, name, type));

                    hist = Histogram.normalize(hist);
                    StatsWriter.writeHistogram(hist, name, "probability", String.format("%s/%s.%s.norm.txt", output, name, type));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
