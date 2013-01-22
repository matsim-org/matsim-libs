/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matsim4opus.config.MATSim4UrbanSimControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.matsim4urbansim.router.PtMatrix;
import org.matsim.contrib.matsim4opus.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
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
		double defaultWalkSpeed = 1.38888889;
		double defaultPtSpeed 	= 6.94444444;	// 6.94444444m/s corresponds to 25 km/h
		double beelineDistanceFactor = 1.3;

		Network network = CreateTestNetwork.createTestNetwork();			// creates a dummy network
		String location = CreateTestNetwork.createTestPtStationCSVFile();	// creates a dummy csv file with pt stops fitting into the dummy network
		
		MATSim4UrbanSimControlerConfigModuleV3 m4uccm = new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		m4uccm.setPtStopsInputFile(location);								// this is to be compatible with real code

		// call and init pt matrix
		PtMatrix ptm = new PtMatrix(network, defaultWalkSpeed, defaultPtSpeed, beelineDistanceFactor, m4uccm);

		// test the matrix
		List<Coord> facilityList = CreateTestNetwork.getTestFacilityLocations();
		
		for(int origin = 0; origin < facilityList.size(); origin++){
			for(int destination = 0; destination < facilityList.size(); destination++){
				
				double travelTime = ptm.getTotalTravelTime(facilityList.get( origin ), facilityList.get( destination ));
				double travelDistance= ptm.getTotalTravelDistance(facilityList.get( origin ), facilityList.get( destination ));
				log.info("From: " + facilityList.get( origin ).getX()+":"+facilityList.get( origin ).getY() + ", To: " + facilityList.get( destination ).getX()+":"+facilityList.get( destination ).getY()  + ", TravelTime: " + travelTime + ", Travel Distance: " + travelDistance);
				// tnicolai todo some assert statements
			}
		}
		
		// cleaning up
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		log.info("Creating pt matrix took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}

}
