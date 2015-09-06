/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.visum;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author johannes
 *
 */
public class Visum2MatsimRoadNetwork {

	private static final Logger logger = Logger.getLogger(Visum2MatsimRoadNetwork.class);
	
	public static void main(String args[]) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		StreamingVisumNetworkReader streamingVisumNetworkReader = new StreamingVisumNetworkReader();

		VisumNetworkRowHandler nodeRowHandler = new VisumNetworkRowHandler() {

			@Override
			public void handleRow(Map<String, String> row) {
				Id<Node> id = Id.create(row.get("NR"), Node.class);
				Coord coord = new Coord(Double.parseDouble(row.get("XKOORD").replace(',', '.')), Double.parseDouble(row.get("YKOORD").replace(',', '.')));
				network.createAndAddNode(id, coord);
			}

		};
		streamingVisumNetworkReader.addRowHandler("KNOTEN", nodeRowHandler);

		VisumNetworkRowHandler edgeRowHandler = new VisumNetworkRowHandler() {

			@Override
			public void handleRow(Map<String, String> row) {
				String nr = row.get("NR");
				Id<Link> id = Id.create(nr, Link.class);
				Id<Node> fromNodeId = Id.create(row.get("VONKNOTNR"), Node.class);
				Id<Node> toNodeId = Id.create(row.get("NACHKNOTNR"), Node.class);
		
				Node fromNode = network.getNodes().get(fromNodeId);
				Node toNode = network.getNodes().get(toNodeId);
				
				Link lastEdge = network.getLinks().get(id);
				
				if (lastEdge != null) {
					if (lastEdge.getFromNode().getId().equals(toNodeId) && lastEdge.getToNode().getId().equals(fromNodeId)) {
						id = Id.create(nr + 'R', Link.class);
					} else {
						throw new RuntimeException("Duplicate edge.");
					}
				}
				
				String lenStr = row.get("LAENGE");
//				lenStr = lenStr.replace("km", "");
				lenStr = lenStr.substring(0, lenStr.indexOf("k"));
				double length = Double.parseDouble(lenStr) * 1000;
				String speedStr = row.get("V0IV");
				speedStr = speedStr.substring(0, speedStr.indexOf("k"));
				double freespeed = Double.parseDouble(speedStr);
				
				double capacity = Double.parseDouble(row.get("KAPIV"));
				int noOfLanes = Integer.parseInt(row.get("ANZFAHRSTREIFEN"));

				if(freespeed >= 80) {
					freespeed /= 3.6;
					network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, noOfLanes, null, null);
				}
			}
		};
		streamingVisumNetworkReader.addRowHandler("STRECKE", edgeRowHandler);
	
		logger.info("Reading visum network...");
		streamingVisumNetworkReader.read("/home/johannes/gsv/matsim/studies/netz2030/data/raw/roadnetwork.net");
		network.setCapacityPeriod(3600);
		
		logger.info("Cleaning network...");
		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(network);
		
		logger.info("Writing network...");
		NetworkWriter writer = new NetworkWriter(network);
		writer.write("/home/johannes/gsv/matsim/studies/netz2030/data/roadnetwork.80.xml");
	}
}
