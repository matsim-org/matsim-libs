/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package matrix;

import gnu.trove.TObjectLongHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class BetweennessLoader {

	public static TObjectLongHashMap<Link> loadBetweenness(String file, Network network) throws IOException {
		/*
		 * Create node list...
		 */
//		logger.info("Indexing nodes...");
		List<Node> nodeList = new ArrayList<Node>(network.getNodes().size());
		for(Node node : network.getNodes().values()) {
			nodeList.add(node);
		}
		/*
		 * Load betweenness data
		 */
		TObjectLongHashMap<Link> values = new TObjectLongHashMap<Link>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		int cnt = 0;
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(" ");
			
			int fromNodeIdx = Integer.parseInt(tokens[1]);
			int toNodeIdx = Integer.parseInt(tokens[3]);
			
			Node fromNode = nodeList.get(fromNodeIdx);
			Node toNode = nodeList.get(toNodeIdx);
			
			Link link = null;
			for(Link outLink : fromNode.getOutLinks().values()) {
				if(outLink.getToNode() == toNode)
					link = outLink;
			}
			
//			if(link == null) {
//				logger.warn("Link not found!");
//				System.exit(-1);
//			}
//			linkSet.add(link);
			
			line = reader.readLine();
			
			long value = Long.parseLong(line);
			if(value < 0) {
//				logger.warn("Value < 0!");
//				System.exit(-1);
			} else {
				values.put(link, value);
			}
			if(value < nodeList.size()) {
				cnt++;
			}
		}
		
		return values;
	}
}
