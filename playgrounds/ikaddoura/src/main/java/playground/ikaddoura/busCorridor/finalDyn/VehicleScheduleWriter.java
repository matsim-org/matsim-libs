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
package playground.ikaddoura.busCorridor.finalDyn;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author Ihab
 *
 */
public class VehicleScheduleWriter {
	
	private final static Logger log = Logger.getLogger(Operator.class);
	private String networkFile;
	private String directoryExtIt;
	private int capacity;
	private double length;
	private int busSeats;
	private int standingRoom;
	private TransitSchedule schedule;
	private List<Id> vehicleIDs;
	private Map<Integer, TimePeriod> day;
	private double pausenzeit;
	
	public VehicleScheduleWriter(Map<Integer, TimePeriod> day, int capacity, String networkFile, String directoryExtIt) {
		
		this.networkFile = networkFile;
		this.directoryExtIt = directoryExtIt;
		this.capacity = capacity;
		this.length = getLength();
		this.standingRoom = getStandingRoom();
		this.busSeats = getBusSeats();
		this.day = day;
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
	
		generator.setStopTime(20); // for schedule!
		generator.setScheduleSpeed(8.33333); // for schedule!
		generator.setPausenzeit(5*60);
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
		
		generator.setDay(day);		
		
		generator.createSchedule();
		
		generator.writeScheduleFile();
		generator.writeVehicleFile();
	}

	public TransitSchedule getSchedule() {
		return schedule;
	}
	
	public List<Id> getVehicleIDs() {
		return vehicleIDs;
	}

}
