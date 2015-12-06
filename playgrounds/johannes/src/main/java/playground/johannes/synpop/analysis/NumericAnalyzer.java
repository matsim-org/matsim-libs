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

import playground.johannes.gsv.popsim.CollectionUtils;
import playground.johannes.synpop.data.Person;

import java.util.Collection;
import java.util.List;

/**
 * @author jillenberger
 */
public class NumericAnalyzer implements AnalyzerTask<Collection<? extends Person>> {

    private final Collector<Double> collector;

    private final String dimension;

    private final HistogramWriter histogramWriter;

    public NumericAnalyzer(Collector<Double> collector, String dimension) {
        this(collector, dimension, null);
    }

    public NumericAnalyzer(Collector<Double> collector, String dimension, HistogramWriter histogramWriter) {
        this.collector = collector;
        this.dimension = dimension;
        this.histogramWriter = histogramWriter;

    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        List<Double> values = collector.collect(persons);
        containers.add(new StatsContainer(dimension, values));

        if (histogramWriter != null) {
            double[] doubleValues = CollectionUtils.toNativeArray(values);
            histogramWriter.writeHistograms(doubleValues, dimension);
        }

    }
}
