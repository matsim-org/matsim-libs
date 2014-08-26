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

package playground.andreas.P2.hook;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import playground.andreas.P2.helper.PConfigGroup;

/**
 * Generates vehicles for a whole transit schedule
 * 
 * @author aneumann
 *
 */
public class PVehiclesFactory {
	private final static Logger log = Logger.getLogger(PVehiclesFactory.class);
	
	private PConfigGroup pConfig;

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
		VehicleType vehType = vehFactory.createVehicleType(new IdImpl(this.pConfig.getPIdentifier()));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(this.pConfig.getPaxPerVehicle() + 1)); // july 2011 the driver takes one seat
		capacity.setStandingRoom(Integer.valueOf(0));
		vehType.setCapacity(capacity);
		vehType.setPcuEquivalents(this.pConfig.getPassengerCarEquivalents());
		vehType.setMaximumVelocity(this.pConfig.getVehicleMaximumVelocity());
		vehType.setAccessTime(this.pConfig.getDelayPerBoardingPassenger());
		vehType.setEgressTime(this.pConfig.getDelayPerAlightingPassenger());
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
