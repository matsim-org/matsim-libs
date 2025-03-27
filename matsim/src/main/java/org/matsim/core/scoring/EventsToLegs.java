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
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
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
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	public static final String ENTER_VEHICLE_TIME_ATTRIBUTE_NAME = "enterVehicleTime";
	public static final String VEHICLE_ID_ATTRIBUTE_NAME = "vehicleId";

	private record PendingTransitTravel(Id<Vehicle> vehicleId, Id<TransitStopFacility> accessStop, double boardingTime) {
	}

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

	private final Network network;
	private TransitSchedule transitSchedule = null;

	@Inject(optional = true)
	public void setTransitSchedule(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;
	}

	private final Map<Id<Person>, Leg> legs = new IdMap<>(Person.class);
	private final Map<Id<Person>, List<Id<Link>>> experiencedRoutes = new IdMap<>(Person.class);
	private final Map<Id<Person>, Double> relPosOnDepartureLinkPerPerson = new IdMap<>(Person.class);
	private final Map<Id<Person>, Double> relPosOnArrivalLinkPerPerson = new IdMap<>(Person.class);

	private final Map<Id<Person>, TeleportationArrivalEvent> routelessTravels = new IdMap<>(Person.class);
	private final Map<Id<Person>, PendingTransitTravel> transitTravels = new IdMap<>(Person.class);
	private final Map<Id<Person>, PendingVehicleTravel> vehicleTravels = new IdMap<>(Person.class);

	private final Map<Id<Vehicle>, LineAndRoute> transitVehicle2currentRoute = new IdMap<>(Vehicle.class);
	private final Map<Id<Vehicle>, VehicleRoute> vehicle2route = new IdMap<>(Vehicle.class);

	private final List<LegHandler> legHandlers = new ArrayList<>();

	public EventsToLegs(Scenario scenario) {
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
		TripStructureUtils.setRoutingMode(leg, event.getRoutingMode());
		leg.setDepartureTime(event.getTime());
		legs.put(event.getPersonId(), leg);

		List<Id<Link>> route = new ArrayList<>();
		route.add(event.getLinkId());
		experiencedRoutes.put(event.getPersonId(), route);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Leg leg = legs.get(event.getPersonId());
		leg.getAttributes().putAttribute(ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, event.getTime());
		leg.getAttributes().putAttribute(VEHICLE_ID_ATTRIBUTE_NAME, event.getVehicleId());
		LineAndRoute lineAndRoute = transitVehicle2currentRoute.get(event.getVehicleId());
		if (lineAndRoute != null) {
			if (!event.getPersonId().equals(lineAndRoute.driverId)) {
				// transit drivers are not considered to travel by transit
				transitTravels.put(event.getPersonId(),
						new PendingTransitTravel(event.getVehicleId(), lineAndRoute.lastFacilityId, event.getTime()));
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
		leg.setTravelTime(event.getTime() - leg.getDepartureTime().seconds());
		double travelTime = leg.getDepartureTime().seconds()
				+ leg.getTravelTime().seconds() - leg.getDepartureTime().seconds();
		leg.setTravelTime(travelTime);
		List<Id<Link>> experiencedRoute = experiencedRoutes.get(event.getPersonId());
		assert !experiencedRoute.isEmpty() : "Experienced route for personId: " + event.getPersonId() + " is empty.";
		PendingTransitTravel pendingTransitTravel;
		PendingVehicleTravel pendingVehicleTravel;
		if (experiencedRoute.size() > 1) { // different links processed
			NetworkRoute networkRoute = RouteUtils.createNetworkRoute(experiencedRoute );
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
			assert lineAndRoute != null : "lineAndRoute for vehicle " + pendingTransitTravel.vehicleId + "is null";

			final TransitStopFacility accessFacility = transitSchedule.getFacilities()
					.get(pendingTransitTravel.accessStop);
			assert accessFacility != null : "accessFacility for " + pendingTransitTravel.accessStop + " is null.";

			final TransitLine line = transitSchedule.getTransitLines().get(lineAndRoute.transitLineId);
			assert line != null : "Transit line for transitLineId " + lineAndRoute.transitLineId + " is null.";

			final TransitRoute route = line.getRoutes().get(lineAndRoute.transitRouteId);
			assert route != null : "route for transitRouteId " + lineAndRoute.transitRouteId + " is null.";

			final Id<TransitStopFacility> lastFacilityId = lineAndRoute.lastFacilityId;
			if (lastFacilityId == null) {
				LogManager.getLogger(this.getClass()).warn("breakpoint");
			}
			assert lastFacilityId != null : "lastFacilityId for " + lineAndRoute + "is null";

			final TransitStopFacility egressFacility = transitSchedule.getFacilities().get(lastFacilityId);
			assert egressFacility != null : "egressFacility for lastFacilityId " + lastFacilityId + " is null.";

			DefaultTransitPassengerRoute passengerRoute = new DefaultTransitPassengerRoute(accessFacility, line, route, egressFacility);
			passengerRoute.setBoardingTime(pendingTransitTravel.boardingTime);
			passengerRoute.setTravelTime(travelTime);
			passengerRoute.setDistance(RouteUtils.calcDistance(passengerRoute, transitSchedule, network));
			leg.setRoute(passengerRoute);
		} else if ((pendingVehicleTravel = vehicleTravels.remove(event.getPersonId())) != null) {
			VehicleRoute vehicleRoute = pendingVehicleTravel.route;
			List<Id<Link>> traveledLinks = vehicleRoute.links.subList(pendingVehicleTravel.accessLinkIdx,
					vehicleRoute.links.size());
			Route route;
			if (traveledLinks.isEmpty()) {//special case: enter and leave vehicle without entering traffic
				route = RouteUtils.createGenericRouteImpl(experiencedRoute.getFirst(), event.getLinkId());
				route.setDistance(0.0);
			} else {
				route = RouteUtils.createNetworkRoute(traveledLinks );
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
			Route genericRoute = RouteUtils.createGenericRouteImpl(experiencedRoute.getFirst(), event.getLinkId());
			genericRoute.setTravelTime(travelTime);
			if (travelEvent != null) {
				genericRoute.setDistance(travelEvent.getDistance());
			} else {
				genericRoute.setDistance(0.0);
			}
			leg.setRoute(genericRoute);
		}

		PersonExperiencedLeg personExperiencedLeg = new PersonExperiencedLeg(event.getPersonId(), leg);
		for (LegHandler legHandler : legHandlers) {
			legHandler.handleLeg(personExperiencedLeg);
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
