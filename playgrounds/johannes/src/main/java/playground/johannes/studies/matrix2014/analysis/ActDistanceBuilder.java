/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.facilities.ActivityFacilities;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class ActDistanceBuilder {

    private Predicate<Segment> predicate;

    private String predicateName;

    private boolean useWeights = false;

    private HistogramWriter writer;

    public ActDistanceBuilder setPredicate(Predicate<Segment> predicate, String predicateName) {
        this.predicate = predicate;
        this.predicateName = predicateName;
        return this;
    }

    public ActDistanceBuilder setUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
        return this;
    }

    public ActDistanceBuilder setHistogramWriter(HistogramWriter writer) {
        this.writer = writer;
        return this;
    }

     public NumericAnalyzer build(ActivityFacilities facilities) {
        String dimension = "facDistance";

        ValueProvider<Double, Segment> provider = new FacilityDistanceProvider(facilities);

        LegCollector<Double> collector = new LegCollector<>(provider);
        if (predicate != null) {
            collector.setPredicate(predicate);
            dimension = String.format("%s.%s", dimension, predicateName);
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
