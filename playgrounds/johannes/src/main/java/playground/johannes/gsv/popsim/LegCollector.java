/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public abstract class LegCollector implements Collector {

    private Predicate<Person> personPredicate;

    private Predicate<Episode> episodePredicate;

    private Predicate<Segment> legPredicate;

    public void setPersonPredicate(Predicate<Person> predicate) {
        this.personPredicate = predicate;
    }

    public void setEpisodePredicate(Predicate<Episode> predicate) {
        this.episodePredicate = predicate;
    }

    public void setLegPredicate(Predicate<Segment> predicate) {
        this.legPredicate = predicate;
    }

    @Override
    public List<Double> collect(Collection<? extends Person> persons) {
        List<Double> values = new ArrayList<>(persons.size() * 10);

        for(Person p : persons) {
            if(personPredicate == null || personPredicate.test(p)) {
                for(Episode e : p.getEpisodes()) {
                    if(episodePredicate == null || episodePredicate.test(e)) {
                        for(Segment leg : e.getLegs()) {
                            if(legPredicate == null || legPredicate.test(leg)) {
                                values.add(value(leg));
                            }
                        }
                    }
                }
            }
        }

        return values;
    }

    protected abstract Double value(Segment leg);
}
