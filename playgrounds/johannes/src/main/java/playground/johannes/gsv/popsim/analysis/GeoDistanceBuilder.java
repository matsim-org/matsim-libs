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

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.Collection;
import java.util.Map;

/**
 * @author jillenberger
 */
public class GeoDistanceBuilder {

    private Map<String, Predicate<Segment>> predicates;

    private final FileIOContext ioContext;

    public GeoDistanceBuilder(FileIOContext ioContext) {
        this.ioContext = ioContext;
    }

    public AnalyzerTask<Collection<? extends Person>> build() {
        AnalyzerTask<Collection<? extends Person>> task;

        if (predicates == null || predicates.isEmpty()) {
            task = buildWithPredicate(null, null);
        } else {
            ConcurrentAnalyzerTask<Collection<? extends Person>> composite = new ConcurrentAnalyzerTask<>();

            for (Map.Entry<String, Predicate<Segment>> entry : predicates.entrySet()) {
                composite.addComponent(buildWithPredicate(entry.getValue(), entry.getKey()));
            }

            task = composite;
        }

        return task;
    }

    private AnalyzerTask buildWithPredicate(Predicate<Segment> predicate, String predicateName) {
        ValueProvider<Double, Segment> getter = new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE);

        LegCollector<Double> collector = new LegCollector<>(getter);
        if (predicate != null)
            collector.setPredicate(predicate);

        String name = CommonKeys.LEG_GEO_DISTANCE;
        if (predicateName != null)
            name = String.format("%s.%s", CommonKeys.LEG_GEO_DISTANCE, predicateName);

        return new NumericAnalyzer(collector, name, ioContext);
    }
}
