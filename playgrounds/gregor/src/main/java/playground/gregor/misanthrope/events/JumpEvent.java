package playground.gregor.misanthrope.events;/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;

public class JumpEvent extends Event {
    private final Id<Person> personId;
    private final double dir;
    private final int from;
    private final double realtime;
    private final int to;

    public JumpEvent(double time, Id<Person> personId, double dir, int from, int to) {
        super(Math.floor(time));
        this.personId = personId;
        this.dir = dir;
        this.from = from;
        this.to = to;
        this.realtime = time;
    }

    public double getRealtime() {
        return realtime;
    }

    public Id<Person> getPersonId() {
        return personId;
    }

    public double getDir() {
        return dir;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    @Override
    public String getEventType() {
        return null;
    }
}
