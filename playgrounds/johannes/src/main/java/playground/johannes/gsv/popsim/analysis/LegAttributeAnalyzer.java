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

import playground.johannes.gsv.popsim.Predicate;
import playground.johannes.gsv.popsim.Predicates;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author jillenberger
 */
public class LegAttributeAnalyzer extends AbstractAnalyzerTask<Collection<? extends Person>> {

    private final String attKey;

    public LegAttributeAnalyzer(String attKey) {
        this.attKey = attKey;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        Map<String, Predicate<Segment>> predicates = Predicates.legPredicates(persons);


    }
}
