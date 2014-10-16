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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;

/**
 * @author Ihab
 *
 */
public class VehicleScheduleWriter {

	int numberOfBuses;
	int capacity;
	Network network;
	String outputDir;
	
	double length;
	int busSeats;
	int standingRoom;
	
	private double headway;
	
	public VehicleScheduleWriter(int numberOfBuses, int capacity, Network network, String outputDir) {
		this.numberOfBuses = numberOfBuses;
		this.capacity = capacity;
		this.network = network;
		this.outputDir = outputDir;
		
		this.length = (0.1184 * this.capacity + 5.2152) + 2.;	// see linear regression analysis in "BusCostsEstimations.xls", + 2m distance (before/behind)
		this.busSeats = (int) (this.capacity * 1.) + 1; // plus one seat because a seat for the driver is expected
		this.standingRoom = (int) (this.capacity * 0.); // for future functionality (e.g. disutility for standing in bus)
	}

	public void writeTransitVehiclesAndSchedule() throws IOException {
		
		VehicleScheduleGenerator generator = new VehicleScheduleGenerator();
		
		generator.setNumberOfBuses(numberOfBuses);
		generator.setStartTime(4.0 * 3600);	// [sec]
		generator.setEndTime(24.0 * 3600);	// [sec]
		generator.setStopTime(10.0); 		// [sec]
		generator.setScheduleSpeed(8.3333);	// [m/sec] 
		generator.setPausenzeit(5.0 * 60); 	// [sec]
		generator.setNetwork(this.network);
		generator.setScheduleFile(this.outputDir + "/scheduleFile.xml");
		generator.setVehicleFile(this.outputDir + "/vehiclesFile.xml");
		
		generator.setTransitLineId(Id.create("busLine", TransitLine.class));
		generator.setRouteId1(Id.create("west-east", TransitRoute.class));
		generator.setRouteId2(Id.create("east-west", TransitRoute.class));
		
		generator.setVehTypeId(Id.create("bus", VehicleType.class));
		generator.setAccessSeconds(2.0); 	// [sec/person]
		generator.setEgressSeconds(1.5); 	// [sec/person]
		generator.setDoorOperationMode(DoorOperationMode.parallel);
		generator.setSeats(busSeats);
		generator.setStandingRoom(standingRoom);
		generator.setLength(length);
		
		generator.createVehicles();
		generator.createSchedule();
		
		generator.writeScheduleFile();
		generator.writeVehicleFile();
		
		this.headway = generator.getHeadway();
	}

	public double getHeadway() {
		return headway;
	}
}