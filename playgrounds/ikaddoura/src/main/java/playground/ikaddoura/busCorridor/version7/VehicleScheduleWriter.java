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
package playground.ikaddoura.busCorridor.version7;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author Ihab
 *
 */
public class VehicleScheduleWriter {
	
	private final static Logger log = Logger.getLogger(Operator.class);
	private int numberOfBuses;
	private String networkFile;
	private String directoryExtIt;
	private int capacity;
	private double length;
	private double egressSeconds;
	private double accessSeconds;
	private int busSeats;
	private int standingRoom;
	
	public VehicleScheduleWriter(int numberOfBuses, int capacity, String networkFile, String directoryExtIt) {
	
	if (numberOfBuses <= 0){
		log.info("At least one bus expected, number of buses set to 1.");
		this.numberOfBuses = 1;
	}
	else {
		this.numberOfBuses = numberOfBuses;
	}
		
		this.networkFile = networkFile;
		this.directoryExtIt = directoryExtIt;
		this.capacity = capacity;
		this.length = getLength();
		this.standingRoom = getStandingRoom();
		this.busSeats = getBusSeats();
	}

	private int getBusSeats() {
		double busSeats = this.capacity * 0.7; 
		return (int) busSeats;
	}

	private int getStandingRoom() {
		double standingRoom = this.capacity * 0.3;
		return (int) standingRoom;
	}

	private double getLength() {
		double length = 0.1184 * this.capacity + 5.2152; // siehe lineare Regressionsanalyse in "BusCostsEstimations.xls"
		return length;
	}

	public void writeTransit() throws IOException {
		
		ScheduleVehiclesGenerator generator = new ScheduleVehiclesGenerator();
		generator.setStopTime(20); // for schedule schedule!
		generator.setNetworkFile(networkFile);
		generator.setScheduleFile(this.directoryExtIt+"/scheduleFile.xml");
		generator.setVehicleFile(this.directoryExtIt+"/vehiclesFile.xml");
		
		generator.setTransitLineId(new IdImpl("Bus Line"));
		generator.setRouteId1(new IdImpl("West-Ost"));
		generator.setRouteId2(new IdImpl("Ost-West"));
		
		generator.setVehTypeId(new IdImpl("Bus"));
		generator.setAccessSeconds(1.0); // seconds per person for entering a vehicle 
		generator.setEgressSeconds(1.0); // seconds per person for leaving a vehicle
		generator.setSeats(busSeats);
		generator.setStandingRoom(standingRoom);
		generator.setLength(length);
		
		generator.setNumberOfBusses(this.numberOfBuses);
		generator.setStartTime(7*3600); // first cycle Time
		generator.setEndTime(18*3600); // latest begin of cycle Time, bus will finish cycle after that time
		
		generator.createVehicles();
		generator.createSchedule();
		
		generator.writeScheduleFile();
		generator.writeVehicleFile();		
	}

}
