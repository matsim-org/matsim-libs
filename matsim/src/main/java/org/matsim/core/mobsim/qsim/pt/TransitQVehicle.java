/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;


public class TransitQVehicle extends QVehicleImpl implements TransitVehicle {

	private TransitStopHandler stopHandler;

	public TransitQVehicle(final Vehicle basicVehicle) {
		super(basicVehicle);
		
		VehicleCapacity capacity = basicVehicle.getType().getCapacity();
		if (capacity == null) {
			throw new NullPointerException("No capacity set in vehicle type.");
		}
		// New default is that vehicle is created with capacity. Initial values are 1 seat for the driver and no standing room {@link org.matsim.vehicles.VehicleCapacity} (sep 19, KMT)
		// PT does not count the driver, so only warn if there is indeed no real capacity (mar 21, mrieser)
		if (capacity.getSeats() == 0 && capacity.getStandingRoom() == 0) {
			throw new NullPointerException("No capacity set in vehicle type.");
		}
	}
	
	public void setStopHandler(TransitStopHandler stopHandler) {
		this.stopHandler = stopHandler;
	}

	@Override
	public TransitStopHandler getStopHandler() {
		return this.stopHandler;
	}

}
