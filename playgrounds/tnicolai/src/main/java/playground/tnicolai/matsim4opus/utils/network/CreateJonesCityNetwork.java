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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.io.HeaderParser;

/**
 * @author thomas
 *
 */
public class CreateJonesCityNetwork {
	
	private static final String filename = "/Users/thomas/Development/opus_home/data/jonescity_zone/tables20120503/zones_for_network_generation.csv";
	private static ScenarioImpl scenario = null;
	
	private static final int numberOfLanes = 1;
	private static final double capacity = 1000.;
	
	private static final double xmin = 0.;
	private static final double ymin = 0.;
	private static final double xmax = 1.;
	private static final double ymax = 1.;
	
	private static final String separator = ",";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// empty scenario
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		// create empty network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);

			// read header of facilities table
			String line = reader.readLine();

			// get and initialize the column number of each header element
			Map<String, Integer> idxFromKey = HeaderParser.createIdxFromKey(line, separator);
			final int indexXCoodinate = idxFromKey.get(Constants.X_COORDINATE);
			final int indexYCoodinate = idxFromKey.get(Constants.Y_COORDINATE);
			final int indexZoneID = idxFromKey.get(Constants.ZONE_ID);

			Iterator<Node> nodes = addNodes2Network(network, reader, indexXCoodinate, indexYCoodinate, indexZoneID);
			
			while(nodes.hasNext()){
				Node node = nodes.next();
				System.out.println(node.getId() + "," + node.getCoord().getX() + "," + node.getCoord().getY());
			}
			
			// TODO sort nodes into right order according to their coordinates (from xmin>xmax and ymin>ymax)
			// Add links between the nodes

		} catch (Exception e) {
			e.printStackTrace();
		}
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

}
