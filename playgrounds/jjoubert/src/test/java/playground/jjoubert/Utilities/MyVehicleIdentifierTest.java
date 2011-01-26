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

import java.util.List;

import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.Utilities.MyVehicleIdentifier;


public class MyVehicleIdentifierTest extends MatsimTestCase{
	
	public void testMyVehicleIdentifierConstructor1(){
		// Test the first constructor
		double lowerThreshold = 0.6;
		double upperThreshold = 0.92;
		MyVehicleIdentifier mvi1 = new MyVehicleIdentifier(lowerThreshold, upperThreshold);
		assertEquals("Lower threshold incorrect.", lowerThreshold, mvi1.getLowerThreshold());
		assertEquals("Upper threshold incorrect.", upperThreshold, mvi1.getUpperThreshold());
		assertEquals("Threshold must be Double.MIN_VALUE", Double.valueOf(Double.MIN_VALUE), mvi1.getThreshold());
	}
	
	public void testMyVehicleIdentifierConstructor2(){
		// Test the second constructor
		double threshold = 0.9;
		MyVehicleIdentifier mvi2 = new MyVehicleIdentifier(threshold);
		assertEquals("Lower threshold must be Double.MIN_VALUE.", Double.valueOf(Double.MIN_VALUE), mvi2.getLowerThreshold());
		assertEquals("Upper threshold must be Double.MIN_VALUE.", Double.valueOf(Double.MIN_VALUE), mvi2.getUpperThreshold());
		assertEquals("Threshold incorrect", threshold, mvi2.getThreshold());
	}
	
	public void testBuildVehicleList(){
		String testFile = getInputDirectory() + "testVehicleStats.txt";
		double lowerThreshold = 0.6;
		double upperThreshold = 0.92;
		MyVehicleIdentifier mvi = new MyVehicleIdentifier(lowerThreshold, upperThreshold);

		// Test double threshold
		List<Integer> list = mvi.buildVehicleList(testFile, ",");
		assertEquals("Wrong number of vehicles.", 1, list.size());
		assertEquals("Wrong vehicle in list.", Integer.valueOf(3), Integer.valueOf(list.get(0)));
	}
	
	public void testBuildVehicleLists(){
		String testFile = getInputDirectory() + "testVehicleStats.txt";
		double threshold = 0.9;
		MyVehicleIdentifier mvi = new MyVehicleIdentifier(threshold);
		
		// Test single threshold
		List<List<Integer>> lists = mvi.buildVehicleLists(testFile, ",");
		List<Integer> listWithin = lists.get(0);
		assertEquals("Wrong number of vehicles in 'within' list.", Integer.valueOf(1), Integer.valueOf(listWithin.size()));
		assertEquals("Wrong vehicle in 'within' list.", Integer.valueOf(4), Integer.valueOf(listWithin.get(0)));
		
		List<Integer> listThrough = lists.get(1);		
		assertEquals("Wrong number of vehicles in 'through' list.", Integer.valueOf(3), Integer.valueOf(listThrough.size()));
		assertEquals("Wrong vehicle in 'through' list.", Integer.valueOf(1), Integer.valueOf(listThrough.get(0)));
		assertEquals("Wrong vehicle in 'through' list.", Integer.valueOf(2), Integer.valueOf(listThrough.get(1)));
		assertEquals("Wrong vehicle in 'through' list.", Integer.valueOf(3), Integer.valueOf(listThrough.get(2)));
	}

}
