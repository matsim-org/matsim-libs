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
package playground.ikaddoura.optimization;

import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

/**
 * 
 * @author Ihab
 *
 */
public class ScheduleWriter {

	private Network network;
	private double umlaufzeit;
	
	public ScheduleWriter(Network network) {
		this.network = network;
	}

	public void createSchedule(int numberOfBuses, String outputFile) throws IOException {
		
		ScheduleGenerator generator = new ScheduleGenerator();
		generator.setStartTime(4.0 * 3600);		// [sec]
		generator.setEndTime(24.0 * 3600);		// [sec]
		generator.setStopTime(10.0); 			// [sec]
		generator.setScheduleSpeed(8.3333333);	// [m/sec] 
		generator.setPausenzeit(5.0 * 60); 		// [sec]
		generator.setNetwork(this.network);
		generator.setTransitLineId(new IdImpl("busLine"));
		generator.setRouteId1(new IdImpl("west-east"));
		generator.setRouteId2(new IdImpl("east-west"));
		
		generator.createSchedule(numberOfBuses);
		generator.writeScheduleFile(outputFile);
		
		this.umlaufzeit = generator.getUmlaufzeit();
	}

	public double getUmlaufzeit() {
		return umlaufzeit;
	}
}