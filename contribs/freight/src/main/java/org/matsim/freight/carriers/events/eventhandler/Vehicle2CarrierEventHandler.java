/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */
package org.matsim.freight.carriers.events.eventhandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.CarrierTourStartEvent;
import org.matsim.vehicles.Vehicle;

/**
 * Basic event handler that collects the relation between vehicles and carrier.
 * Necessary since link enter and leave events do not contain any information of the carrier.
 * For the connection between driver and Vehicle the {@link Vehicle2CarrierEventHandler} can be used.
 *
 * @author kturner
 */
public final class Vehicle2CarrierEventHandler implements CarrierTourStartEventHandler, CarrierTourEndEventHandler {


	// Comment from Janek (in https://github.com/matsim-org/matsim-libs/pull/2128)
	// Is this handler ever going to be called concurrently? If not a normal HashMap is probably sufficient
	// At least the default events manager guarantees single threaded invocation of your handler.
	// --> we can check this. Currently, it was made only analogous to Vehicle2DriverEventHandler  kmt sep'22
	private final Map<Id<Vehicle>, Id<Carrier>> carrierVehicles = new ConcurrentHashMap<>();

	@Override
	public void reset(int iteration) {
		carrierVehicles.clear();
	}

	@Override
	public void handleEvent(CarrierTourStartEvent event) {
		carrierVehicles.put(event.getVehicleId(), event.getCarrierId());
	}

	@Override
	public void handleEvent(CarrierTourEndEvent event) {
		carrierVehicles.remove(event.getVehicleId());
	}

	/**
	 * @param vehicleId the unique vehicle identifier.
	 * @return person id of the driver
	 */
	public Id<Carrier> getCarrierOfVehicle(Id<Vehicle> vehicleId){
		return carrierVehicles.get(vehicleId);
	}
}
