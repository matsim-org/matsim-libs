/*
 *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       :  (C) 2022 by the members listed in the COPYING,       *
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
 * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceEndEventHandler;
import org.matsim.freight.carriers.events.eventhandler.CarrierServiceStartEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * Example {@link org.matsim.core.events.handler.EventHandler} that tracks, for all carrier
 * services of a simulation run, the total number of shipments, their total weight, and the total
 * loading costs incurred by service durations.
 */
/*package-private*/ class CollectionServiceHandler
	implements CarrierServiceStartEventHandler, CarrierServiceEndEventHandler {

	private final Collection<ServiceTuple> tuples;
	private final Vehicles allVehicles;
	private double totalLoadingCosts;
	private int totalNumberOfShipments;
	private int totalWeightOfShipments;

	public CollectionServiceHandler(Scenario scenario) {
		this.allVehicles = VehicleUtils.getOrCreateAllvehicles(scenario);
		this.tuples = new ArrayList<>();
	}

	/**
	 * Clears the accumulated counters and the recorded service tuples for a new iteration.
	 *
	 * @param iteration the current iteration (used only to trigger the reset)
	 */
	@Override
	public void reset(int iteration) {
		tuples.clear();
		totalNumberOfShipments = 0;
		totalWeightOfShipments = 0;
	}

	/**
	 * Completes the service recorded for the matching {@link #handleEvent(CarrierServiceStartEvent)}
	 * and adds the resulting loading cost (duration times per-second vehicle cost) to the running
	 * total.
	 *
	 * @param event the carrier service end event
	 */
	@Override
	public void handleEvent(CarrierServiceEndEvent event) {
		System.out.println("Service Ends");
		double loadingCosts;
		for (ServiceTuple tuple : tuples) {
			if (tuple.serviceId() == event.getServiceId()) {
				double serviceDuration = event.getTime() - tuple.startTime();

				final Vehicle vehicle = allVehicles.getVehicles().get(event.getVehicleId());
				loadingCosts = serviceDuration * vehicle.getType().getCostInformation().getCostsPerSecond();
				totalLoadingCosts = totalLoadingCosts + loadingCosts;
				tuples.remove(tuple);
				break;
			}
		}
	}

	@Override
	public void handleEvent(CarrierServiceStartEvent event) {
		totalNumberOfShipments++;
		totalWeightOfShipments = totalWeightOfShipments + event.getCapacityDemand();
		tuples.add(new ServiceTuple(event.getServiceId(), event.getTime()));
	}

	public double getTotalLoadingCosts() {
		return totalLoadingCosts;
	}

	public int getTotalNumberOfShipments() {
		return totalNumberOfShipments;
	}

	public int getTotalWeightOfShipments() {
		return totalWeightOfShipments;
	}

	/** Pairing of a service id with the time at which the service started. */
	private record ServiceTuple(Id<CarrierService> serviceId, double startTime) {
	}
}
