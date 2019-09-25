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
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
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

import com.google.inject.Inject;

/**
 * Converts a stream of Events into a stream of Legs. Passes Legs to a single LegHandler which must be registered with this class.
 * Mainly intended for scoring, but can be used for any kind of Leg related statistics. Essentially, it allows you to read
 * Legs from the simulation like you would read Legs from Plans, except that the Plan does not even need to exist.
 * <p>
 * Note that the instances of Leg passed to the LegHandler will never be identical to those in the Scenario! Even
 * in a "no-op" simulation which only reproduces the Plan, new instances will be created. So if you attach your own data
 * to the Legs in the Scenario, that's your own lookout.
 *
 * @author michaz
 */
public final class EventsToLegs
		implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler,
		TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
		VehicleArrivesAtFacilityEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private static class PendingTransitTravel {
		final Id<Vehicle> vehicleId;
		final Id<org.matsim.facilities.Facility> accessStop;

		PendingTransitTravel(Id<Vehicle> vehicleId, Id<org.matsim.facilities.Facility> accessStop) {
			this.vehicleId = vehicleId;
			this.accessStop = accessStop;
		}
	}

	private static class LineAndRoute {
		final Id<TransitLine> transitLineId;
		final Id<TransitRoute> transitRouteId;
		final Id<Person> driverId;
		Id<org.matsim.facilities.Facility> lastFacilityId;

		LineAndRoute(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, Id<Person> driverId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
			this.driverId = driverId;
		}

		@Override
		public String toString() {
			return "["
					+ super.toString()
					+ " transitLineId="
					+ transitLineId
					+ " transitRouteId="
					+ transitRouteId
					+ " driverId="
					+ driverId
					+ " lastFacilityId="
					+ lastFacilityId
					+ "]";
		}
	}

	private static class PendingVehicleTravel {
		private final int accessLinkIdx;
		private final VehicleRoute route;
		private double relativePositionOnDepartureLink;

		private PendingVehicleTravel(VehicleRoute route, int accessLinkIdx) {
			this.route = route;
			this.accessLinkIdx = accessLinkIdx;
		}
	}

	private static class VehicleRoute {
		private final List<Id<Link>> links = new ArrayList<>();
		private final List<PendingVehicleTravel> newVehicleTravels = new ArrayList<>();
		private double relativePositionOnLastArrivalLink;
	}

	public interface LegHandler {
		void handleLeg(PersonExperiencedLeg leg);
	}

	private Network network;
	private TransitSchedule transitSchedule = null;

	@Inject(optional = true)
	public void setTransitSchedule(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;
	}

	private Map<Id<Person>, Leg> legs = new HashMap<>();
	private Map<Id<Person>, List<Id<Link>>> experiencedRoutes = new HashMap<>();
	private Map<Id<Person>, Double> relPosOnDepartureLinkPerPerson = new HashMap<>();
	private Map<Id<Person>, Double> relPosOnArrivalLinkPerPerson = new HashMap<>();

	private Map<Id<Person>, TeleportationArrivalEvent> routelessTravels = new HashMap<>();
	private Map<Id<Person>, PendingTransitTravel> transitTravels = new HashMap<>();
	private Map<Id<Person>, PendingVehicleTravel> vehicleTravels = new HashMap<>();

	private Map<Id<Vehicle>, LineAndRoute> transitVehicle2currentRoute = new HashMap<>();
	private Map<Id<Vehicle>, VehicleRoute> vehicle2route = new HashMap<>();

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
	public void reset(int iteration) {
		legs.clear();
		experiencedRoutes.clear();

		transitTravels.clear();
		vehicleTravels.clear();
		routelessTravels.clear();

		transitVehicle2currentRoute.clear();
		vehicle2route.clear();

		relPosOnDepartureLinkPerPerson.clear();
		relPosOnArrivalLinkPerPerson.clear();
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
	public void handleEvent(PersonEntersVehicleEvent event) {
		LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
		if (lineAndRoute != null) {
			if (!event.getPersonId().equals(lineAndRoute.driverId)) {
				// transit drivers are not considered to travel by transit
				transitTravels.put(event.getPersonId(),
						new PendingTransitTravel(event.getVehicleId(), lineAndRoute.lastFacilityId));
			}
		} else {
			VehicleRoute route = vehicle2route.computeIfAbsent(event.getVehicleId(), vehicleId -> new VehicleRoute());
			int currentLinkIdx = Math.max(0, route.links.size() - 1);
			PendingVehicleTravel vehicleTravel = new PendingVehicleTravel(route, currentLinkIdx);
			vehicleTravels.put(event.getPersonId(), vehicleTravel);
			route.newVehicleTravels.add(vehicleTravel);
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		VehicleRoute route = vehicle2route.get(event.getVehicleId());
		if (route != null) {
			if (route.links.isEmpty()) {
				route.links.add(event.getLinkId());
			} else {
				// in case vehicle was teleported (TODO assuming teleportation is not possible with people on board??)
				route.links.set(route.links.size() - 1, event.getLinkId());
			}

			//update relativePositionOnDepartureLink for each new passenger and the driver
			route.newVehicleTravels.forEach(
					vehicleTravel -> vehicleTravel.relativePositionOnDepartureLink = event.getRelativePositionOnLink());
			route.newVehicleTravels.clear();
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		VehicleRoute route = vehicle2route.get(event.getVehicleId());
		if (route != null) {
			route.links.add(event.getLinkId());
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		VehicleRoute route = vehicle2route.get(event.getVehicleId());
		if (route != null) {
			route.relativePositionOnLastArrivalLink = event.getRelativePositionOnLink();
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
		if (lineAndRoute != null) {
			lineAndRoute.lastFacilityId = event.getFacilityId();
		}
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent travelEvent) {
		routelessTravels.put(travelEvent.getPersonId(), travelEvent);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Leg leg = legs.get(event.getPersonId());
		leg.setTravelTime(event.getTime() - leg.getDepartureTime());
		double travelTime = leg.getDepartureTime() + leg.getTravelTime() - leg.getDepartureTime();
		leg.setTravelTime(travelTime);
		List<Id<Link>> experiencedRoute = experiencedRoutes.get(event.getPersonId());
		assert experiencedRoute.size() >= 1;
		PendingTransitTravel pendingTransitTravel;
		PendingVehicleTravel pendingVehicleTravel;
		if (experiencedRoute.size() > 1) { // different links processed
			NetworkRoute networkRoute = RouteUtils.createNetworkRoute(experiencedRoute, null);
			networkRoute.setTravelTime(travelTime);

			/* use the relative position of vehicle enter/leave traffic events on first/last links
			 * to calculate the correct route distance including the first/last link.
			 * (see MATSIM-227) tt feb'16
			 */
			double relPosOnDepartureLink = relPosOnDepartureLinkPerPerson.get(event.getPersonId());
			Double relPosOnArrivalLink = relPosOnArrivalLinkPerPerson.get(event.getPersonId());
			Gbl.assertNotNull(relPosOnArrivalLink);
			networkRoute.setDistance(
					RouteUtils.calcDistance(networkRoute, relPosOnDepartureLink, relPosOnArrivalLink, network));

			leg.setRoute(networkRoute);
		} else if ((pendingTransitTravel = transitTravels.remove(event.getPersonId())) != null) {
			// i.e. experiencedRoute.size()==1 && pending transit travel (= person has entered a vehicle)

			final LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(pendingTransitTravel.vehicleId);
			assert lineAndRoute != null;

			final TransitStopFacility accessFacility = transitSchedule.getFacilities()
					.get(pendingTransitTravel.accessStop);
			assert accessFacility != null;

			final TransitLine line = transitSchedule.getTransitLines().get(lineAndRoute.transitLineId);
			assert line != null;

			final TransitRoute route = line.getRoutes().get(lineAndRoute.transitRouteId);
			assert route != null;

			final Id<org.matsim.facilities.Facility> lastFacilityId = lineAndRoute.lastFacilityId;
			if (lastFacilityId == null) {
				Logger.getLogger(this.getClass()).warn("breakpoint");
			}
			assert lastFacilityId != null;

			final TransitStopFacility egressFacility = transitSchedule.getFacilities().get(lastFacilityId);
			assert egressFacility != null;

			ExperimentalTransitRoute experimentalTransitRoute = new ExperimentalTransitRoute(accessFacility, line,
					route, egressFacility);
			experimentalTransitRoute.setTravelTime(travelTime);
			experimentalTransitRoute.setDistance(
					RouteUtils.calcDistance(experimentalTransitRoute, transitSchedule, network));
			leg.setRoute(experimentalTransitRoute);
		} else if ((pendingVehicleTravel = vehicleTravels.remove(event.getPersonId())) != null) {
			VehicleRoute vehicleRoute = pendingVehicleTravel.route;
			List<Id<Link>> traveledLinks = vehicleRoute.links.subList(pendingVehicleTravel.accessLinkIdx,
					vehicleRoute.links.size());
			Route route;
			if (traveledLinks.isEmpty()) {//special case: enter and leave vehicle without entering traffic
				route = RouteUtils.createGenericRouteImpl(experiencedRoute.get(0), event.getLinkId());
				route.setDistance(0.0);
			} else {
				route = RouteUtils.createNetworkRoute(traveledLinks, null);
				double relPosOnDepartureLink = pendingVehicleTravel.relativePositionOnDepartureLink;
				double relPosOnArrivalLink = vehicleRoute.relativePositionOnLastArrivalLink;
				route.setDistance(
						RouteUtils.calcDistance((NetworkRoute)route, relPosOnDepartureLink, relPosOnArrivalLink,
								network));
			}
			route.setTravelTime(travelTime);
			leg.setRoute(route);

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
		LineAndRoute lineAndRoute = new LineAndRoute(event.getTransitLineId(), event.getTransitRouteId(),
				event.getDriverId());
		transitVehicle2currentRoute.put(event.getVehicleId(), lineAndRoute);
	}

	public void addLegHandler(LegHandler legHandler) {
		this.legHandlers.add(legHandler);
	}
}
