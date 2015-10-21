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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;



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
public final class EventsToLegs implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, 
TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, VehicleArrivesAtFacilityEventHandler {
	
	private class PendingTransitTravel {

		final Id<Vehicle> vehicleId;
		final Id<TransitStopFacility> accessStop;

		public PendingTransitTravel(Id<Vehicle> vehicleId, Id<TransitStopFacility> accessStop) {
			this.vehicleId = vehicleId;
			this.accessStop = accessStop;
		}

	}

	private class LineAndRoute {

		final Id<TransitLine> transitLineId;
		final Id<TransitRoute> transitRouteId;
		final Id<Person> driverId;
		Id<TransitStopFacility> lastFacilityId;

		LineAndRoute(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, Id<Person> driverId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
            this.driverId = driverId;
		}
		
	}

	public interface LegHandler {
	    void handleLeg(Id<Person> agentId, Leg leg);
	}
	
	private Scenario scenario;
	private Map<Id<Person>, LegImpl> legs = new HashMap<>();
	private Map<Id<Person>, List<Id<Link>>> experiencedRoutes = new HashMap<>();
	private Map<Id<Person>, TeleportationArrivalEvent> routelessTravels = new HashMap<>();
	private Map<Id<Person>, PendingTransitTravel> transitTravels = new HashMap<>();
	private Map<Id<Vehicle>, LineAndRoute> transitVehicle2currentRoute = new HashMap<>();
	private LegHandler legHandler;
	private Map<Id<Vehicle>, List<Id<Person>>> personsInsideVehicle = new HashMap<>();
	
	public EventsToLegs(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
	    LegImpl leg = new LegImpl(event.getLegMode());
	    leg.setDepartureTime(event.getTime());
	    legs.put(event.getPersonId(), leg);
	    
	    List<Id<Link>> route = new ArrayList<>();
	    route.add(event.getLinkId());
	    experiencedRoutes.put(event.getPersonId(), route);
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
		if (!personsInsideVehicle.containsKey(event.getVehicleId())){
			personsInsideVehicle.put(event.getVehicleId(), new ArrayList<Id<Person>>());
		}
		personsInsideVehicle.get(event.getVehicleId()).add(event.getPersonId());
		
		LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
		if (lineAndRoute != null
                && !event.getPersonId().equals(lineAndRoute.driverId)) { // transit drivers are not considered to travel by transit
			transitTravels.put(event.getPersonId(), new PendingTransitTravel(event.getVehicleId(), lineAndRoute.lastFacilityId));
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
	
	}

	@Override
    public void handleEvent(LinkEnterEvent event) {
		// add the link to the route of all agents inside the vehicle
		for (Id<Person> personInsideVehicle : personsInsideVehicle.get(event.getVehicleId())){
			List<Id<Link>> route = experiencedRoutes.get(personInsideVehicle);
	        route.add(event.getLinkId());
		}
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
	    List<Id<Link>> experiencedRoute = experiencedRoutes.get(event.getPersonId());
	    assert experiencedRoute.size() >= 1;
	    PendingTransitTravel pendingTransitTravel;
	    if (experiencedRoute != null && experiencedRoute.size() > 1) {
	        NetworkRoute networkRoute = RouteUtils.createNetworkRoute(experiencedRoute, null);
	        networkRoute.setTravelTime(travelTime);

	        networkRoute.setDistance(RouteUtils.calcDistance(networkRoute, scenario.getNetwork()));
	        // TODO MATSIM-227: replace the above by taking distance from List<Id<Link>> experiencedRoute (minus first/last link)
	        // and add manually distance on first/last link.  Newly based on VehicleEnters/LeavesTrafficEvents, which should (newly)
	        // contain this information.  kai/mz, sep'15
	        
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
	    	Route genericRoute = new GenericRouteImpl(experiencedRoute.get(0), event.getLinkId());
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
		LineAndRoute lineAndRoute = new LineAndRoute(event.getTransitLineId(), event.getTransitRouteId(), event.getDriverId());
		transitVehicle2currentRoute.put(event.getVehicleId(), lineAndRoute);
	}

	@Override
	public void reset(int iteration) {
	    legs.clear();
	    experiencedRoutes.clear();
	}

	public void setLegHandler(LegHandler legHandler) {
	    this.legHandler = legHandler;
	}

}
