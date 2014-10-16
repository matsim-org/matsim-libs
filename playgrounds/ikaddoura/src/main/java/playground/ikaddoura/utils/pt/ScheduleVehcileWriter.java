/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author ikaddoura
 *
 */
public class ScheduleVehcileWriter {

	private String outputDirectory;

	private TransitSchedule schedule;
	private String scheduleFile;

	private String networkFile;
	private String transitRouteMode;
	private boolean isBlocking;
	private boolean awaitDeparture;
	private double stopTime_sec;
	private double scheduleSpeed_m_sec;
	
	private double headway_sec;
	private double startTime;
	private double endTime;
	private double pausenzeit;
	
	private Vehicles vehicles;
	private String vehicleFile;
	private int busSeats;
	private int standingRoom;
	private double length;
	private Id vehTypeId;
	private double egressSeconds;
	private double accessSeconds;
	private DoorOperationMode doorOperationMode;
	private double pcu;
	private double maxVelocity;
	
	public static void main(String[] args) {

		
		ScheduleVehcileWriter svw = new ScheduleVehcileWriter();
		
		svw.setOutputDirectory("/Users/Ihab/Desktop/");
		svw.setNetworkFile("/Users/Ihab/Desktop/network_mixed.xml");
		svw.setScheduleFile("transitSchedule.xml");
		svw.setVehiclesFile("transitVehicles.xml");
		svw.setHeadway_sec(3600.);
		svw.setBusSeats(61);
		svw.setStandingRoom(0);
		svw.setLength(15);
		svw.setMaxVelocity(8.34); // default: Double.POSITIVE_INFINITY
		svw.setPcu(2.5); // if 0 the pcu are calculated: pcu = (length+3m) / 7.5m
		
		svw.setStopTime_sec(15);
		svw.setScheduleSpeed_m_sec(8.3333333);
		svw.setAwaitDeparture(true);
		svw.setBlocking(false);
		svw.setTransitRouteMode("bus");
		svw.setPausenzeit(600);
		svw.setStartTime(4 * 3600);
		svw.setEndTime(24 * 3600);
		
//		double length = (0.1184 * capacity + 5.2152);	// see linear regression analysis in "BusCostsEstimations.xls"
//		int busSeats = (int) (capacity * 1.) + 1; // plus one seat because a seat for the driver is expected
//		int standingRoom = (int) (capacity * 0.); // for future functionality (e.g. disutility for standing in bus)
		
		svw.setVehTypeId(Id.create("bus", VehicleType.class));
		svw.setAccessSeconds(2);
		svw.setEgressSeconds(1.5);
		svw.setDoorOperationMode(DoorOperationMode.parallel);
		
		svw.run();
	}

	private void run() {
		
		File directory = new File(this.outputDirectory);
		directory.mkdirs();

		ScheduleFromCorridor sfn = new ScheduleFromCorridor(this.networkFile);
		sfn.createTransitSchedule(this.transitRouteMode, this.isBlocking, this.awaitDeparture, this.scheduleSpeed_m_sec, this.stopTime_sec);
		this.schedule = sfn.getTransitSchedule();

		List<Id<TransitLine>> lineIDs = new ArrayList<>();
		lineIDs.addAll(this.schedule.getTransitLines().keySet());
		
		DeparturesGenerator dg = new DeparturesGenerator();
		dg.addDepartures(this.schedule, lineIDs, this.headway_sec, this.startTime, this.endTime, this.pausenzeit);

		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(this.schedule);
		scheduleWriter.write(this.outputDirectory + this.scheduleFile);
		
		// create Vehicles for each line
		VehiclesGenerator vg = new VehiclesGenerator();
		vg.createVehicles(this.schedule, lineIDs, this.busSeats, this.standingRoom, this.length, this.vehTypeId, this.egressSeconds, this.accessSeconds, this.doorOperationMode, this.pcu, this.maxVelocity);
		this.vehicles = vg.getVehicles();
		
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(this.vehicles);
		vehicleWriter.writeFile(this.outputDirectory + this.vehicleFile);
		
	}

	public void setHeadway_sec(double headway_sec) {
		this.headway_sec = headway_sec;
	}

	public void setNetworkFile(String networkFile) {
		this.networkFile = networkFile;
	}
	
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	
	public void setScheduleFile(String scheduleFile) {
		this.scheduleFile = scheduleFile;
	}
	
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public void setPausenzeit(double pausenzeit) {
		this.pausenzeit = pausenzeit;
	}

	public void setStopTime_sec(double stopTime_sec) {
		this.stopTime_sec = stopTime_sec;
	}
	
	public void setScheduleSpeed_m_sec(double scheduleSpeed_m_sec) {
		this.scheduleSpeed_m_sec = scheduleSpeed_m_sec;
	}

	public void setTransitRouteMode(String transitRouteMode) {
		this.transitRouteMode = transitRouteMode;
	}

	public void setBlocking(boolean isBlocking) {
		this.isBlocking = isBlocking;
	}

	public void setAwaitDeparture(boolean awaitDeparture) {
		this.awaitDeparture = awaitDeparture;
	}

	public void setVehicleFile(String vehicleFile) {
		this.vehicleFile = vehicleFile;
	}

	public void setBusSeats(int busSeats) {
		this.busSeats = busSeats;
	}

	public void setStandingRoom(int standingRoom) {
		this.standingRoom = standingRoom;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public void setVehTypeId(Id<VehicleType> vehTypeId) {
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
	
	public void setVehiclesFile(String vehiclesFile) {
		this.vehicleFile = vehiclesFile;
	}

	public void setPcu(double pcu) {
		this.pcu = pcu;
	}

	public void setMaxVelocity(double maxVelocity) {
		this.maxVelocity = maxVelocity;
	}
		
}
