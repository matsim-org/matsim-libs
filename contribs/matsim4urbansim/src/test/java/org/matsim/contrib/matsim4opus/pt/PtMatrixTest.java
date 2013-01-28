/* *********************************************************************** *
 * project: org.matsim.													   *
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
package org.matsim.contrib.matsim4opus.pt;

import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matsim4opus.config.MATSim4UrbanSimControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.matsim4urbansim.router.PtMatrix;
import org.matsim.contrib.matsim4opus.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author thomas
 *
 */
public class PtMatrixTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(PtMatrixTest.class);
	
	@Test
	public void testPtMatrix(){

		log.info("Start testing");
		
		long start = System.currentTimeMillis();
		
		// some default values
		double defaultWalkSpeed = 1.; // in m/s
		double defaultPtSpeed 	= 10.; // in m/s
		double beelineDistanceFactor = 2.; // a multiplier for the pt travel distance

		Network network = CreateTestNetwork.createTestNetwork();			// creates a dummy network
		String location = CreateTestNetwork.createTestPtStationCSVFile();	// creates a dummy csv file with pt stops fitting into the dummy network
		
		MATSim4UrbanSimControlerConfigModuleV3 m4uccm = new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		m4uccm.setPtStopsInputFile(location);								// this is to be compatible with real code

		// call and init the pt matrix
		PtMatrix ptm = new PtMatrix(network, defaultWalkSpeed, defaultPtSpeed, beelineDistanceFactor, m4uccm);

		// test the matrix
		List<Coord> facilityList = CreateTestNetwork.getTestFacilityLocations();
		
		for(int origin = 0; origin < facilityList.size(); origin++){
			for(int destination = 0; destination < facilityList.size(); destination++){
				
				// calculate travel times
				double totalTravelTime = ptm.getTotalTravelTime(facilityList.get( origin ), facilityList.get( destination ));
				double walkTravelTime = ptm.getTotalWalkTravelTime(facilityList.get( origin ), facilityList.get( destination ));
				double ptTravelTime = ptm.getPtTravelTime(facilityList.get( origin ), facilityList.get( destination ));
				
				// calculate travel distances
				double totalTravelDistance= ptm.getTotalTravelDistance(facilityList.get( origin ), facilityList.get( destination ));
				double walkTravelDistance = ptm.getTotalWalkTravelDistance(facilityList.get( origin ), facilityList.get( destination ));
				double ptTravelDistance = ptm.getPtTravelDistance(facilityList.get( origin ), facilityList.get( destination ));
				
				log.info("From: " + facilityList.get( origin ).getX()+":"+facilityList.get( origin ).getY() + ", To: " + facilityList.get( destination ).getX()+":"+facilityList.get( destination ).getY()  + ", TravelTime: " + totalTravelTime + ", Travel Distance: " + totalTravelDistance);
				
				// test travel time and distance for same origins and destinations
				if(origin == destination){
					
					Assert.assertTrue(totalTravelTime == 0.);
					Assert.assertTrue(totalTravelDistance == 0.);
				}
				
				// test travel time and distance for neighboring origins and destinations
				else if( (origin + 1) % 4 == destination || (origin + 3) % 4 == destination){
					
					// test total walk travel distance and time (in this setting the total walk distance is 100m)
					Assert.assertTrue(walkTravelDistance == 100.);
					Assert.assertTrue(walkTravelTime == 100./defaultWalkSpeed);
					
					// test pt travel distance and time (in this setting the pt distance is 180m)
					Assert.assertTrue(ptTravelDistance == 180.);
					Assert.assertTrue(ptTravelTime == (180./defaultPtSpeed)*beelineDistanceFactor);
				}
				
				 // test travel times and distances for diagonal origin destination pairs
				else {
					// In this setting pt stops are closer to each other than facilities.
					// So an upper bound for the pt travel distance is the euclidean distance between the facilities (analog for the travel time).
					// A lower bound for the pt travel distance and time are the values of neighboring origin destination pairs.
					
					double euclideanDistance= NetworkUtil.getEuclidianDistance(facilityList.get( origin ), facilityList.get( destination ));
					
					// test total walk travel distance and time (in this setting the total walk distance is 100m)
					Assert.assertTrue(walkTravelDistance == 100.);
					Assert.assertTrue(walkTravelTime == 100./defaultWalkSpeed);
					
					// test upper bounds for pt travel distance and time
					Assert.assertTrue(ptTravelDistance <= euclideanDistance);
					Assert.assertTrue(ptTravelTime <= (euclideanDistance/defaultPtSpeed)*beelineDistanceFactor);
					
					// test lower bounds for pt travel distance and time
					Assert.assertTrue(ptTravelDistance >= 180.);
					Assert.assertTrue(ptTravelTime >= (180./defaultPtSpeed)*beelineDistanceFactor);
				}
			}
		}
		
		// cleaning up
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		log.info("Creating pt matrix took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}

}
