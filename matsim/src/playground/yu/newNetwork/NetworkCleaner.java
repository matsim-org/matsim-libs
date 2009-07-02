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
import java.util.Set;
import java.util.TreeSet;

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
		final String inputNetFilename = "input/Toronto/toronto_m.xml";
		final String outputNetFilename = "output/Toronto/toronto_c.xml.gz";
		String logFilename = "output/Toronto/netCleaner.log";

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(inputNetFilename);
		Set<NodeImpl> nodesToRemove = new TreeSet<NodeImpl>();
		for (NodeImpl n : network.getNodes().values())
			if (n.getIncidentLinks().isEmpty())
				nodesToRemove.add(n);
		int count = 0;
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(logFilename);
			writer.write("Id of nodes removed from Toronto network\n");
			for (NodeImpl n : nodesToRemove) {
				network.removeNode(n);
				writer.write(count++ + n.toString() + "\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new NetworkWriter(network, outputNetFilename).write();
	}
}
