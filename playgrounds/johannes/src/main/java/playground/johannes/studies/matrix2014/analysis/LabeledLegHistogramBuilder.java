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
import playground.johannes.synpop.analysis.LegCollector;
import playground.johannes.synpop.analysis.NumericAttributeProvider;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.analysis.ValueProvider;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.Collection;

/**
 * @author jillenberger
 */
public class LabeledLegHistogramBuilder {

    private LabeledHistogramBuilder builder;

    private LegCollector<String> valueCollector;

    private LegPersonCollector<Double> weightsCollector;

    public LabeledLegHistogramBuilder(ValueProvider<String, Segment> provider) {
        valueCollector = new LegCollector<>(provider);
        weightsCollector = new LegPersonCollector<>(new NumericAttributeProvider<Person>(CommonKeys.PERSON_WEIGHT));
        builder = new LabeledHistogramBuilder(valueCollector, weightsCollector);
    }

    public void setPredicate(Predicate<Segment> predicate) {
        valueCollector.setPredicate(predicate);
        weightsCollector.setPredicate(predicate);
    }

    public TObjectDoubleMap<String> build(Collection<? extends Person> persons) {
        return builder.build(persons);
    }
}
