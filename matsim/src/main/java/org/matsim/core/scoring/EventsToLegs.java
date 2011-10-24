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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.TravelEvent;
import org.matsim.core.events.TravelEventHandler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventsToLegs implements AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, TravelEventHandler {

    private Map<Id, LegImpl> legs = new HashMap<Id, LegImpl>();
    private Map<Id, List<Id>> routes = new HashMap<Id, List<Id>>();
    private Map<Id, TravelEvent> routelessTravels = new HashMap<Id, TravelEvent>();
    private LegHandler legHandler;

    @Override
    public void handleEvent(AgentArrivalEvent event) {
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
            TravelEvent travelEvent = routelessTravels.get(event.getPersonId());
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
    public void handleEvent(AgentDepartureEvent event) {
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
    public void handleEvent(TravelEvent travelEvent) {
        routelessTravels.put(travelEvent.getPersonId(), travelEvent);
    }

}
