/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.drt.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Preconditions;

/**
 * @author jbischoff
 * @author Michal Maciejewski
 */
public class DrtPassengerAndVehicleStats
		implements PersonDepartureEventHandler, DrtRequestSubmittedEventHandler, PersonEntersVehicleEventHandler,
		LinkEnterEventHandler, TeleportationArrivalEventHandler, PersonArrivalEventHandler {

	static class VehicleState {
		final Map<Id<Person>, MutableDouble> distanceByPersonId = new HashMap<>();
		double totalDistance = 0;
		double totalOccupiedDistance = 0;
		double totalRevenueDistance = 0; //in (passenger x meters)
		final double[] totalDistanceByOccupancy;

		private VehicleState(int maxCapacity) {
			totalDistanceByOccupancy = new double[maxCapacity + 1];
		}

		private void linkEntered(Link link) {
			double linkLength = link.getLength();
			distanceByPersonId.values().forEach(distance -> distance.add(linkLength));
			totalDistance += linkLength;
			int occupancy = distanceByPersonId.size();
			if (occupancy > 0) {
				totalOccupiedDistance += linkLength;
				totalRevenueDistance += linkLength * occupancy;
			}
			totalDistanceByOccupancy[occupancy] += linkLength;
		}
	}

	private final Map<Id<Person>, PersonDepartureEvent> departureEvents = new HashMap<>();
	private final Map<Id<Person>, DrtRequestSubmittedEvent> requestSubmittedEvents = new HashMap<>();

	private final List<DrtTrip> drtTrips = new ArrayList<>();
	private final Map<Id<Person>, DrtTrip> currentTrips = new HashMap<>();

	private final Map<Id<Vehicle>, VehicleState> vehicleStates = new HashMap<>();

	private final String mode;
	private final Network network;
	private final FleetSpecification fleetSpecification;

	public DrtPassengerAndVehicleStats(Network network, DrtConfigGroup drtCfg, FleetSpecification fleetSpecification) {
		this.mode = drtCfg.getMode();
		this.network = network;
		this.fleetSpecification = fleetSpecification;

		initializeVehicles();
	}

	@Override
	public void reset(int iteration) {
		departureEvents.clear();
		requestSubmittedEvents.clear();
		drtTrips.clear();
		currentTrips.clear();
		vehicleStates.clear();

		initializeVehicles();
	}

	private void initializeVehicles() {
		int maxCapacity = DrtTripsAnalyser.findMaxVehicleCapacity(fleetSpecification);
		fleetSpecification.getVehicleSpecifications()
				.keySet()
				.stream()
				.map(Id::createVehicleId)
				.forEach(id -> vehicleStates.put(id, new VehicleState(maxCapacity)));
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			Preconditions.checkState(departureEvents.put(event.getPersonId(), event) == null,
					"There is already a departure event associated with this person");
		}
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			Preconditions.checkState(requestSubmittedEvents.put(event.getPersonId(), event) == null,
					"There is already a request associated with this person");
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		PersonDepartureEvent departureEvent = departureEvents.remove(event.getPersonId());
		if (departureEvent != null) {
			double waitTime = event.getTime() - departureEvent.getTime();
			createAndStoreTrip(departureEvent, event.getVehicleId(), waitTime);

			vehicleStates.get(event.getVehicleId()).distanceByPersonId.put(event.getPersonId(), new MutableDouble());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		VehicleState vehicleState = vehicleStates.get(event.getVehicleId());
		if (vehicleState != null) {
			vehicleState.linkEntered(network.getLinks().get(event.getLinkId()));
		}
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		PersonDepartureEvent departureEvent = departureEvents.remove(event.getPersonId());
		if (departureEvent != null) {
			double waitTime = 0;
			DrtTrip trip = createAndStoreTrip(departureEvent, null, waitTime);

			trip.setTravelDistance(event.getDistance());
		}
	}

	private DrtTrip createAndStoreTrip(PersonDepartureEvent event, Id<Vehicle> vehicleId, double waitTime) {
		Coord departureCoord = network.getLinks().get(event.getLinkId()).getCoord();
		DrtRequestSubmittedEvent requestSubmittedEvent = requestSubmittedEvents.remove(event.getPersonId());
		DrtTrip trip = new DrtTrip(event.getTime(), event.getPersonId(), vehicleId, event.getLinkId(), departureCoord,
				waitTime, requestSubmittedEvent.getUnsharedRideDistance(), requestSubmittedEvent.getUnsharedRideTime());

		drtTrips.add(trip);
		Preconditions.checkState(currentTrips.put(event.getPersonId(), trip) == null,
				"There is already an ongoing trip associated with this person");
		return trip;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(mode)) {
			DrtTrip trip = currentTrips.remove(event.getPersonId());
			if (trip.getVehicle() != null) {// not teleported
				double distance = vehicleStates.get(trip.getVehicle()).distanceByPersonId.remove(event.getPersonId())
						.doubleValue();
				trip.setTravelDistance(distance);
			}
			trip.setArrivalTime(event.getTime());
			trip.setToLink(event.getLinkId());
			trip.setToCoord(network.getLinks().get(event.getLinkId()).getCoord());
		}
	}

	/**
	 * @return the drtTrips
	 */
	List<DrtTrip> getDrtTrips() {
		return drtTrips;
	}

	/**
	 * @return the vehicleDistances
	 */
	Map<Id<Vehicle>, VehicleState> getVehicleStates() {
		return vehicleStates;
	}
}
