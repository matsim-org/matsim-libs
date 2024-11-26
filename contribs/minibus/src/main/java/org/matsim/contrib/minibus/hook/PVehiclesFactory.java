/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.hook;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.*;

/**
 * Generates vehicles for a whole transit schedule
 *
 * @author aneumann
 *
 */
class PVehiclesFactory {

	@SuppressWarnings("unused")
	private final static Logger log = LogManager.getLogger(PVehiclesFactory.class);

	private final PConfigGroup pConfig;

	public PVehiclesFactory(PConfigGroup pConfig) {
		this.pConfig = pConfig;
	}

	/**
	 * Create vehicles for each departure of the given transit schedule.
	 *
	 * @return Vehicles used by paratranit lines
	 */
	public Vehicles createVehicles(TransitSchedule pTransitSchedule){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehiclesFactory vehFactory = vehicles.getFactory();
		VehicleType vehType = vehFactory.createVehicleType(Id.create(this.pConfig.getPIdentifier(), VehicleType.class));
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(this.pConfig.getPaxPerVehicle()); // 2018-11 the driver no longer takes one seat
		vehType.getCapacity().setStandingRoom(0);
//		vehType.setCapacity(capacity);
		vehType.setPcuEquivalents(this.pConfig.getPassengerCarEquivalents());
		vehType.setMaximumVelocity(this.pConfig.getVehicleMaximumVelocity());
		vehType.setNetworkMode(TransportMode.car);
        VehicleUtils.setAccessTime(vehType, this.pConfig.getDelayPerBoardingPassenger());
		VehicleUtils.setEgressTime(vehType, this.pConfig.getDelayPerAlightingPassenger());
		VehicleUtils.setDoorOperationMode(vehType, this.pConfig.getDoorOperationMode()) ;
		vehicles.addVehicleType( vehType);

		for (TransitLine line : pTransitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (!vehicles.getVehicles().keySet().contains(departure.getVehicleId())) {
						Vehicle vehicle = vehFactory.createVehicle(departure.getVehicleId(), vehType);
						vehicles.addVehicle( vehicle);
					}
				}
			}
		}

		return vehicles;
	}
}
