/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.newNetwork;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * this class will remove the nodes from network, who don't have incidents
 * links.
 * 
 * @author yu
 * 
 */
public class NetworkCleaner {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String inputNetFilename = "../berlin-bvg09/pt/nullfall_M44_344/test/network_original.xml";
		final String outputNetFilename = "../berlin-bvg09/pt/nullfall_M44_344/test/net.xml";
		String logFilename = "../berlin-bvg09/pt/nullfall_M44_344/test/net.log";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetFilename);
		Set<Node> nodesToRemove = new HashSet<Node>();
		for (Node n : network.getNodes().values())
			if (((NodeImpl) n).getIncidentLinks().isEmpty())
				nodesToRemove.add(n);
		int count = 0;
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(logFilename);
			writer.write("Id of nodes removed from " + inputNetFilename + "\n");
			for (Node n : nodesToRemove) {
				network.removeNode(n);
				writer.write(count++ + n.toString() + "\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new NetworkWriter(network).writeFile(outputNetFilename);
	}
}
