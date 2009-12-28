/* *********************************************************************** *
 * project: org.matsim.*
 * MyVehicleIdentifierTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities;

import java.util.ArrayList;

import org.matsim.testcases.MatsimTestCase;


public class MyVehicleIdentifierTest extends MatsimTestCase{
	
	public void testMyVehicleIdentifier(){
		
		String testFile = getInputDirectory() + "testVehicleStats.txt";
		
		// Test the first constructor
		double lowerThreshold = 0.6;
		double upperThreshold = 0.92;
		MyVehicleIdentifier mvi1 = new MyVehicleIdentifier(lowerThreshold, upperThreshold);
		assertEquals("Lower threshold incorrect.", lowerThreshold, mvi1.getLowerThreshold());
		assertEquals("Upper threshold incorrect.", upperThreshold, mvi1.getUpperThreshold());
		assertEquals("Threshold must be null", true, mvi1.getThreshold()==null);
		
		// Test the second constructor
		double threshold = 0.9;
		MyVehicleIdentifier mvi2 = new MyVehicleIdentifier(threshold);
		assertEquals("Lower threshold must be null.", null, mvi2.getLowerThreshold());
		assertEquals("Upper threshold must be null.", null, mvi2.getUpperThreshold());
		assertEquals("Threshold incorrect", threshold, mvi2.getThreshold());
			
		// Test double threshold
		ArrayList<Integer> list = mvi1.buildVehicleList(testFile, ",");
		assertEquals("Wrong number of vehicles.", 1, list.size());
		assertEquals("Wrong vehicle in list.", Integer.valueOf(3), Integer.valueOf(list.get(0)));
		
		// Test single threshold
		ArrayList<ArrayList<Integer>> lists = mvi2.buildVehicleLists(testFile, ",");
		ArrayList<Integer> listWithin = lists.get(0);
		assertEquals("Wrong number of vehicles in 'within' list.", Integer.valueOf(1), Integer.valueOf(listWithin.size()));
		assertEquals("Wrong vehicle in 'within' list.", Integer.valueOf(4), Integer.valueOf(listWithin.get(0)));
		
		ArrayList<Integer> listThrough = lists.get(1);		
		assertEquals("Wrong number of vehicles in 'through' list.", Integer.valueOf(2), Integer.valueOf(listThrough.size()));
		assertEquals("Wrong vehicle in 'through' list.", Integer.valueOf(2), Integer.valueOf(listThrough.get(0)));
		assertEquals("Wrong vehicle in 'through' list.", Integer.valueOf(3), Integer.valueOf(listThrough.get(1)));
	}

}
