/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler.java
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
package playground.ikaddoura.busCorridor.prepare.scheduleFromNetwork;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ihab
 *
 */

public class Main {
		
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/test_network.xml";
	static String inputPath = "../../shared-svn/studies/ihab/parkAndRide/input";
	
	// settings for first iteration or if values not changed for all iterations
	TimePeriod p1 = new TimePeriod(1, "DAY", 5, 4*3600, 24*3600); // orderId, id, numberOfBuses, fromTime, toTime
//	TimePeriod p2 = new TimePeriod(2, "HVZ_1", 12, 6*3600, 12*3600);
//	TimePeriod p3 = new TimePeriod(3, "NVZ", 8, 12*3600, 15*3600);
//	TimePeriod p4 = new TimePeriod(4, "HVZ_2", 12, 15*3600, 21*3600);
//	TimePeriod p5 = new TimePeriod(5, "SVZ_2", 4, 21*3600, 23*3600);

	
	private Map<Integer, TimePeriod> day = new HashMap<Integer, TimePeriod>();

	public static void main(final String[] args) throws IOException {
		Main simulation = new Main();
		simulation.externalIteration();
	}
	
	private void externalIteration() throws IOException {
		
		day.put(p1.getOrderId(), p1);
//		day.put(p2.getOrderId(), p2);
//		day.put(p3.getOrderId(), p3);
//		day.put(p4.getOrderId(), p4);
//		day.put(p5.getOrderId(), p5);
		
		VehicleScheduleWriter transitWriter = new VehicleScheduleWriter(this.day, 50, networkFile, inputPath);
		transitWriter.writeTransit();
			
	}
}
