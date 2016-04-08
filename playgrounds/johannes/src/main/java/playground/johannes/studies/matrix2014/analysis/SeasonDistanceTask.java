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

import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.data.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 */
public class SeasonDistanceTask implements AnalyzerTask<Collection<? extends Person>> {


    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        /*
        Get all purposes.
         */
        LegCollector<String> purposeCollector = new LegCollector<>(new AttributeProvider<>(CommonKeys.LEG_PURPOSE));
        Set<String> purposes = new HashSet<>(purposeCollector.collect(persons));
        purposes.remove(null);
        /*
        Get all seasons
         */
        PersonCollector<String> seasonCollector = new PersonCollector<>(new AttributeProvider<>(SetSeason.SEASON_KEY));
        Set<String> seasons = new HashSet<>(seasonCollector.collect(persons));
        seasons.remove(null);

        Predicate<Segment> modePredicate = new LegAttributePredicate(CommonKeys.LEG_MODE, CommonValues.LEG_MODE_CAR);

        for(String purpose : purposes) {
            Predicate<Segment> purposePredicate = new LegAttributePredicate(CommonKeys.LEG_PURPOSE, purpose);
            for(String season : seasons) {
                Predicate<Segment> seasonPredicate = new LegPersonAttributePredicate(SetSeason.SEASON_KEY, season);

                Predicate<Segment> predicate = PredicateAndComposite.create(modePredicate,
                        purposePredicate,
                        seasonPredicate);

                LegCollector<Segment> counter = new LegCollector(new EntityProvider());
            }
        }
    }

    private static class EntityProvider implements ValueProvider<Integer, Attributable> {

        @Override
        public Integer get(Attributable attributable) {
            return 1;
        }
    }
}
