/* *********************************************************************** *
 * project: org.matsim.*
 * AddVehicleToContainer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.freight.digicore.io.algorithms;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;

/**
 * The default algorithm to simply add a single {@link DigicoreVehicle} to a
 * given {@link DigicoreVehicles} container.
 * 
 * @author jwjoubert
 */
public class AddVehicleToContainerAlgorithm implements DigicoreVehiclesAlgorithm {
	private DigicoreVehicles vehicles;
	
	public AddVehicleToContainerAlgorithm(DigicoreVehicles vehicles) {
		if(vehicles == null){
			throw new IllegalArgumentException("Vehicles container for this algorithm cannot be null.");
		}
		
		this.vehicles = vehicles;
	}
	
	
	/**
	 * Adds the given vehicle to the container specified during instantiation.
	 * 
	 * @param vehicle to be added to the container.
	 */
	@Override
	public void apply(DigicoreVehicle vehicle){
		this.vehicles.addDigicoreVehicle(vehicle);
	}

}
