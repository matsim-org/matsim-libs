/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.analysis;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import playground.johannes.synpop.analysis.Collector;
import playground.johannes.synpop.data.Person;

import java.util.Collection;
import java.util.List;

/**
 * @author jillenberger
 */
public class LabeledHistogramBuilder {

    private Collector<String> valueCollector;

    private Collector<Double> weightsCollector;

    public LabeledHistogramBuilder(Collector<String> valueCollector, Collector<Double> weightsCollector) {
        this.valueCollector = valueCollector;
        this.weightsCollector = weightsCollector;

    }

    public TObjectDoubleMap<String> build(Collection<? extends Person> persons) {
        List<String> values = valueCollector.collect(persons);
        List<Double> weights = weightsCollector.collect(persons);

        TObjectDoubleMap<String> hist = new TObjectDoubleHashMap<>();

        if(values.size() != weights.size()) throw new RuntimeException("Values and weights have to have equal length.");

        for(int i = 0; i < values.size(); i++) {
            hist.adjustOrPutValue(values.get(i), weights.get(i), weights.get(i));
        }

        return hist;
    }


}
