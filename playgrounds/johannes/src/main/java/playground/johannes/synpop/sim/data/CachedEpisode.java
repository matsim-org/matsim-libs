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

package playground.johannes.synpop.sim.data;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 */
public class CachedEpisode extends CachedElement implements Episode {

    private final List<Segment> activities;

    private final List<Segment> legs;

    private final CachedPerson person;

    public CachedEpisode(Episode delegate, CachedPerson person) {
        super(delegate);
        this.person = person;

        activities = new ArrayList<>(delegate.getActivities().size());
        for(Segment activity : delegate.getActivities()) {
            CachedSegment s = new CachedSegment(activity);
            s.setEpisode(this, false);
            activities.add(s);
        }

        legs = new ArrayList<>(delegate.getLegs().size());
        for(Segment leg : delegate.getLegs()) {
            CachedSegment s = new CachedSegment(leg);
            s.setEpisode(this, true);
            legs.add(s);
        }
    }

    @Override
    public List<Segment> getActivities() {
        return activities;
//        return ((Episode)getDelegate()).getActivities();
    }

    @Override
    public List<Segment> getLegs() {
        return legs;
//        return ((Episode)getDelegate()).getLegs();
    }

    @Override
    public void addActivity(Segment activity) {
        throw new UnsupportedOperationException("Structural modification not allowed.");
//        ((Episode)getDelegate()).addActivity(activity);
    }

    @Override
    public void addLeg(Segment leg) {
        throw new UnsupportedOperationException("Structural modification not allowed.");
//        ((Episode)getDelegate()).addLeg(leg);
    }

    @Override
    public void insertActivity(Segment activity, int index) {
        throw new UnsupportedOperationException("Structural modification not allowed.");
    }

    @Override
    public void insertLeg(Segment leg, int index) {
        throw new UnsupportedOperationException("Structural modification not allowed.");
    }

    @Override
    public void removeActivity(Segment activity) {
        throw new UnsupportedOperationException("Structural modification not allowed.");
    }

    @Override
    public void removeLeg(Segment leg) {
        throw new UnsupportedOperationException("Structural modification not allowed.");
    }

    @Override
    public Person getPerson() {
        return person;
//        throw new UnsupportedOperationException("Navigation not supported.");
//        return ((Episode)getDelegate()).getPerson();
    }
}
