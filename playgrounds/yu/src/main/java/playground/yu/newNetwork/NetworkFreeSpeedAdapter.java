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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;

/**
 * this class will remove the nodes from network, who don't have incidents
 * links.
 * 
 * @author yu
 * 
 */
public class NetworkFreeSpeedAdapter {

	public static void main(final String[] args) {
		final String inputNetFilename = "input/Toronto/connector_netclean.xml";
		final String outputNetFilename = "output/Toronto/connector_netclean_freeSpeed_modified.xml.gz";
		// String logFilename = "output/Toronto/FreeSpeedAdaptor.log";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(inputNetFilename);

		// try {
		// BufferedWriter writer = IOUtils.getBufferedWriter(logFilename);
		// writer.write("Id of nodes removed from Toronto network\n");
		for (LinkImpl l : network.getLinks().values()) {
			l.setFreespeed(l.getFreespeed(0.0) / 3.6);
		}
		// writer.close();
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		new NetworkWriter(network).writeFile(outputNetFilename);
	}
}
