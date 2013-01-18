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
package org.matsim.contrib.matsim4opus.utils;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author thomas
 *
 */
public class CreateTestNetwork {
	
	public static NetworkImpl createTestNetwork() {

		/*
		 * (2)		(5)
		 * 	|		 |
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|		 |
		 * (3)		(6)
		 */
		double freespeed = 2.7;	// this is m/s and corresponds to 50km/h
		double capacity = 500.;
		double numLanes = 1.;

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		// add nodes
		Node node1 = network.createAndAddNode(new IdImpl(1), scenario.createCoord(0, 100));
		Node node2 = network.createAndAddNode(new IdImpl(2), scenario.createCoord(0, 200));
		Node node3 = network.createAndAddNode(new IdImpl(3), scenario.createCoord(0, 0));
		Node node4 = network.createAndAddNode(new IdImpl(4), scenario.createCoord(100, 100));
		Node node5 = network.createAndAddNode(new IdImpl(5), scenario.createCoord(100, 200));
		Node node6 = network.createAndAddNode(new IdImpl(6), scenario.createCoord(100, 0));
		Node node7 = network.createAndAddNode(new IdImpl(7), scenario.createCoord(200, 100));

		// add links (bi-directional)
		network.createAndAddLink(new IdImpl(1), node1, node2, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(2), node2, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(3), node1, node3, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(4), node3, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(5), node1, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(6), node4, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(7), node4, node5, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(8), node5, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(9), node4, node6, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(10), node6, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(11), node4, node7, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(12), node7, node4, 100, freespeed, capacity, numLanes);

		return network;
	}
	
	public static String createTestPtStationCSVFile(){
		
		/*
		 * (2)(pt2)	(5)
		 * 	|		 |
		 * 	|		 |(pt3)
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|		 |
		 * (3)(pt1) (6)
		 */
		
		String location = TempDirectoryUtil.createCustomTempDirectory("ptStopFileDir")  + "/ptStops.csv";
		BufferedWriter bw = IOUtils.getBufferedWriter(location);
		
		try{
			bw.write("id,x,y" + InternalConstants.NEW_LINE); 	// header
			bw.write("1,10,10" + InternalConstants.NEW_LINE);	// pt stop next to node (3)
			bw.write("2,10, 190" + InternalConstants.NEW_LINE); // pt stop next to node (2)
			bw.write("3,110,110" + InternalConstants.NEW_LINE); // pt stop next to node (4)
			bw.flush();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return location;
	}
	
	public static List<Coord> getTestFacilityLocations(){
		
		/*    B
		 * (2)		(5)
		 * 	|		 |
		 * 	|		 |    C
		 * (1)------(4)------(7)
		 * 	|		 |           
		 * 	|		 |
		 * (3)		(6)
		 *    A
		 */   
		
		List<Coord> facilityList = new ArrayList<Coord>();
		facilityList.add(new CoordImpl(10, -40));  // 50m to pt station 1
		facilityList.add(new CoordImpl(10, 240));  // 50m to pt station 2
		facilityList.add(new CoordImpl(160, 110)); // 50m to pt station 3
		return facilityList;
	}
}
