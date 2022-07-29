/*
 *  *********************************************************************** *
 *  * project: org.matsim.*													  *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING, 		  *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.freight.events.eventhandler;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.events.FreightTourEndEvent;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic event handler that collects the relation between vehicles and carrier.
 * Necessary since link enter and leave events do not contain any information of the carrier.
 * For the connection between driver and Vehicle the {@link Vehicle2CarrierEventHandler} can be used.
 * 
 * @author kturner
 */
public final class Vehicle2CarrierEventHandler implements FreightTourStartEventHandler, FreightTourEndEventHandler {

	private final Map<Id<Vehicle>, Id<Carrier>> carrierVehicles = new ConcurrentHashMap<>();
	
	@Override
	public void reset(int iteration) {
		carrierVehicles.clear();
	}

	@Override
	public void handleEvent(FreightTourStartEvent event) {
		carrierVehicles.put(event.getVehicleId(), event.getCarrierId());
	}

	@Override
	public void handleEvent(FreightTourEndEvent event) {
		carrierVehicles.remove(event.getVehicle().getId());
	}

	/**
	 * @param vehicleId the unique vehicle identifier.
	 * @return person id of the driver
	 */
	public Id<Carrier> getCarrierOfVehicle(Id<Vehicle> vehicleId){
		return carrierVehicles.get(vehicleId);
	}
}
