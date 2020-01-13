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
package org.matsim.contrib.accessibility.run;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * This class creates a simple test network to test for example the pt simulation in MATSim.
 * 
 * @author thomas
 * @author tthunig
 */
@Deprecated
class CreateTestNetwork {
	
	@Rule public static MatsimTestUtils utils = new MatsimTestUtils();
	
	private static final String NEW_LINE	= "\r\n";
	
	/**
	 * This method creates 4 pt stops for the test network from createTestNetwork().
	 * The information about the coordinates will be written to a csv file.
	 * The 4 pt stops are located as a square in the coordinate plane with a side length of 180 meter (see the sketch below).
	 *  
	 * @return the location of the written csv file
	 */
	public static String createTestPtStationCSVFile(){
		
		/*
		 * (2)	    (5)------(8)
		 * 	|(pt2)   |   (pt3)
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|(pt1)   |   (pt4)
		 * (3)      (6)------(9)
		 */
		
		String location = utils.getOutputDirectory()  + "/ptStops.csv";
		BufferedWriter bw = IOUtils.getBufferedWriter(location);
		
		try{
			bw.write("id,x,y" + NEW_LINE); 	// header
			bw.write("1,10,10" + NEW_LINE);	// pt stop next to node (3)
			bw.write("2,10, 190" + NEW_LINE); // pt stop next to node (2)
			bw.write("3,190,190" + NEW_LINE); // pt stop next to node (8)
			bw.write("4,190,10" + NEW_LINE);  // pt stop next to node (9)
			bw.flush();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return location;
	}
	
	/**
	 * This methods creates a csv file with informations about pt travel times and pt distances for the test network from createTestNetwork().
	 * We set the pt travel time between all pairs of pt stops to 100 seconds, except pairs of same pt stops where the travel time is 0 seconds.
	 * We set the pt distance between all pairs of pt stops to 100 meter, except pairs of same pt stops where the distance is 0 meter.
	 * Because the data in the csv file does not need any measurement unit, one can use the same csv file for both informations.
	 * 
	 * @return the location of the written file
	 */
	public static String createTestPtTravelTimesAndDistancesCSVFile(){
		
		// set dummy travel times or distances to all possible pairs of pt stops
		
		String location = utils.getOutputDirectory()  + "/ptTravelInfo.csv";
		BufferedWriter bw = IOUtils.getBufferedWriter(location);
		
		try{
			for (int origin = 1; origin <= 4; origin++){
				for (int destination = 1; destination <= 4; destination++){
					if (origin == destination)
						// set a travel time/distance of 0s or 0m between same origin and destination pt stops
						bw.write(origin + " " + destination + " 0" + NEW_LINE);
					else
						// set a dummy travel time/distance of 100s or 100m between different origin and destination pt stops
						bw.write(origin + " " + destination + " 100" + NEW_LINE); 
				}
			}
			bw.flush();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return location;
	}
	
	/**
	 * This method creates 4 facilities for the test network from createTestNetwork().
	 * The distance between each facility and the nearest pt stop is 50 meter (see the sketch below).
	 * 
	 * @return the facility list
	 */
	public static List<Coord> getTestFacilityLocations(){
		
		/*    B             C
		 * (2)		(5)------(8)
		 * 	|		 |
		 * 	|		 |    
		 * (1)------(4)------(7)
		 * 	|		 |           
		 * 	|		 |
		 * (3)		(6)------(9)
		 *    A             D
		 */   
		
		List<Coord> facilityList = new ArrayList<Coord>();
		final double y1 = -40;
		facilityList.add(new Coord((double) 10, y1));  // 50m to pt station 1
		facilityList.add(new Coord((double) 10, (double) 240));  // 50m to pt station 2
		facilityList.add(new Coord((double) 190, (double) 240)); // 50m to pt station 3
		final double y = -40;
		facilityList.add(new Coord((double) 190, y)); // 50m to pt station 4
		return facilityList;
	}
	
	/**
	 * creating a test network
	 * the path 1,2,4 has a total length of 1000m with a free speed travel time of 10m/s
	 * the second path 1,3,4 has a total length of 100m but only a free speed travel time of 0.1m/s
	 */
	public static Network createTriangularNetwork() {
		/*
		 * 			(2)
		 *         /   \
		 *        /     \
		 *(10m/s)/       \(10m/s)
		 *(500m)/	      \(500m)
		 *     /           \
		 *    /             \
		 *	 /               \
		 *(1)-------(3)-------(4)
		 *(50m,0.1m/s)(50m,0.1m/s) 			
		 */
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = (Network) scenario.getNetwork();
		
		// add nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 50, (double) 100));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 50, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;

		// add links
		NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 500.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node4;
		NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, 500.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode2 = node1;
		final Node toNode2 = node3;
		NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode2, toNode2, 50.0, 0.1, 3600.0, (double) 1 );
		final Node fromNode3 = node3;
		final Node toNode3 = node4;
		NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), fromNode3, toNode3, 50.0, 0.1, 3600.0, (double) 1 );
		
		return network;
	}
	
}
