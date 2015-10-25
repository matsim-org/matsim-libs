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

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.*;

/**
 * @author johannes
 */
public class Predicates {

    public static Map<String, ActTypePredicate> actTypePredicates(Collection<? extends Person> persons) {
        Set<String> acttypes = new HashSet<>();

        for(Person person : persons) {
            for(Episode episode : person.getEpisodes()) {
                for(Segment act : episode.getActivities()) {
                    String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
                    if(type != null)
                        acttypes.add(type);
                }
            }
        }

        Map<String, ActTypePredicate> predicates = new HashMap<>();
        for(String type : acttypes) {
            predicates.put(type, new ActTypePredicate(type));
        }

        return predicates;
    }
}
