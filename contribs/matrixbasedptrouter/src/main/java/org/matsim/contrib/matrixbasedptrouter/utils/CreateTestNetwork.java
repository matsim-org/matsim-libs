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
import org.matsim.core.network.NetworkUtils;
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

		Network network = (Network) scenario.getNetwork();

		// add nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 100));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 0, (double) 200));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 0, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 100, (double) 100));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 100, (double) 200));
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create(6, Node.class), new Coord((double) 100, (double) 0));
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create(7, Node.class), new Coord((double) 200, (double) 100));
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create(8, Node.class), new Coord((double) 200, (double) 200));
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create(9, Node.class), new Coord((double) 200, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		final double freespeed1 = freespeed;
		final double capacity1 = capacity;
		final double numLanes1 = numLanes;

		// add links (bi-directional)
		NetworkUtils.createAndAddLink(network,Id.create( 1, Link.class), fromNode, toNode, (double) 100, freespeed1, capacity1, numLanes1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node1;
		final double freespeed2 = freespeed;
		final double capacity2 = capacity;
		final double numLanes2 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 2, Link.class), fromNode1, toNode1, (double) 100, freespeed2, capacity2, numLanes2 );
		final Node fromNode2 = node1;
		final Node toNode2 = node3;
		final double freespeed3 = freespeed;
		final double capacity3 = capacity;
		final double numLanes3 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 3, Link.class), fromNode2, toNode2, (double) 100, freespeed3, capacity3, numLanes3 );
		final Node fromNode3 = node3;
		final Node toNode3 = node1;
		final double freespeed4 = freespeed;
		final double capacity4 = capacity;
		final double numLanes4 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 4, Link.class), fromNode3, toNode3, (double) 100, freespeed4, capacity4, numLanes4 );
		final Node fromNode4 = node1;
		final Node toNode4 = node4;
		final double freespeed5 = freespeed;
		final double capacity5 = capacity;
		final double numLanes5 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 5, Link.class), fromNode4, toNode4, (double) 100, freespeed5, capacity5, numLanes5 );
		final Node fromNode5 = node4;
		final Node toNode5 = node1;
		final double freespeed6 = freespeed;
		final double capacity6 = capacity;
		final double numLanes6 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 6, Link.class), fromNode5, toNode5, (double) 100, freespeed6, capacity6, numLanes6 );
		final Node fromNode6 = node4;
		final Node toNode6 = node5;
		final double freespeed7 = freespeed;
		final double capacity7 = capacity;
		final double numLanes7 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 7, Link.class), fromNode6, toNode6, (double) 100, freespeed7, capacity7, numLanes7 );
		final Node fromNode7 = node5;
		final Node toNode7 = node4;
		final double freespeed8 = freespeed;
		final double capacity8 = capacity;
		final double numLanes8 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 8, Link.class), fromNode7, toNode7, (double) 100, freespeed8, capacity8, numLanes8 );
		final Node fromNode8 = node4;
		final Node toNode8 = node6;
		final double freespeed9 = freespeed;
		final double capacity9 = capacity;
		final double numLanes9 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create( 9, Link.class), fromNode8, toNode8, (double) 100, freespeed9, capacity9, numLanes9 );
		final Node fromNode9 = node6;
		final Node toNode9 = node4;
		final double freespeed10 = freespeed;
		final double capacity10 = capacity;
		final double numLanes10 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(10, Link.class), fromNode9, toNode9, (double) 100, freespeed10, capacity10, numLanes10 );
		final Node fromNode10 = node4;
		final Node toNode10 = node7;
		final double freespeed11 = freespeed;
		final double capacity11 = capacity;
		final double numLanes11 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(11, Link.class), fromNode10, toNode10, (double) 100, freespeed11, capacity11, numLanes11 );
		final Node fromNode11 = node7;
		final Node toNode11 = node4;
		final double freespeed12 = freespeed;
		final double capacity12 = capacity;
		final double numLanes12 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(12, Link.class), fromNode11, toNode11, (double) 100, freespeed12, capacity12, numLanes12 );
		final Node fromNode12 = node5;
		final Node toNode12 = node8;
		final double freespeed13 = freespeed;
		final double capacity13 = capacity;
		final double numLanes13 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(13, Link.class), fromNode12, toNode12, (double) 100, freespeed13, capacity13, numLanes13 );
		final Node fromNode13 = node8;
		final Node toNode13 = node5;
		final double freespeed14 = freespeed;
		final double capacity14 = capacity;
		final double numLanes14 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(14, Link.class), fromNode13, toNode13, (double) 100, freespeed14, capacity14, numLanes14 );
		final Node fromNode14 = node6;
		final Node toNode14 = node9;
		final double freespeed15 = freespeed;
		final double capacity15 = capacity;
		final double numLanes15 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(15, Link.class), fromNode14, toNode14, (double) 100, freespeed15, capacity15, numLanes15 );
		final Node fromNode15 = node9;
		final Node toNode15 = node6;
		final double freespeed16 = freespeed;
		final double capacity16 = capacity;
		final double numLanes16 = numLanes;
		NetworkUtils.createAndAddLink(network,Id.create(16, Link.class), fromNode15, toNode15, (double) 100, freespeed16, capacity16, numLanes16 );

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
