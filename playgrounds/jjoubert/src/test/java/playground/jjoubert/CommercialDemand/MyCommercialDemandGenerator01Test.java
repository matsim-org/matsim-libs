/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialDemandGenerator01Test.java
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

package playground.jjoubert.CommercialDemand;

import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.CommercialDemandGenerator.MyCommercialDemandGenerator01;


public class MyCommercialDemandGenerator01Test extends MatsimTestCase{
	
	public void testMyCommercialDemandGeneratorConstructor(){
		
		/*
		 * Test the constructor.
		 */
		String root = "ABC";
		String area = "DEF";
		Integer numberOfPlans = Integer.valueOf(2);
		Double threshold = 0.9;
		MyCommercialDemandGenerator01 mcdg = new MyCommercialDemandGenerator01(root, area, numberOfPlans, threshold);
		assertEquals("Root name not correct.", root, mcdg.getRoot());
		assertEquals("Study area name not correct.", area, mcdg.getStudyArea());
		assertEquals("Number of plans files to create not correct", numberOfPlans, Integer.valueOf(mcdg.getNumberOfSamples()));
		assertEquals("Threshold not correct.", threshold, mcdg.getActivityThreshold());
				
		/*
		 * I don't have to test whether the right 'within' and 'through' vehicles are
		 * identified; assuming the jjoubert.Utilities.MyVehicleIdentifier test will
		 * do a sufficient job.
		 */		
	}
	
	public void testSomethingElse(){
		
	}

}
