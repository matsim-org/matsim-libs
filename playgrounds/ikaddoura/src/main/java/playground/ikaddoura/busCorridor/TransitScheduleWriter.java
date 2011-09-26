/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleWriter.java
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
package playground.ikaddoura.busCorridor;

import java.io.IOException;

import org.matsim.core.basic.v01.IdImpl;

/**
 * @author Ihab
 *
 */
public class TransitScheduleWriter {

	public static void main(String[] args) throws IOException {
		BusCorridorTransitScheduleGenerator generator = new BusCorridorTransitScheduleGenerator();
		generator.setStopTime(30);
		generator.setTravelTimeBus(3*60);
		generator.setNetworkFile("../../shared-svn/studies/ihab/busCorridor/input/network_busline.xml");
		generator.setScheduleFile("../../shared-svn/studies/ihab/busCorridor/input/transitschedule10buses.xml");
		generator.setVehicleFile("../../shared-svn/studies/ihab/busCorridor/input/transitVehicles10buses.xml");
		
		generator.setTransitLineId(new IdImpl("Bus Line"));
		generator.setRouteId1(new IdImpl("West-Ost"));
		generator.setRouteId2(new IdImpl("Ost-West"));
		
		generator.setVehTypeId(new IdImpl("Bus"));
		generator.setSeats(15);
		generator.setStandingRoom(20);
		
		generator.setNumberOfBusses(10); // Anzahl der Busse
		generator.setStartTime(7*3600);
		generator.setEndTime(17*3600);
		
		generator.createVehicles();
		generator.createSchedule();
		
		generator.writeScheduleFile();
		generator.writeVehicleFile();
	}

}
