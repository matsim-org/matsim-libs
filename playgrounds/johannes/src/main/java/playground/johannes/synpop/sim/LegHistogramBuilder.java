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
package playground.johannes.synpop.sim;

import gnu.trove.map.TDoubleDoubleMap;
import org.matsim.contrib.common.stats.Discretizer;
import playground.johannes.studies.matrix2014.analysis.LegPersonCollector;
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
public class LegHistogramBuilder implements HistogramBuilder {

    private DefaultHistogramBuilder builder;

    private LegCollector<Double> valueCollector;

    private LegPersonCollector<Double> weightsCollector;

    public LegHistogramBuilder(ValueProvider<Double, Segment> provider, Discretizer discretizer) {
        valueCollector = new LegCollector<>(provider);
        weightsCollector = new LegPersonCollector<>(new NumericAttributeProvider<Person>(CommonKeys.PERSON_WEIGHT));
        builder = new DefaultHistogramBuilder(valueCollector, weightsCollector, discretizer);
    }

    public void setPredicate(Predicate<Segment> predicate) {
        valueCollector.setPredicate(predicate);
        weightsCollector.setPredicate(predicate);
    }

    @Override
    public TDoubleDoubleMap build(Collection<? extends Person> persons) {
        return builder.build(persons);
    }
}
