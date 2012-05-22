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
package playground.tnicolai.matsim4opus.utils.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.io.HeaderParser;

/**
 * @author thomas
 *
 */
public class CreateJonesCityNetwork {
	
	private static final String filename = "/Users/thomas/Development/opus_home/data/jonescity_zone/data/received_from_jonathan/tabels20120504/zones4network.csv";
	private static final String networkname = "/Users/thomas/Development/opus_home/data/jonescity_zone/testNetwork.xml";
	private static final String shapefilename = "/Users/thomas/Development/opus_home/data/jonescity_zone/testNetwork.shp";
	
	private static final double stepSize = 1.41421356237310;
	
	private static double freeSpeed = -1.;
	private static double length = -1.;
	private static double lanes = -1.;
	private static double capacity = -1;
	
	private static String defaultCRS = "DHDN_GK4";
	private static final String separator = ",";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		freeSpeed 	= 50. * (1000./3600.); // 50kmh in meter/sec
		length 		= 1000 * stepSize;	   // should be 1,41km
		capacity 	= 1000.;
		lanes		= 2.;		

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem(defaultCRS);
		
		// create empty network
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();

		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);

			// read header of facilities table
			String line = reader.readLine();

			// get and initialize the column number of each header element
			Map<String, Integer> idxFromKey = HeaderParser.createIdxFromKey(line, separator);
			final int indexXCoodinate = idxFromKey.get(Constants.X_COORDINATE);
			final int indexYCoodinate = idxFromKey.get(Constants.Y_COORDINATE);
			final int indexZoneID = idxFromKey.get(Constants.ZONE_ID);

			addNodes2Network(network, reader, indexXCoodinate, indexYCoodinate, indexZoneID);
			addLinks2Network(network);
			
			(new NetworkCleaner() ).run(network);
			
			// Consistency check
			concistencyCheck(network);
			
			new NetworkWriter(network).write(networkname);
			
			// write shape file
			FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, defaultCRS);
			builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
			builder.setWidthCoefficient(0.5);
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
	private static void concistencyCheck(NetworkImpl network) {
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
	private static Iterator<Node> addNodes2Network(NetworkImpl network,
			BufferedReader reader, final int indexXCoodinate,
			final int indexYCoodinate, final int indexZoneID)
			throws IOException, NumberFormatException {

		String line;
		// temporary variables, needed to construct nodes
		IdImpl zoneID;
		Coord coord;
		String[] parts;

		while ((line = reader.readLine()) != null) {
			parts = line.split(separator);

			// get zone ID, UrbanSim sometimes writes IDs as floats!
			int zoneIdAsInt = Integer.parseInt(parts[indexZoneID]);
			zoneID = new IdImpl(zoneIdAsInt);
			// get the coordinates of that parcel
			coord = new CoordImpl(parts[indexXCoodinate], parts[indexYCoodinate]);

			// create a new node
			network.createAndAddNode(zoneID, coord);
		}
		return network.getNodes().values().iterator();
	}
	
	/**
	 * @param network
	 */
	private static void addLinks2Network(NetworkImpl network) {
		
		int zones = network.getNodes().size();
		int steps = (int)Math.sqrt(zones)-1;
		double maxValue = steps * stepSize;
		double minValue = 0.;
		long linkID = 0;
		
		for(double x = minValue; x < maxValue; x = x+stepSize ){
			System.out.println("X-Coord:" + x);
			for(double y = minValue; y < maxValue; y = y+stepSize){
				System.out.println("Y-Coord:" + y);
				
				Node currentNode = network.getNearestNode(new CoordImpl(x,y));
				// add link to above neighbor
				linkID = addLink(network, maxValue, linkID, currentNode, new CoordImpl(x, y + stepSize));
				// add link to left neighbor
				linkID = addLink(network, maxValue, linkID, currentNode, new CoordImpl(x+ stepSize, y));
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
	private static long addLink(NetworkImpl network, double maxValue,
								long linkID, Node currentNode, Coord neighbor) {
		
		if (neighbor.getX() < maxValue
		 && neighbor.getY() < maxValue) {
			
			Node neighborNode = network.getNearestNode(neighbor);
			if(neighborNode != currentNode){
				network.createAndAddLink(new IdImpl(linkID++), currentNode, neighborNode, length, freeSpeed, capacity, lanes);
				network.createAndAddLink(new IdImpl(linkID++), neighborNode, currentNode, length, freeSpeed, capacity, lanes);
			}
		}
		else
			System.out.println("This lies outside the network: x="+ neighbor.getX() + " y="+ neighbor.getY());
		
		return linkID;
	}
}
