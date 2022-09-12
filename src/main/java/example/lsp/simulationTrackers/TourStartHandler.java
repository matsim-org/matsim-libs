/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package example.lsp.simulationTrackers;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightTourStartEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/*package-private*/ class TourStartHandler implements FreightTourStartEventHandler {

	private static final Logger log = LogManager.getLogger(TourStartHandler.class);

	private double vehicleFixedCosts;

	@Inject Scenario scenario;

	@Override
	public void reset(int iteration) {
		vehicleFixedCosts = 0;
	}

	@Override
	public void handleEvent(FreightTourStartEvent event) {
		log.warn("handling tour start event=" + event);
		//final Vehicle vehicle = (Vehicle) event.getVehicle();
		final Vehicle vehicle = VehicleUtils.findVehicle(event.getVehicleId(), scenario);
		vehicleFixedCosts = vehicleFixedCosts + vehicle.getType().getCostInformation().getFixedCosts();

	}

	public double getVehicleFixedCosts() {
		return vehicleFixedCosts;
	}

}
