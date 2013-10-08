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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;



/**
 * 
 * Converts a stream of Events into a stream of Legs. Passes Legs to a single LegHandler which must be registered with this class.
 * Mainly intended for scoring, but can be used for any kind of Leg related statistics. Essentially, it allows you to read
 * Legs from the simulation like you would read Legs from Plans, except that the Plan does not even need to exist.
 * 
 * Note that the instances of Leg passed to the LegHandler will never be identical to those in the Scenario! Even
 * in a "no-op" simulation which only reproduces the Plan, new instances will be created. So if you attach your own data
 * to the Legs in the Scenario, that's your own lookout.
 * 
 * @author michaz
 *
 */
public final class EventsToLegs implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, TeleportationArrivalEventHandler {
	// made this, for the time being, final.  This is currently not pluggable at this level.  If you need such
	// functionality, please talk to Michael Zilske. kai & dominik, dec'12
	
	public interface LegHandler {
	    void handleLeg(Id agentId, Leg leg);
	}
	
    private Map<Id, LegImpl> legs = new HashMap<Id, LegImpl>();
    private Map<Id, List<Id>> routes = new HashMap<Id, List<Id>>();
    private Map<Id, TeleportationArrivalEvent> routelessTravels = new HashMap<Id, TeleportationArrivalEvent>();
    private LegHandler legHandler;

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        LegImpl leg = legs.get(event.getPersonId());
        leg.setArrivalTime(event.getTime());
        double travelTime = leg.getArrivalTime() - leg.getDepartureTime();
        leg.setTravelTime(travelTime);
        List<Id> route = routes.get(event.getPersonId());
        assert route.size() >= 1;
        if (route.size() > 1) {
            NetworkRoute networkRoute = RouteUtils.createNetworkRoute(route, null);
            networkRoute.setTravelTime(travelTime);
            leg.setRoute(networkRoute);
            routes.remove(event.getPersonId());
        } else {
            GenericRoute genericRoute = new GenericRouteImpl(route.get(0), event.getLinkId());
            TeleportationArrivalEvent travelEvent = routelessTravels.get(event.getPersonId());
            if (travelEvent != null) {
                genericRoute.setDistance(travelEvent.getDistance());
            } else {
                genericRoute.setDistance(0.0);
            }
            leg.setRoute(genericRoute);
            routelessTravels.remove(event.getPersonId());
        }
        legHandler.handleLeg(event.getPersonId(), leg);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        LegImpl leg = new LegImpl(event.getLegMode());
        leg.setDepartureTime(event.getTime());
        legs.put(event.getPersonId(), leg);
        List<Id> route = new ArrayList<Id>();
        route.add(event.getLinkId());
        routes.put(event.getPersonId(), route);
    }

    @Override
    public void reset(int iteration) {
        legs.clear();
        routes.clear();
    }

    public void setLegHandler(LegHandler legHandler) {
        this.legHandler = legHandler;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        List<Id> route = routes.get(event.getPersonId());
        route.add(event.getLinkId());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

    }

    @Override
    public void handleEvent(TeleportationArrivalEvent travelEvent) {
        routelessTravels.put(travelEvent.getPersonId(), travelEvent);
    }

}
