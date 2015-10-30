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
package org.matsim.contrib.matrixbasedptrouter.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * This class creates a simple test network to test for example the pt simulation in MATSim.
 * 
 * @author thomas
 * @author tthunig
 */
public final class CreateTestNetwork {

	/**
	 * This method creates a test network. It is used for example in PtMatrixTest.java to test the pt simulation in MATSim.
	 * The network has 9 nodes and 8 links (see the sketch below).
	 * 
	 * @return the created test network
	 */
	public static Network createTestNetwork() {

		/*
		 * (2)		(5)------(8)
		 * 	|		 |
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|		 |
		 * (3)		(6)------(9)
		 */
		double freespeed = 2.7;	// this is m/s and corresponds to 50km/h
		double capacity = 500.;
		double numLanes = 1.;

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		// add nodes
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 100));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 0, (double) 200));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 0, (double) 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 100, (double) 100));
		Node node5 = network.createAndAddNode(Id.create(5, Node.class), new Coord((double) 100, (double) 200));
		Node node6 = network.createAndAddNode(Id.create(6, Node.class), new Coord((double) 100, (double) 0));
		Node node7 = network.createAndAddNode(Id.create(7, Node.class), new Coord((double) 200, (double) 100));
		Node node8 = network.createAndAddNode(Id.create(8, Node.class), new Coord((double) 200, (double) 200));
		Node node9 = network.createAndAddNode(Id.create(9, Node.class), new Coord((double) 200, (double) 0));

		// add links (bi-directional)
		network.createAndAddLink(Id.create( 1, Link.class), node1, node2, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 2, Link.class), node2, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 3, Link.class), node1, node3, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 4, Link.class), node3, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 5, Link.class), node1, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 6, Link.class), node4, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 7, Link.class), node4, node5, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 8, Link.class), node5, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create( 9, Link.class), node4, node6, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(10, Link.class), node6, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(11, Link.class), node4, node7, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(12, Link.class), node7, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(13, Link.class), node5, node8, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(14, Link.class), node8, node5, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(15, Link.class), node6, node9, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(Id.create(16, Link.class), node9, node6, 100, freespeed, capacity, numLanes);

		return network;
	}

	/**
	 * This method creates 4 pt stops for the test network from createTestNetwork().
	 * The information about the coordinates will be written to a csv file.
	 * The 4 pt stops are located as a square in the coordinate plane with a side length of 180 meter (see the sketch below).
	 *
	 * @return the location of the written csv file
	 */
	public static String createTestPtStationCSVFile(File file){

		/*
		 * (2)	    (5)------(8)
		 * 	|(pt2)   |   (pt3)
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|(pt1)   |   (pt4)
		 * (3)      (6)------(9)
		 */

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			bw.write("id,x,y"); 	// header
			bw.newLine();
			bw.write("1,10,10");	// pt stop next to node (3)
			bw.newLine();
			bw.write("2,10, 190"); // pt stop next to node (2)
			bw.newLine();
			bw.write("3,190,190"); // pt stop next to node (8)
			bw.newLine();
			bw.write("4,190,10");  // pt stop next to node (9)
			bw.newLine();
			return file.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This methods creates a csv file with informations about pt travel times and pt distances for the test network from createTestNetwork().
	 * We set the pt travel time between all pairs of pt stops to 100 seconds, except pairs of same pt stops where the travel time is 0 seconds.
	 * We set the pt distance between all pairs of pt stops to 100 meter, except pairs of same pt stops where the distance is 0 meter.
	 * Because the data in the csv file does not need an entity, you can use the same csv file for both informations.
	 * 
	 * @return the location of the written file
	 */
	public static String createTestPtTravelTimesAndDistancesCSVFile(File file){
		// set dummy travel times or distances to all possible pairs of pt stops

		try (BufferedWriter bw = IOUtils.getBufferedWriter(file.getCanonicalPath())) {
			for (int origin = 1; origin <= 4; origin++){
				for (int destination = 1; destination <= 4; destination++){
					if (origin == destination) {
						// set a travel time/distance of 0s or 0m between same origin and destination pt stops
						bw.write(origin + " " + destination + " 0");
						bw.newLine();
					} else {
						// set a dummy travel time/distance of 100s or 100m between different origin and destination pt stops
						bw.write(origin + " " + destination + " 100");
						bw.newLine();
					}

				}
			}
			bw.flush();
			return file.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		// add nodes
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 50, (double) 100));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 50, (double) 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 100, (double) 0));

		// add links
		network.createAndAddLink(Id.create(1, Link.class), node1, node2, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(2, Link.class), node2, node4, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create(3, Link.class), node1, node3, 50.0, 0.1, 3600.0, 1);
		network.createAndAddLink(Id.create(4, Link.class), node3, node4, 50.0, 0.1, 3600.0, 1);

		return network;
	}

}
