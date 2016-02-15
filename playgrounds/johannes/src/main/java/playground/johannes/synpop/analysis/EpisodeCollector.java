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
package playground.johannes.synpop.analysis;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author jillenberger
 */
public class EpisodeCollector<T> extends AbstractCollector<T, Episode, Episode> {

    public EpisodeCollector(ValueProvider<T, Episode> provider) {
        super(provider);
    }

    @Override
    public List<T> collect(Collection<? extends Person> persons) {
        ArrayList<T> values = new ArrayList<>(persons.size() * 2);
        for(Person p : persons) {
            for(Episode e : p.getEpisodes()) {
                if(predicate == null || predicate.test(e)) {
                    values.add(provider.get(e));
                }
            }
        }

        return values;
    }
}
