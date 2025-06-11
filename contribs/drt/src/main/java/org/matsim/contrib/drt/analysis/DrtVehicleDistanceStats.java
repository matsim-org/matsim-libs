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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.analysis.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Preconditions;

/**
 * @author jbischoff
 * @author Michal Maciejewski
 */
public class DrtVehicleDistanceStats
		implements PassengerPickedUpEventHandler, LinkEnterEventHandler, PassengerDroppedOffEventHandler,
		TeleportationArrivalEventHandler {

	static class VehicleState {
		final Map<Id<Person>, MutableDouble> distanceByPersonId = new HashMap<>();
		double totalDistance = 0;
		double totalOccupiedDistance = 0;
		double totalPassengerTraveledDistance = 0; //in (passenger x meters)
		final double[] totalDistanceByOccupancy;
		final double serviceDuration;

		private VehicleState(int maxCapacity, double serviceTime) {
			this.totalDistanceByOccupancy = new double[maxCapacity + 1];
			this.serviceDuration = serviceTime;
		}

		private void linkEntered(Link link) {
			double linkLength = link.getLength();
			distanceByPersonId.values().forEach(distance -> distance.add(linkLength));
			totalDistance += linkLength;
			int occupancy = distanceByPersonId.size();
			if (occupancy > 0) {
				totalOccupiedDistance += linkLength;
				totalPassengerTraveledDistance += linkLength * occupancy;
			}

			if (occupancy < totalDistanceByOccupancy.length) {
				totalDistanceByOccupancy[occupancy] += linkLength;
			}
		}
	}

	private final Map<Id<Vehicle>, VehicleState> vehicleStates = new HashMap<>();
	private final Map<Id<Request>, Double> travelDistances = new HashMap<>();

	private final String mode;
	private final Network network;
	private final FleetSpecification fleetSpecification;
	private final DvrpLoadType loadType;

	public DrtVehicleDistanceStats(Network network, DrtConfigGroup drtCfg, FleetSpecification fleetSpecification, DvrpLoadType loadType) {
		this.mode = drtCfg.getMode();
		this.network = network;
		this.fleetSpecification = fleetSpecification;
		this.loadType = loadType;
		initializeVehicles();
	}

	@Override
	public void reset(int iteration) {
		vehicleStates.clear();
		initializeVehicles();
	}

	private void initializeVehicles() {
		int maxCapacity = VehicleOccupancyProfileCalculator.findMaxVehicleCapacity(fleetSpecification, loadType);
		fleetSpecification.getVehicleSpecifications()
				.values()
				.stream()
				.forEach(spec -> vehicleStates.put(Id.createVehicleId(spec.getId()),
					new VehicleState(maxCapacity, spec.getServiceEndTime() - spec.getServiceBeginTime())));
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent event) {
		if (event.getMode().equals(mode)) {
			if (event.getVehicleId() != null) {
				vehicleStates.get(Id.createVehicleId(event.getVehicleId())).distanceByPersonId.put(event.getPersonId(),
						new MutableDouble());
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		VehicleState vehicleState = vehicleStates.get(event.getVehicleId());
		if (vehicleState != null) {
			vehicleState.linkEntered(network.getLinks().get(event.getLinkId()));
		}
	}

	private final Map<Id<Person>, Id<Request>> soonArrivingTeleportedRequests = new HashMap<>();

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		if (event.getMode().equals(mode)) {
			if (event.getVehicleId() != null) {
				double distance = vehicleStates.get(Id.createVehicleId(event.getVehicleId())).distanceByPersonId.remove(
						event.getPersonId()).doubleValue();
				travelDistances.put(event.getRequestId(), distance);
			} else {
				Preconditions.checkArgument(
						soonArrivingTeleportedRequests.put(event.getPersonId(), event.getRequestId()) == null,
						"Duplicate entry for arriving passenger: (%s)", event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (event.getMode().equals(mode)) {
			Id<Request> requestId = Objects.requireNonNull(soonArrivingTeleportedRequests.remove(event.getPersonId()));
			travelDistances.put(requestId, event.getDistance());
		}
	}

	/**
	 * @return the vehicleDistances
	 */
	Map<Id<Vehicle>, VehicleState> getVehicleStates() {
		return vehicleStates;
	}

	Map<Id<Request>, Double> getTravelDistances() {
		return travelDistances;
	}
}
