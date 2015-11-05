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

package playground.johannes.gsv.popsim.analysis;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class LegNextCollector<T> implements Collector<T> {

    private Predicate<Segment> predicate;

    private ValueProvider<T, Segment> provider;

    public LegNextCollector(ValueProvider<T, Segment> provider) {
        this.provider = provider;
    }

    public void setPredicate(Predicate<Segment> predicate) {
        this.predicate = predicate;
    }

    @Override
    public List<T> collect(Collection<? extends Person> persons) {
        ArrayList<T> values = new ArrayList<>(persons.size() * 10);

        for (Person p : persons) {
            for (Episode e : p.getEpisodes()) {
                for (Segment leg : e.getLegs()) {
                    if (predicate == null || predicate.test(leg)) {
                        values.add(provider.get(leg.next()));
                    }
                }
            }
        }

        values.trimToSize();

        return values;
    }
}
