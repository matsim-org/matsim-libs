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

    AnalyzerTask<Collection<? extends Person>> build() {

        AnalyzerTaskComposite<Collection<? extends Person>> composite = new AnalyzerTaskComposite<>();

        for(Map.Entry<String, Predicate<Segment>> entry : predicates.entrySet()) {
            ValueProvider<Double, Segment> getter = new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE);
            LegCollector<Double> collector = new LegCollector<>(getter);
            collector.setPredicate(entry.getValue());

            String name = String.format("%s.%s", CommonKeys.LEG_GEO_DISTANCE, entry.getKey());
            NumericAnalyzer analyzer = new NumericAnalyzer(collector, name);

            composite.addComponent(analyzer);

        }

        return composite;
    }
}
