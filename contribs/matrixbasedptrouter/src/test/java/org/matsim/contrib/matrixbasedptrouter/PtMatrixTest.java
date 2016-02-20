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
package org.matsim.contrib.matrixbasedptrouter;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * This class tests the pt simulation of MATSim for a simple network created with CreateTestNetwork.java.
 * It checks the values of pt distances and times from the pt matrix for this network.
 * 
 * MATSim always calculates the total pt time and distance respectively between origin and destination facility as a sum of the following three values:
 * 	1.	the walk time and distance respectively from the origin facility to the nearest pt stop,
 * 	2.	the pt time and distance respectively between this origin pt stop and the destination pt stop, which is the nearest one from the destination facility,
 * 	3.	the walk time and distance respectively from this destination pt stop to the destination facility.
 * 
 * MATSim offers to possibilities to use pt simulation, which differ in the information you have to provide:
 * 	1.	The calculation of pt times and distances only from the information about the coordinates of pt stops and facilities.
 * 		For this calculation the euclidean distance between the pt stops and a given pt speed is used.
 * 	2.	The pt simulation with the additional information about the pt times and distances between all possible pt stops.
 * 
 * The walk distances always are calculated as the euclidean distances between facilities and pt stops. The walk times corresponds to these distances for a given walk speed.
 * 
 * In our test network the euclidean distance between a facility and its nearest pt stop is always 50 m. So the total walk distance always is 100 m.
 * If origin and destination pt stop coincide, the agents still have to walk via the pt stop to their destination. Even if origin and destination facility are the same.
 * Moreover there are four pt stops ordered as a square with an euclidean distance of 180 m as the distance between to neighboring pt stops.
 * There are four facilities, each corresponding to one of the four pt stop.
 * The distance between to facilities is always bigger than the distance between their corresponding pt stops. So the euclidean distance between to facilities is an upper bound for the distance between to pt stops.
 * 
 * @author thomas
 * @author tthunig
 */
public class PtMatrixTest {
	
	private static final Logger log = Logger.getLogger(PtMatrixTest.class);

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		OutputDirectoryLogging.catchLogEntries();
		// (collect log messages internally before they can be written to file.  Can be called multiple times without harm.)
	}

	
	/**
	 * tests the pt matrix for the first case, when only the coordinates of the pt stops are known
	 * @throws IOException 
	 */
	@Test
	public void testPtMatrixStops() throws IOException{
		log.info("Start testing the pt matrix with information about the pt stops.");
		long start = System.currentTimeMillis();
		
		
		// some default values
		double defaultWalkSpeed = 1.; // in m/s
		double defaultPtSpeed 	= 10.; // in m/s
		double beelineDistanceFactor = 2.; // a multiplier for the pt travel distance

		Config config = ConfigUtils.createConfig() ;
		config.plansCalcRoute().setBeelineDistanceFactor(beelineDistanceFactor ) ;
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed) ;
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, defaultPtSpeed ) ;

		Network network = CreateTestNetwork.createTestNetwork();			// creates a dummy network
		String location = CreateTestNetwork.createTestPtStationCSVFile(folder.newFile("ptStops.csv"));	// creates a dummy csv file with pt stops fitting into the dummy network
		
		MatrixBasedPtRouterConfigGroup module = new MatrixBasedPtRouterConfigGroup();
		module.setPtStopsInputFile(location);								// this is to be compatible with real code
		config.addModule(module) ;

		// determining the bounds minX/minY -- maxX/maxY. For optimal performance of the QuadTree. All pt stops should be evenly distributed within this rectangle.
		BoundingBox nbb = BoundingBox.createBoundingBox(network);
		// call and init the pt matrix
		PtMatrix ptm = PtMatrix.createPtMatrix(config.plansCalcRoute(), nbb, module);

		// test the matrix
		List<Coord> facilityList = CreateTestNetwork.getTestFacilityLocations();
		
		for(int origin = 0; origin < facilityList.size(); origin++){
			for(int destination = 0; destination < facilityList.size(); destination++){
				
				// calculate travel times
				double totalTravelTime = ptm.getTotalTravelTime_seconds(facilityList.get( origin ), facilityList.get( destination ));
				double walkTravelTime = ptm.getTotalWalkTravelTime_seconds(facilityList.get( origin ), facilityList.get( destination ));
				double ptTravelTime = ptm.getPtTravelTime_seconds(facilityList.get( origin ), facilityList.get( destination ));
				
				// calculate travel distances
				double totalTravelDistance= ptm.getTotalTravelDistance_meter(facilityList.get( origin ), facilityList.get( destination ));
				double walkTravelDistance = ptm.getTotalWalkTravelDistance_meter(facilityList.get( origin ), facilityList.get( destination ));
				double ptTravelDistance = ptm.getPtTravelDistance_meter(facilityList.get( origin ), facilityList.get( destination ));
				
				log.info("From: " + facilityList.get( origin ).getX()+":"+facilityList.get( origin ).getY() + ", To: " + facilityList.get( destination ).getX()+":"+facilityList.get( destination ).getY()  + ", TravelTime: " + totalTravelTime + ", Travel Distance: " + totalTravelDistance);
				
				// test travel time and distance for same origins and destinations
				// the agents will walk 50 m to the nearest pt stop and 50 m back to their origin facility, so the total travel distance have to be 100 m.
				if(origin == destination){
					
					Assert.assertTrue(totalTravelTime == 100./defaultWalkSpeed);
					Assert.assertTrue(totalTravelDistance == 100.);
				}
				
				// test travel time and distance for neighboring origins and destinations
				else if( (origin + 1) % 4 == destination || (origin + 3) % 4 == destination){
					
					// test total walk travel distance and time
					// in the test network the total walk distance always is 100 m, because the euclidean distance between a facility and its nearest pt stop always is 50 m
					Assert.assertTrue(walkTravelDistance == 100.);
					Assert.assertTrue(walkTravelTime == 100./defaultWalkSpeed);
					
					// test pt travel distance and time
					// in the test network the euclidean distance between neighboring pt stops always is 180 m
					Assert.assertTrue(ptTravelDistance == 180.*beelineDistanceFactor);
					Assert.assertTrue(ptTravelTime == (180./defaultPtSpeed)*beelineDistanceFactor);
				}
				
				 // test travel times and distances for diagonal origin destination pairs
				else {
					// In our test network pt stops are closer to each other than facilities.
					// So an upper bound for the pt travel distance is the euclidean distance between the facilities (analog for the travel time).
					// A lower bound for pt travel distance and time are the values of neighboring origin destination pairs.
					
					double euclideanDistance= CoordUtils.calcEuclideanDistance(facilityList.get( origin ), facilityList.get( destination ));
					
					// test total walk travel distance and time
					// in the test network the total walk distance always is 100 m, because the euclidean distance between a facility and its nearest pt stop always is 50 m
					Assert.assertTrue(walkTravelDistance == 100.);
					Assert.assertTrue(walkTravelTime == 100./defaultWalkSpeed);
					
					// test upper bounds for pt travel distance and time (as described above)
					Assert.assertTrue(ptTravelDistance <= euclideanDistance*beelineDistanceFactor);
					Assert.assertTrue(ptTravelTime <= (euclideanDistance/defaultPtSpeed)*beelineDistanceFactor);
					
					// test lower bounds for pt travel distance and time (as described above)
					Assert.assertTrue(ptTravelDistance >= 180.*beelineDistanceFactor);
					Assert.assertTrue(ptTravelTime >= (180./defaultPtSpeed)*beelineDistanceFactor);
				}
			}
		}
		
		log.info("Creating pt matrix took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}
	
	
	/**
	 * tests the pt matrix for the second case, when pt stops and pt travel times and distances are known
	 * 
	 * the values for the pt distances and times are given from a csv-file created with CreateTestNetwork.java
	 * For reasons of simplification we use the same csv-file for both informations (times and distances). Between all pt stops we set the pt distance to 100 m and the pt travel time to 100 min, except pairs of same pt stops, where we set both informations to zero.  
	 * @throws IOException 
	 */
	@Test
	public void testPtMatrixTimesAndDistances() throws IOException{
		log.info("Start testing the pt matrix with information about the pt stops, pt travel times and distances.");
		long start = System.currentTimeMillis();
		
		
		// some default values
		double defaultWalkSpeed = 1.; // in m/s
		double defaultPtSpeed 	= 10.; // in m/s
		double beelineDistanceFactor = 2.; // a multiplier for the pt travel distance
		
		Config config = ConfigUtils.createConfig() ;
		config.plansCalcRoute().setBeelineDistanceFactor(beelineDistanceFactor ) ;
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed) ;
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, defaultPtSpeed ) ;


		Network network = CreateTestNetwork.createTestNetwork();			// creates a dummy network
		String stopsLocation = CreateTestNetwork.createTestPtStationCSVFile(folder.newFile("ptStops.csv"));	// creates a dummy csv file with pt stops fitting into the dummy network
		String timesLocation = CreateTestNetwork.createTestPtTravelTimesAndDistancesCSVFile(folder.newFile("ptTravelInfo.csv"));	// creates a dummy csv file with pt travel times fitting into the dummy network

		MatrixBasedPtRouterConfigGroup module = new MatrixBasedPtRouterConfigGroup();
		module.setUsingPtStops(true);
		module.setUsingTravelTimesAndDistances(true);
		module.setPtStopsInputFile(stopsLocation);								
		module.setPtTravelTimesInputFile(timesLocation);						
		module.setPtTravelDistancesInputFile(timesLocation);

		// determining the bounds minX/minY -- maxX/maxY. For optimal performance of the QuadTree. All pt stops should be evenly distributed within this rectangle.
		BoundingBox nbb = BoundingBox.createBoundingBox(network);
		// call and init the pt matrix
		PtMatrix ptm = PtMatrix.createPtMatrix(config.plansCalcRoute(), nbb, module);

		// test the matrix
		List<Coord> facilityList = CreateTestNetwork.getTestFacilityLocations();
		
		for(int origin = 0; origin < facilityList.size(); origin++){
			for(int destination = 0; destination < facilityList.size(); destination++){
				
				// calculate travel times
				double totalTravelTime = ptm.getTotalTravelTime_seconds(facilityList.get( origin ), facilityList.get( destination ));
				double walkTravelTime = ptm.getTotalWalkTravelTime_seconds(facilityList.get( origin ), facilityList.get( destination ));
				double ptTravelTime = ptm.getPtTravelTime_seconds(facilityList.get( origin ), facilityList.get( destination ));
				
				// calculate travel distances
				double totalTravelDistance= ptm.getTotalTravelDistance_meter(facilityList.get( origin ), facilityList.get( destination ));
				double walkTravelDistance = ptm.getTotalWalkTravelDistance_meter(facilityList.get( origin ), facilityList.get( destination ));
				double ptTravelDistance = ptm.getPtTravelDistance_meter(facilityList.get( origin ), facilityList.get( destination ));
				
				log.info("From: " + facilityList.get( origin ).getX()+":"+facilityList.get( origin ).getY() + ", To: " + facilityList.get( destination ).getX()+":"+facilityList.get( destination ).getY()  + ", TravelTime: " + totalTravelTime + ", Travel Distance: " + totalTravelDistance);
				
				// test travel time and distance for same origins and destinations
				// the agents will walk 50 m to the nearest pt stop and 50 m back to their origin facility, so the total travel distance have to be 100 m.
				if(origin == destination){
					
					Assert.assertTrue(totalTravelDistance == 100.);
					Assert.assertTrue(totalTravelTime == 100./defaultWalkSpeed);
				}
				
				// test travel time and distance for different origins and destinations
				else {
					
					// test total walk travel distance and time
					// in the test network the total walk distance always is 100 m, because the euclidean distance between a facility and its nearest pt stop always is 50 m
					Assert.assertTrue(walkTravelDistance == 100.);
					Assert.assertTrue(walkTravelTime == 100./defaultWalkSpeed);
					
					// test pt travel distance and time
					// in the csv-file the pt travel distance is given as 100 m; the pt travel time as 100 min
					Assert.assertTrue(ptTravelDistance == 100.);
					Assert.assertTrue(ptTravelTime == 100. * 60); // multiplied by 60 to convert minutes to seconds (csv-files are saved in minutes; matsim works with seconds)
				}
			}
		}
		
		log.info("Creating pt matrix took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}

}