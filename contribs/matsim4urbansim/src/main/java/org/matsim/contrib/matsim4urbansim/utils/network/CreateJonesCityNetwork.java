/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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
package org.matsim.contrib.matsim4urbansim.utils.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.utils.io.HeaderParser;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;


/**
 * @author thomas
 *
 */
public class CreateJonesCityNetwork {
	
	private static final String filename = "/Users/thomas/Development/opus_home/data/jonescity_zone/data/received_from_jonathan/tables20120522/zones.csv";
	private static final String networkname = "/Users/thomas/Development/opus_home/data/jonescity_zone/jonesCityNetwork.xml";
	private static final String shapefilename = "/Users/thomas/Development/opus_home/data/jonescity_zone/jonesCityNetwork.shp";
	
	private static final double stepSize = 1.41421356237310;
	
	private static double freeSpeed = -1.;
	private static double length = -1.;
	private static double lanes = -1.;
	private static double capacity = -1;
	
	private static String defaultCRS = "EPSG:31300"; // CRS for Belgium = EPSG:31300
	private static final String separator = ",";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		freeSpeed 	= 50. * (1000./3600.); // 50kmh in meter/sec
		length 		= 1000 * stepSize;	   // should be 1,41km
		capacity 	= 500.;
		lanes		= 1.;

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem(defaultCRS);
		
		// create empty network
		Network network = (Network)scenario.getNetwork();

		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);

			// read header of facilities table
			String line = reader.readLine();

			// get and initialize the column number of each header element
			Map<String, Integer> idxFromKey = HeaderParser.createIdxFromKey(line, separator);
			final int indexXCoodinate = idxFromKey.get(InternalConstants.X_COORDINATE);
			final int indexYCoodinate = idxFromKey.get(InternalConstants.Y_COORDINATE);
			final int indexZoneID = idxFromKey.get(InternalConstants.ZONE_ID);

			addNodes2Network(network, reader, indexXCoodinate, indexYCoodinate, indexZoneID);
			addLinks2Network(network);
			
			(new NetworkCleaner() ).run(network);
			
			// Consistency check
			concistencyCheck(network);
			
			new NetworkWriter(network).write(networkname);
						
			// write shape file
			FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, defaultCRS);
			builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
			builder.setWidthCoefficient(1.0);
			builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
			builder.setCoordinateReferenceSystem(MGC.getCRS(defaultCRS));
			new Links2ESRIShape(network,shapefilename, builder).write();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param network
	 */
	private static void concistencyCheck(Network network) {
		int nodeSize = network.getNodes().values().size();
		int linkSize = network.getLinks().values().size();
		int steps = (int)Math.sqrt(nodeSize);
		int part1 = nodeSize-(steps*2);
		int expectedNumberOfLinks = 2*(2*part1 + 2*steps);
		if(expectedNumberOfLinks != linkSize)
			System.out.println("The network might be inconsistent");
	}

	/**
	 * @param network
	 * @param reader
	 * @param indexXCoodinate
	 * @param indexYCoodinate
	 * @param indexZoneID
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	private static Iterator<? extends Node> addNodes2Network(Network network,
			BufferedReader reader, final int indexXCoodinate,
			final int indexYCoodinate, final int indexZoneID)
			throws IOException, NumberFormatException {

		String line;
		// temporary variables, needed to construct nodes
		Id<ActivityFacility> zoneID;
		Coord coord;
		String[] parts;

		while ((line = reader.readLine()) != null) {
			parts = line.split(separator);

			// get zone ID, UrbanSim sometimes writes IDs as floats!
			int zoneIdAsInt = Integer.parseInt(parts[indexZoneID]);
			zoneID = Id.create(zoneIdAsInt, ActivityFacility.class);
			// get the coordinates of that parcel
			// tnicolai remove the 1000.
			double x = Double.parseDouble( parts[indexXCoodinate] );
			double y = Double.parseDouble( parts[indexYCoodinate] );

			coord = new Coord(x, y);
			final Coord coord1 = coord;

			// create a new node
			Node node = (Node)NetworkUtils.createAndAddNode(network, Id.create(zoneID, Node.class), coord1);
			NetworkUtils.setOrigId( node, zoneID.toString() ) ;
			NetworkUtils.setType(node,"unknownType");		
		}
		return network.getNodes().values().iterator();
	}
	
	/**
	 * @param network
	 */
	private static void addLinks2Network(Network network) {
		
		int zones = network.getNodes().size();
		int steps = (int)Math.sqrt(zones)-1;
		double length = stepSize * 1000; // tnicolai: replace the 1000 when the new zones data set is delivered. With this setting the network is created correctley but dosen't match with the home and work locations
		double maxValue = steps * length;
		double minValue = 0.;
		long linkID = 0;
		
		for(double x = minValue; x < maxValue; x = x+length ){
			System.out.println("X-Coord:" + x);
			for(double y = minValue; y < maxValue; y = y+length){
				System.out.println("Y-Coord:" + y);

				Node currentNode = NetworkUtils.getNearestNode(network,new Coord(x, y));
				// add link to above neighbor
				linkID = addLink(network, maxValue, linkID, currentNode, new Coord(x, y + length));
				// add link to left neighbor
				linkID = addLink(network, maxValue, linkID, currentNode, new Coord(x + length, y));
			}
		}
	}

	/**
	 * @param network
	 * @param maxValue
	 * @param linkID
	 * @param currentNode
	 * @param neighbor
	 */
	private static long addLink(Network network, double maxValue,
								long linkID, Node currentNode, Coord neighbor) {
		
		if (neighbor.getX() < maxValue
		 && neighbor.getY() < maxValue) {
			
			final Coord coord = neighbor;
			Node neighborNode = NetworkUtils.getNearestNode(network,coord);
			if(neighborNode != currentNode){
				final Node fromNode = currentNode;
				final Node toNode = neighborNode;
				final double length1 = length;
				final double freespeed = freeSpeed;
				final double capacity1 = capacity;
				final double numLanes = lanes;
				NetworkUtils.createAndAddLink(network,Id.create(linkID++, Link.class), fromNode, toNode, length1, freespeed, capacity1, numLanes );
				final Node fromNode1 = neighborNode;
				final Node toNode1 = currentNode;
				final double length2 = length;
				final double freespeed1 = freeSpeed;
				final double capacity2 = capacity;
				final double numLanes1 = lanes;
				NetworkUtils.createAndAddLink(network,Id.create(linkID++, Link.class), fromNode1, toNode1, length2, freespeed1, capacity2, numLanes1 );
			}
		}
		else
			System.out.println("This lies outside the network: x="+ neighbor.getX() + " y="+ neighbor.getY());
		
		return linkID;
	}
}
