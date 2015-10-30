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

package playground.johannes.synpop.data;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.common.util.ProgressLogger;

import java.util.*;

/**
 * @author johannes
 */
public class PersonUtils {

    public static  Set<? extends Person> weightedCopy(Collection<? extends Person> persons, Factory factory, int N,
                                                      Random random) {
        if(persons.size() == N) {
            return new HashSet<>(persons); //TODO weights are left untouched
        } else if(persons.size() > N) {
            throw new IllegalArgumentException("Cannot shrink population.");
        }

        List<Person> templates = new ArrayList<>(persons);
		/*
		 * get max weight
		 */
        TObjectDoubleHashMap<Person> weights = new TObjectDoubleHashMap<>(persons.size());
        double maxW = 0;
        for(Person person : persons) {
            String wStr = person.getAttribute(CommonKeys.PERSON_WEIGHT);
            double w = 0;
            if(wStr != null) {
                w = Double.parseDouble(wStr);
            }
            weights.put(person, w);
            maxW = Math.max(w, maxW);
        }
		/*
		 * adjust weight so that max weight equals probability 1
		 */
        ProgressLogger.init(N, 2, 10);
        Set<Person> clones = new HashSet<>();
        while(clones.size() < N) {
            Person template = templates.get(random.nextInt(templates.size()));
            double w = weights.get(template);
            double p = w/maxW;
            if(p > random.nextDouble()) {
                StringBuilder builder = new StringBuilder();
                builder.append(template.getId());
                builder.append("clone");
                builder.append(clones.size());

                Person clone = PersonUtils.deepCopy(template, builder.toString(), factory);
                clone.setAttribute(CommonKeys.PERSON_WEIGHT, "1.0");
                clones.add(clone);
                ProgressLogger.step();
            }
        }

        return clones;
    }

    public static Person shallowCopy(Person person, Factory factory) {
        return shallowCopy(person, person.getId(), factory);
    }

    public static Person shallowCopy(Person person, String id, Factory factory) {
        Person clone = factory.newPerson(id);
        for(String key : person.keys()) {
            clone.setAttribute(key, person.getAttribute(key));
        }

        return clone;
    }

    public static Person deepCopy(Person person, String id, Factory factory) {
        Person clone = shallowCopy(person, id, factory);
        for(Episode e : person.getEpisodes()) {
            clone.addEpisode(deepCopy(e, factory));
        }
        return clone;
    }

    public static Episode shallowCopy(Episode episode, Factory factory) {
        Episode clone = factory.newEpisode();
        for(String key : episode.keys()) {
            clone.setAttribute(key, episode.getAttribute(key));
        }

        return clone;
    }

    public static Episode deepCopy(Episode episode, Factory factory) {
        Episode clone = shallowCopy(episode, factory);

        for(Segment act : episode.getActivities()) {
            Segment actClone = shallowCopy(act, factory);
            clone.addActivity(actClone);
        }

        for(Segment leg : episode.getLegs()) {
            Segment legClone = shallowCopy(leg, factory);
            clone.addLeg(legClone);
        }

        return clone;
    }

    public static Segment shallowCopy(Segment segment, Factory factory) {
        Segment clone = factory.newSegment();
        for(String key : segment.keys()) {
            clone.setAttribute(key, segment.getAttribute(key));
        }

        return clone;
    }
}
