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

import java.util.Collection;
import java.util.Map;

/**
 * @author jillenberger
 */
public class GeoDistanceBuilder {

    private Map<String, Predicate<Segment>> predicates;

    private final HistogramWriter histogramWriter;

    public GeoDistanceBuilder(HistogramWriter histogramWriter) {
        this(histogramWriter, null);
    }

    public GeoDistanceBuilder(HistogramWriter histogramWriter, Map<String, Predicate<Segment>> predicates) {
        this.histogramWriter = histogramWriter;
        this.setPredicates(predicates);
    }

    public void setPredicates(Map<String, Predicate<Segment>> predicates) {
        this.predicates = predicates;
    }

    public AnalyzerTask<Collection<? extends Person>> build() {
        AnalyzerTask<Collection<? extends Person>> task;

        if (predicates == null || predicates.isEmpty()) {
            NumericAnalyzer analyzer = buildWithPredicate(null, null);
            task = analyzer;
        } else {
            //ConcurrentAnalyzerTask<Collection<? extends Person>> composite = new ConcurrentAnalyzerTask<>();
            AnalyzerTaskComposite<Collection<? extends Person>> composite = new AnalyzerTaskComposite<>();
            for (Map.Entry<String, Predicate<Segment>> entry : predicates.entrySet()) {
                NumericAnalyzer analyzer = buildWithPredicate(entry.getValue(), entry.getKey());
                composite.addComponent(analyzer);
            }

            task = composite;
        }

        return task;
    }

    private NumericAnalyzer buildWithPredicate(Predicate<Segment> predicate, String predicateName) {
        ValueProvider<Double, Segment> getter = new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE);
        ValueProvider<Double, Person> weightGetter = new NumericAttributeProvider(CommonKeys.PERSON_WEIGHT);

        LegCollector<Double> collector = new LegCollector<>(getter);
        LegPersonCollector<Double> weightCollector = new LegPersonCollector<>(weightGetter);

        if (predicate != null) {
            collector.setPredicate(predicate);
            weightCollector.setPredicate(predicate);
        }

        String name = CommonKeys.LEG_GEO_DISTANCE;
        if (predicateName != null)
            name = String.format("%s.%s", CommonKeys.LEG_GEO_DISTANCE, predicateName);

        return new NumericAnalyzer(collector, weightCollector, name, histogramWriter);
    }
}
