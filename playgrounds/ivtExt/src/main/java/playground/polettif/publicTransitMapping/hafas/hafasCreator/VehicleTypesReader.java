/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.hafas.hafasCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads all the vehicle types from the file specified.
 *
 * For an example for the required vehicle definition file-structure see:
 * test/input/playground/boescpa/converters/osm/scheduleCreator/TestPTScheduleCreatorDefault/VehicleData.csv.
 *
 * @author boescpa
 */
public class VehicleTypesReader {

	protected static void readVehicles(Vehicles vehicles, String vehicleFile) {
		VehiclesFactory vehicleBuilder = vehicles.getFactory();
		try {
			BufferedReader readsLines = new BufferedReader(new FileReader(vehicleFile));
			// read header 1 and 2
			readsLines.readLine();
			readsLines.readLine();
			// start the actual readout:
			String newLine = readsLines.readLine();
			while (newLine != null) {
				String[] newType = newLine.split(";");
				// The first line without a key breaks the readout.
				if (newType.length == 0) {
					break;
				}
				// Create the vehicle:
				Id<VehicleType> typeId = Id.create(newType[0].trim(), VehicleType.class);
				VehicleType vehicleType = vehicleBuilder.createVehicleType(typeId);
				vehicleType.setLength(Double.parseDouble(newType[1]));
				vehicleType.setWidth(Double.parseDouble(newType[2]));
				vehicleType.setAccessTime(Double.parseDouble(newType[3]));
				vehicleType.setEgressTime(Double.parseDouble(newType[4]));
				if ("serial".matches(newType[5])) {
					vehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.serial);
				} else if ("parallel".matches(newType[5])) {
					vehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.parallel);
				}
				VehicleCapacity vehicleCapacity = vehicleBuilder.createVehicleCapacity();
				vehicleCapacity.setSeats(Integer.parseInt(newType[6]));
				vehicleCapacity.setStandingRoom(Integer.parseInt(newType[7]));
				vehicleType.setCapacity(vehicleCapacity);
				vehicleType.setPcuEquivalents(Double.parseDouble(newType[8]));
				vehicleType.setDescription(newType[9]);
				vehicles.addVehicleType(vehicleType);
				// Read the next line:
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
