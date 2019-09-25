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

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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
public final class EventsToLegs implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler,
        TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {


    private IdMap<Vehicle, Set<Id<Person>>> vehicle2passengers = new IdMap<>(Vehicle.class);

	private static class LineAndRoute {

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
	private IdMap<Person, Leg> legs = new IdMap<>(Person.class);
	private IdMap<Person, List<Id<Link>>> experiencedRoutes = new IdMap<>(Person.class);
	private IdMap<Person, Double> relPosOnDepartureLinkPerPerson = new IdMap<>(Person.class);
	private IdMap<Person, Double> relPosOnArrivalLinkPerPerson = new IdMap<>(Person.class);
	private IdMap<Person, TeleportationArrivalEvent> routelessTravels = new IdMap<>(Person.class);
	private IdMap<Person, PendingTransitTravel> transitTravels = new IdMap<>(Person.class);
	private IdMap<Vehicle, LineAndRoute> transitVehicle2currentRoute = new IdMap<>(Vehicle.class);
	private List<LegHandler> legHandlers = new ArrayList<>();

    EventsToLegs(Scenario scenario) {
		this.network = scenario.getNetwork();
		if (scenario.getConfig().transit().isUseTransit()) {
			this.transitSchedule = scenario.getTransitSchedule();
		}
	}


    @Inject
    EventsToLegs(Network network) {
        this.network = network;
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
        if (lineAndRoute != null
                && !event.getPersonId().equals(lineAndRoute.driverId)) { // transit drivers are not considered to travel by transit
            transitTravels.put(event.getPersonId(), new PendingTransitTravel(event.getVehicleId(), lineAndRoute.lastFacilityId));
        } else {
            Set<Id<Person>> passengersInVehicle = vehicle2passengers.getOrDefault(event.getVehicleId(), new HashSet<>());
            passengersInVehicle.add(event.getPersonId());
            vehicle2passengers.put(event.getVehicleId(), passengersInVehicle);
        }
    }

	@Override
	public void reset(int iteration) {
		legs.clear();
		experiencedRoutes.clear();
		transitTravels.clear();
		routelessTravels.clear();
		transitVehicle2currentRoute.clear();

	}



	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Leg leg = PopulationUtils.createLeg(event.getLegMode());
		leg.setDepartureTime(event.getTime());
		legs.put(event.getPersonId(), leg);

		List<Id<Link>> route = new ArrayList<>();
		route.add(event.getLinkId());
		experiencedRoutes.put(event.getPersonId(), route);
	}

	@Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (vehicle2passengers.containsKey(event.getVehicleId())) {
            vehicle2passengers.get(event.getVehicleId()).remove(event.getPersonId());
        }
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
        vehicle2passengers.get(event.getVehicleId())
				.forEach(p -> experiencedRoutes.computeIfPresent(p, (personId, ids) -> {
					ids.add(event.getLinkId());
					return ids;
				}));

    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {

        // remember the relative position on the link
        if (vehicle2passengers.containsKey(event.getVehicleId())) {
            vehicle2passengers.get(event.getVehicleId())
                    .forEach(personId -> relPosOnDepartureLinkPerPerson.put(personId, event.getRelativePositionOnLink()));
        }

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
		Leg leg = legs.get(event.getPersonId());
		leg.setTravelTime( event.getTime() - leg.getDepartureTime() );
		double travelTime = leg.getDepartureTime() + leg.getTravelTime() - leg.getDepartureTime();
		leg.setTravelTime(travelTime);
		List<Id<Link>> experiencedRoute = experiencedRoutes.get(event.getPersonId());
		assert experiencedRoute.size() >= 1  ;
		PendingTransitTravel pendingTransitTravel;
		if (experiencedRoute.size() > 1) { // different links processed
			NetworkRoute networkRoute = RouteUtils.createNetworkRoute(experiencedRoute, null);
			networkRoute.setTravelTime(travelTime);

			/* use the relative position of vehicle enter/leave traffic events on first/last links
			 * to calculate the correct route distance including the first/last link. 
			 * (see MATSIM-227) tt feb'16
			 */
			double relPosOnDepartureLink = relPosOnDepartureLinkPerPerson.get(event.getPersonId());
			Double relPosOnArrivalLink = relPosOnArrivalLinkPerPerson.get(event.getPersonId());
			Gbl.assertNotNull( relPosOnArrivalLink );
			networkRoute.setDistance(RouteUtils.calcDistance(networkRoute, relPosOnDepartureLink, 
					relPosOnArrivalLink, network));

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
			Route genericRoute = RouteUtils.createGenericRouteImpl(experiencedRoute.get(0), event.getLinkId());
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
	public void handleEvent(VehicleLeavesTrafficEvent event) {

		// remember the relative position on the link
        if (vehicle2passengers.containsKey(event.getVehicleId())) {
            vehicle2passengers.get(event.getVehicleId())
                    .forEach(personId -> relPosOnArrivalLinkPerPerson.put(personId, event.getRelativePositionOnLink()));
        }
    }

    private static class PendingTransitTravel {

        final Id<Vehicle> vehicleId;
        final Id<TransitStopFacility> accessStop;

        PendingTransitTravel(Id<Vehicle> vehicleId, Id<TransitStopFacility> accessStop) {
            this.vehicleId = vehicleId;
            this.accessStop = accessStop;
        }

    }

}
