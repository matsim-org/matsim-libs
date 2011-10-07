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
package playground.ikaddoura.busCorridor.version4;

import java.io.IOException;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author Ihab
 *
 */
public class VehicleScheduleWriter {
	
	private int numberOfBuses;
	private String networkFile;
	private String directoryExtIt;
	
	public VehicleScheduleWriter(int numberOfBuses, String networkFile, String directoryExtIt) {
		if (numberOfBuses==0){
			System.out.println(" ******************* at least 1 Bus expected --> numberOfBuses set on 1");
			this.numberOfBuses = 1;
		}
		else {
			this.numberOfBuses = numberOfBuses;
		}
		this.networkFile = networkFile;
		this.directoryExtIt = directoryExtIt;
	}

	public void writeTransit() throws IOException {
		
		ScheduleVehiclesGenerator generator = new ScheduleVehiclesGenerator();
		generator.setStopTime(30);
		generator.setTravelTimeBus(3*60);
		generator.setNetworkFile(networkFile);
		generator.setScheduleFile(this.directoryExtIt+"/scheduleFile.xml");
		generator.setVehicleFile(this.directoryExtIt+"/vehiclesFile.xml");
		
		generator.setTransitLineId(new IdImpl("Bus Line"));
		generator.setRouteId1(new IdImpl("West-Ost"));
		generator.setRouteId2(new IdImpl("Ost-West"));
		
		generator.setVehTypeId(new IdImpl("Bus"));
		generator.setSeats(15);
		generator.setStandingRoom(20);
		
		generator.setNumberOfBusses(this.numberOfBuses); // Anzahl der Busse
		generator.setStartTime(7*3600);
		generator.setEndTime(17*3600);
		
		generator.createVehicles();
		generator.createSchedule();
		
		generator.writeScheduleFile();
		generator.writeVehicleFile();		
	}

}
