/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author johannes
 */
public class PlainEpisode extends PlainElement implements playground.johannes.synpop.data.Episode {

    private List<Segment> activities = new ArrayList<Segment>();

    private List<Segment> legs = new ArrayList<Segment>();

    private Person person;

    public void addLeg(Segment leg) {
        if(legs.contains(leg)) throw new IllegalArgumentException("You cannot add the same segment twice.");
        legs.add(leg);
        ((PlainSegment)leg).setEpisode(this, true);
    }

    @Override
    public Person getPerson() {
        return person;
    }

    public List<Segment> getLegs() {
        return legs;
    }

    public void addActivity(Segment activity) {
        if(activities.contains(activity)) throw new IllegalArgumentException("You cannot add the same segment twice.");
        activities.add(activity);
        ((PlainSegment)activity).setEpisode(this, false);
    }

    public List<Segment> getActivities() {
        return activities;
    }

    public void removeActivity(Segment activity) {
        activities.remove(activity);
        ((PlainSegment)activity).setEpisode(null, false);
    }

    public void removeLeg(Segment leg) {
        legs.remove(leg);
        ((PlainSegment)leg).setEpisode(null, true);
    }

    public PlainEpisode clone() {
        PlainEpisode clone = new PlainEpisode();
//        clone.setPerson(person); not clear if should do this here.

        for (Entry<String, String> entry : getAttributes().entrySet()) {
            clone.setAttribute(entry.getKey(), entry.getValue());
        }

        for (Attributable act : activities) {
            clone.addActivity(((PlainSegment) act).clone());
        }

        for (Attributable leg : legs) {
            clone.addLeg(((PlainSegment) leg).clone());
        }

        return clone;
    }

    void setPerson(Person person) {
        this.person = person;
    }
}
