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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;



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
public final class EventsToLegs implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, VehicleArrivesAtFacilityEventHandler {
	
	private class PendingTransitTravel {

		final Id vehicleId;
		final Id accessStop;

		public PendingTransitTravel(Id vehicleId, Id accessStop) {
			this.vehicleId = vehicleId;
			this.accessStop = accessStop;
		}

	}

	private class LineAndRoute {

		final Id transitLineId;
		final Id transitRouteId;
		Id lastFacilityId;

		LineAndRoute(Id transitLineId, Id transitRouteId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
		}
		
	}

	public interface LegHandler {
	    void handleLeg(Id agentId, Leg leg);
	}
	
    private Scenario scenario;
	private Map<Id, LegImpl> legs = new HashMap<Id, LegImpl>();
    private Map<Id, List<Id>> routes = new HashMap<Id, List<Id>>();
    private Map<Id, TeleportationArrivalEvent> routelessTravels = new HashMap<Id, TeleportationArrivalEvent>();
    private Map<Id, PendingTransitTravel> transitTravels = new HashMap<Id, PendingTransitTravel>();
    private Map<Id, LineAndRoute> transitVehicle2currentRoute = new HashMap<Id, LineAndRoute>();
    private LegHandler legHandler;
	public EventsToLegs(Scenario scenario) {
		this.scenario = scenario;
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
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
		if (lineAndRoute != null) {
			lineAndRoute.lastFacilityId = event.getFacilityId();
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
		if (lineAndRoute != null) {
			transitTravels.put(event.getPersonId(), new PendingTransitTravel(event.getVehicleId(), lineAndRoute.lastFacilityId));
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
	
	}

	@Override
    public void handleEvent(LinkEnterEvent event) {
        List<Id> route = routes.get(event.getPersonId());
        route.add(event.getLinkId());
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent travelEvent) {
        routelessTravels.put(travelEvent.getPersonId(), travelEvent);
    }

	@Override
	public void handleEvent(PersonArrivalEvent event) {
	    LegImpl leg = legs.get(event.getPersonId());
	    leg.setArrivalTime(event.getTime());
	    double travelTime = leg.getArrivalTime() - leg.getDepartureTime();
	    leg.setTravelTime(travelTime);
	    List<Id> route = routes.remove(event.getPersonId());
	    assert route.size() >= 1;
	    PendingTransitTravel pendingTransitTravel;
	    if (route.size() > 1) {
	        NetworkRoute networkRoute = RouteUtils.createNetworkRoute(route, null);
	        networkRoute.setTravelTime(travelTime);
	        networkRoute.setDistance(RouteUtils.calcDistance(networkRoute, scenario.getNetwork()));
	        leg.setRoute(networkRoute);
	    } else if ((pendingTransitTravel = transitTravels.remove(event.getPersonId())) != null) {
	    	LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(pendingTransitTravel.vehicleId);
			TransitLine line = scenario.getTransitSchedule().getTransitLines().get(lineAndRoute.transitLineId);
			ExperimentalTransitRoute experimentalTransitRoute = new ExperimentalTransitRoute(
					scenario.getTransitSchedule().getFacilities().get(pendingTransitTravel.accessStop),
					line, 
					line.getRoutes().get(lineAndRoute.transitRouteId), 
					scenario.getTransitSchedule().getFacilities().get(lineAndRoute.lastFacilityId));
			experimentalTransitRoute.setTravelTime(travelTime);
			experimentalTransitRoute.setDistance(RouteUtils.calcDistance(experimentalTransitRoute, scenario.getTransitSchedule(), scenario.getNetwork()));
			leg.setRoute(experimentalTransitRoute);
	    } else {
	    	TeleportationArrivalEvent travelEvent = routelessTravels.remove(event.getPersonId());
	    	GenericRoute genericRoute = new GenericRouteImpl(route.get(0), event.getLinkId());
	    	genericRoute.setTravelTime(travelTime);
	        if (travelEvent != null) {
	            genericRoute.setDistance(travelEvent.getDistance());
	        } else {
	            genericRoute.setDistance(0.0);
	        }
	        leg.setRoute(genericRoute);
	    }
	    legHandler.handleLeg(event.getPersonId(), leg);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		LineAndRoute lineAndRoute = new LineAndRoute(event.getTransitLineId(), event.getTransitRouteId());
		transitVehicle2currentRoute.put(event.getVehicleId(), lineAndRoute);
	}

	@Override
	public void reset(int iteration) {
	    legs.clear();
	    routes.clear();
	}

	public void setLegHandler(LegHandler legHandler) {
	    this.legHandler = legHandler;
	}

}
