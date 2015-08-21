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

/**
 * @author johannes
 */
public class PersonUtils {

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

    public static Episode deepCopy(Episode episode, Factory factory) {
        Episode clone = factory.newEpisode();
        for(String key : episode.keys()) {
            clone.setAttribute(key, episode.getAttribute(key));
        }

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
