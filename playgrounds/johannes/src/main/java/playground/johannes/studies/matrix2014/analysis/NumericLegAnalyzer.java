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
package playground.johannes.studies.matrix2014.analysis;

import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

/**
 * @author jillenberger
 */
public class NumericLegAnalyzer {

    public static NumericAnalyzer create(String key, boolean useWeights, Predicate<Segment> predicate, String predicateName, HistogramWriter writer) {
        String dimension = key;
        ValueProvider<Double, Segment> provider = new NumericAttributeProvider<>(key);

        LegCollector<Double> collector = new LegCollector<>(provider);
        if (predicate != null) {
            collector.setPredicate(predicate);
            dimension = String.format("%s.%s", key, predicateName);
        }

        LegPersonCollector<Double> weightCollector = null;
        if (useWeights) {
            ValueProvider<Double, Person> weightProvider = new NumericAttributeProvider<>(CommonKeys.PERSON_WEIGHT);
            weightCollector = new LegPersonCollector<>(weightProvider);
            if (predicate != null) weightCollector.setPredicate(predicate);
        }

        return new NumericAnalyzer(collector, weightCollector, dimension, writer);
    }
}
