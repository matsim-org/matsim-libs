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

import org.matsim.core.basic.v01.IdImpl;

/**
 * @author Ihab
 *
 */
class VehicleScheduleWriter {

	String networkFile;
	String outputDir;
	int capacity;
	int numberOfBuses;
	
	double length;
	int busSeats;
	int standingRoom;
	
	public VehicleScheduleWriter(int numberOfBuses, int capacity, String networkFile, String outputDir) {
		this.numberOfBuses = numberOfBuses;
		this.capacity = capacity;
		this.networkFile = networkFile;
		this.outputDir = outputDir;
		
		this.length = 0.1184 * this.capacity + 5.2152;   // see linear regression analysis in "BusCostsEstimations.xls"
		this.busSeats = (int) (this.capacity * 0.7);     // for future functionality (e.g. disutility for standing in bus)
		this.standingRoom = (int) (this.capacity * 0.3); // for future functionality (e.g. disutility for standing in bus)
	}

	public void writeTransitVehiclesAndSchedule() throws IOException {
		
		VehicleScheduleGenerator generator = new VehicleScheduleGenerator();
		
		generator.setNumberOfBuses(numberOfBuses);
		generator.setStartTime(4*3600);
		generator.setEndTime(24*3600);
		generator.setStopTime(10.0); 
		generator.setScheduleSpeed(13.888); // m/s ???
		generator.setPausenzeit(5 * 60);
		generator.setNetworkFile(networkFile);
		generator.setScheduleFile(this.outputDir + "/scheduleFile.xml");
		generator.setVehicleFile(this.outputDir + "/vehiclesFile.xml");
		
		generator.setTransitLineId(new IdImpl("busLine"));
		generator.setRouteId1(new IdImpl("west-east"));
		generator.setRouteId2(new IdImpl("east-west"));
		
		generator.setVehTypeId(new IdImpl("bus"));
		generator.setAccessSeconds(2.0); // seconds per person for entering a vehicle 
		generator.setEgressSeconds(1.5); // seconds per person for leaving a vehicle
		generator.setSeats(busSeats);
		generator.setStandingRoom(standingRoom);
		generator.setLength(length);
		
		generator.createVehicles();
		generator.createSchedule();
		
		generator.writeScheduleFile();
		generator.writeVehicleFile();
	}
}