/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleScheduleWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.utils.pt;

import java.io.IOException;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vehicles.VehicleType.DoorOperationMode;

/**
 * 
 * @author Ihab
 *
 */
public class VehicleWriter {

	public void writeVehicles(int numberOfBuses, int capacity, String outputFile) throws IOException {
		
		double length = (0.1184 * capacity + 5.2152) + 2.;	// see linear regression analysis in "BusCostsEstimations.xls", + 2m distance (before/behind)
		int busSeats = (int) (capacity * 1.) + 1; // plus one seat because a seat for the driver is expected
		int standingRoom = (int) (capacity * 0.); // for future functionality (e.g. disutility for standing in bus)
		
		VehiclesGenerator generator = new VehiclesGenerator();
		generator.setVehTypeId(new IdImpl("bus"));
		generator.setAccessSeconds(2.0); 	// [sec/person]
		generator.setEgressSeconds(1.5); 	// [sec/person]
		generator.setDoorOperationMode(DoorOperationMode.parallel);
		
		generator.createVehicles(numberOfBuses, busSeats, standingRoom, length);
		generator.writeVehicleFile(outputFile);
	}

}