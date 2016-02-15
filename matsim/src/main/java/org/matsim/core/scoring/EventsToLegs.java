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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;



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
TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	
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
		
		@Override
		public String toString() {
			return "[" + super.toString() + 
					" transitLineId=" + transitLineId +
					" transitRouteId=" + transitRouteId +
					" driverId=" + driverId +
					" lastFacilityId=" + lastFacilityId + "]" ;
		}
		
	}

	public interface LegHandler {
	    void handleLeg(PersonExperiencedLeg leg);
	}

	private Network network;
	private TransitSchedule transitSchedule = null;

	@Inject(optional=true)
	public void setTransitSchedule(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;
	}
	private Map<Id<Person>, LegImpl> legs = new HashMap<>();
	private Map<Id<Person>, List<Id<Link>>> experiencedRoutes = new HashMap<>();
	private Map<Id<Person>, TeleportationArrivalEvent> routelessTravels = new HashMap<>();
	private Map<Id<Person>, PendingTransitTravel> transitTravels = new HashMap<>();
	private Map<Id<Vehicle>, LineAndRoute> transitVehicle2currentRoute = new HashMap<>();
	private List<LegHandler> legHandlers = new ArrayList<>();
	

	@Inject
	EventsToLegs(Network network, EventsManager eventsManager) {
		this.network = network;
		eventsManager.addHandler(this);
	}



	public EventsToLegs(Scenario scenario) {
		this.network = scenario.getNetwork();
		if (scenario.getConfig().transit().isUseTransit()) {
			this.transitSchedule = scenario.getTransitSchedule();
		}
	}
	
	@Override
	public void reset(int iteration) {
	    legs.clear();
	    experiencedRoutes.clear();
	    transitTravels.clear();
	    routelessTravels.clear();
	    transitVehicle2currentRoute.clear();

	    delegate.reset(iteration);
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
	public void handleEvent(PersonEntersVehicleEvent event) {
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
		Id<Person> driverOfVehicle = delegate.getDriverOfVehicle(event.getVehicleId());
		List<Id<Link>> route = experiencedRoutes.get(driverOfVehicle);
	    route.add(event.getLinkId());
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent travelEvent) {
        routelessTravels.put(travelEvent.getPersonId(), travelEvent);
    }

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
	    LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
	    if (lineAndRoute != null) {
		    lineAndRoute.lastFacilityId = event.getFacilityId();
	    }
    }
    
	@Override
	public void handleEvent(PersonArrivalEvent event) {
	    LegImpl leg = legs.get(event.getPersonId());
	    leg.setArrivalTime(event.getTime());
	    double travelTime = leg.getArrivalTime() - leg.getDepartureTime();
	    leg.setTravelTime(travelTime);
	    List<Id<Link>> experiencedRoute = experiencedRoutes.get(event.getPersonId());
	    assert experiencedRoute.size() >= 1  ;
	    PendingTransitTravel pendingTransitTravel;
	    if (experiencedRoute != null && experiencedRoute.size() > 1) {
		    // yy first condition always fulfilled?  (since otherwise the above assert would fail)?? kai, jan'16
		    
	        NetworkRoute networkRoute = RouteUtils.createNetworkRoute(experiencedRoute, null);
	        networkRoute.setTravelTime(travelTime);

	        networkRoute.setDistance(RouteUtils.calcDistance(networkRoute, network));
	        // TODO MATSIM-227: replace the above by taking distance from List<Id<Link>> experiencedRoute (minus first/last link)
	        // and add manually distance on first/last link.  Newly based on VehicleEnters/LeavesTrafficEvents, which should (newly)
	        // contain this information.  kai/mz, sep'15
	        
	        leg.setRoute(networkRoute);
	    } else if ((pendingTransitTravel = transitTravels.remove(event.getPersonId())) != null) {
		    // i.e. experiencedRoute.size()==1 && pending transit travel (= person has entered a vehicle)
		    
		    final LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(pendingTransitTravel.vehicleId);
		    assert lineAndRoute!=null ;
		    
		    final TransitStopFacility accessFacility = transitSchedule.getFacilities().get(pendingTransitTravel.accessStop);
		    assert accessFacility!=null ;

		    final TransitLine line = transitSchedule.getTransitLines().get(lineAndRoute.transitLineId);
		    assert line!=null ;

		    final TransitRoute route = line.getRoutes().get(lineAndRoute.transitRouteId);
		    assert route!=null ;

		    final Id<TransitStopFacility> lastFacilityId = lineAndRoute.lastFacilityId;
		    if ( lastFacilityId==null ) {
			    Logger.getLogger(this.getClass()).warn("breakpoint");
		    }
		    assert lastFacilityId!=null ;
		    
		    final TransitStopFacility egressFacility = transitSchedule.getFacilities().get(lastFacilityId);
		    assert egressFacility!=null ;

			ExperimentalTransitRoute experimentalTransitRoute = new ExperimentalTransitRoute(
					accessFacility,
					line, 
					route,
					egressFacility);
			experimentalTransitRoute.setTravelTime(travelTime);
			experimentalTransitRoute.setDistance(RouteUtils.calcDistance(experimentalTransitRoute, transitSchedule, network));
			leg.setRoute(experimentalTransitRoute);
	    } else {
		    // i.e. experiencedRoute.size()==1 and no pendingTransitTravel
		    
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
		for (LegHandler legHandler : legHandlers) {
			legHandler.handleLeg(new PersonExperiencedLeg(event.getPersonId(), leg));
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		LineAndRoute lineAndRoute = new LineAndRoute(event.getTransitLineId(), event.getTransitRouteId(), event.getDriverId());
		transitVehicle2currentRoute.put(event.getVehicleId(), lineAndRoute);
	}

	public void addLegHandler(LegHandler legHandler) {
	    this.legHandlers.add(legHandler);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

}
