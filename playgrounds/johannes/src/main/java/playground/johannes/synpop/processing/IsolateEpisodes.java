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

package playground.johannes.synpop.processing;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Factory;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PersonUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 */
public class IsolateEpisodes implements PersonTask {

    private final String attrKey;

    private final Factory factory;

    private final Map<String, Set<Person>> populations;

    public IsolateEpisodes(String attrKey, Factory factory) {
        this.attrKey = attrKey;
        this.factory = factory;
        populations = new HashMap<>();
    }

    @Override
    public void apply(Person person) {
        int idCnt = 0;

        for(Episode episode : person.getEpisodes()) {
            String key = episode.getAttribute(attrKey);
            Set<Person> persons = populations.get(key);
            if(persons == null) {
                persons = new HashSet<>();
                populations.put(key, persons);
            }

            String id = person.getId();
            if(person.getEpisodes().size() > 1) {
                id = String.format("%s.%s", person.getId(), idCnt);
                idCnt++;
            }
            Person clone = PersonUtils.shallowCopy(person, id, factory);
            clone.addEpisode(PersonUtils.deepCopy(episode, factory));

            persons.add(clone);
        }
    }

    public Map<String, Set<Person>> getPopulations() {
        return populations;
    }
}
