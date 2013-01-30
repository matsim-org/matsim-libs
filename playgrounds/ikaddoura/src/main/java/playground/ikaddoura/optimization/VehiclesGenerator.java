/* *********************************************************************** *
 * project: org.matsim.*
 * BusCorridorScheduleVehiclesGenerator.java
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
package playground.ikaddoura.optimization;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author Ihab
 *
 */
public class VehiclesGenerator {
	private final static Logger log = Logger.getLogger(VehiclesGenerator.class);

	private Id vehTypeId;
	private double egressSeconds;
	private double accessSeconds;
	private DoorOperationMode doorOperationMode;
	private List<Id> vehicleIDs = new ArrayList<Id>();	
	private Vehicles veh = VehicleUtils.createVehiclesContainer();

	public void createVehicles(int numberOfBuses, int busSeats, int standingRoom, double length) {
		
		VehicleType type = veh.getFactory().createVehicleType(this.vehTypeId);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(busSeats);
		cap.setStandingRoom(standingRoom);
		type.setCapacity(cap);
		type.setLength(length);
		type.setAccessTime(accessSeconds);
		type.setEgressTime(egressSeconds);
		type.setDoorOperationMode(doorOperationMode);
		
		veh.getVehicleTypes().put(this.vehTypeId, type); 
		
		for (int vehicleNr=1 ; vehicleNr <= numberOfBuses ; vehicleNr++){
			vehicleIDs.add(new IdImpl("bus_"+vehicleNr));
		}

		if (vehicleIDs.isEmpty()){
			throw new RuntimeException("At least 1 Bus is expected. Aborting...");
		} else {
			for (Id vehicleId : vehicleIDs){
				Vehicle vehicle = veh.getFactory().createVehicle(vehicleId, veh.getVehicleTypes().get(vehTypeId));
				veh.getVehicles().put(vehicleId, vehicle);
			}
		}
	}
	
	public void writeVehicleFile(String vehicleFile) {
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(veh);
		vehicleWriter.writeFile(vehicleFile);
	}

	public void setVehTypeId(Id vehTypeId) {
		this.vehTypeId = vehTypeId;
	}

	public void setEgressSeconds(double egressSeconds) {
		this.egressSeconds = egressSeconds;
	}

	public void setAccessSeconds(double accessSeconds) {
		this.accessSeconds = accessSeconds;
	}

	public void setDoorOperationMode(DoorOperationMode doorOperationMode) {
		this.doorOperationMode = doorOperationMode;
	}
	
}