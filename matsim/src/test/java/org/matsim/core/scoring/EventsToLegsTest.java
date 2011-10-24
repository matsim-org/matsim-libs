/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.TravelEventImpl;
import org.matsim.testcases.MatsimTestCase;


public class EventsToLegsTest extends MatsimTestCase {

    private Leg producedLeg;

    public void testCreatesLeg() {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventsToLegs eventsToLegs = new EventsToLegs();
        eventsToLegs.setLegHandler(new LegHandler(){
            @Override
            public void handleLeg(Id agentId, Leg leg) {
                producedLeg = leg;
            }
        });
        eventsToLegs.handleEvent(eventsManager.getFactory().createAgentDepartureEvent(10.0, new IdImpl("1"), new IdImpl("l1"), "walk"));
        eventsToLegs.handleEvent(new TravelEventImpl(30.0, new IdImpl("1"), 50.0));
        eventsToLegs.handleEvent(eventsManager.getFactory().createAgentArrivalEvent(30.0, new IdImpl("1"), new IdImpl("l2"), "walk"));
        assertNotNull(producedLeg);
        assertEquals(10.0,producedLeg.getDepartureTime());
        assertEquals(20.0,producedLeg.getTravelTime());
        assertEquals(50.0,producedLeg.getRoute().getDistance());
    }

    public void testCreatesLegWithRoute() {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventsToLegs eventsToLegs = new EventsToLegs();
        eventsToLegs.setLegHandler(new LegHandler(){
            @Override
            public void handleLeg(Id agentId, Leg leg) {
                producedLeg = leg;
            }
        });
        IdImpl agentId = new IdImpl("1");
        eventsToLegs.handleEvent(eventsManager.getFactory().createAgentDepartureEvent(10.0, agentId, new IdImpl("l1"), "car"));
        eventsToLegs.handleEvent(eventsManager.getFactory().createLinkLeaveEvent(10.0, agentId, new IdImpl("l1")));
        eventsToLegs.handleEvent(eventsManager.getFactory().createLinkEnterEvent(11.0, agentId, new IdImpl("l2")));
        eventsToLegs.handleEvent(eventsManager.getFactory().createLinkLeaveEvent(15.0, agentId, new IdImpl("l2")));
        eventsToLegs.handleEvent(eventsManager.getFactory().createLinkEnterEvent(16.0, agentId, new IdImpl("l3")));
        eventsToLegs.handleEvent(eventsManager.getFactory().createAgentArrivalEvent(30.0, agentId, new IdImpl("l3"), "car"));
        assertNotNull(producedLeg);
        assertEquals(10.0,producedLeg.getDepartureTime());
        assertEquals(20.0,producedLeg.getTravelTime());
        assertEquals(20.0,producedLeg.getRoute().getTravelTime());
    }

}
