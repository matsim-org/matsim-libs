/* *********************************************************************** *
 * project: org.matsim.*
 * CapeTownNetworkSplitter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.gfipQueuePassing;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.network.LongLinkSplitter;

/**
 * This class runs some code for the Joubert & DeKoker paper on passing queues
 * for the GFIP network. The goal is to see what the impact on network size is
 * when splitting long links. That is, how does the number of nodes and links 
 * increase for different max-length-thresholds.
 * 
 * @author jwjoubert
 */
public class GfipNetworkSplitter {
	final private static Logger LOG = Logger.getLogger(GfipNetworkSplitter.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GfipNetworkSplitter.class.toString(), args);
		
		/* Parse the original network. */
		String networkFolder = args[0];
		networkFolder += networkFolder.endsWith("/") ? "" : "/";
		String inputNetwork = args[1];
		String outputNetworkPrefix = args[2];
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).parse(networkFolder + inputNetwork);
		
		List<Double> thresholds = new ArrayList<>();
		for(int i = 3; i < args.length; i++){
			thresholds.add(Double.parseDouble(args[i]));
		}

		Network newNetwork = null;
		for(double d : thresholds){
			newNetwork = splitNetwork(sc.getNetwork(), d);
			new NetworkWriter(newNetwork).write(String.format("%s%s_%04.0f.xml.gz", networkFolder, outputNetworkPrefix, d));
		}
		
		Header.printFooter();
	}
	
	
	public static Network splitNetwork(Network original, double threshold){
		LOG.info("------------------------------------------------------------------------");
		LOG.info("Splitting the links of a network: Threshold " + threshold + "...");
		LOG.info("Original network:");
		LOG.info("  Nodes: " + original.getNodes().size());
		LOG.info("  Links: " + original.getLinks().size());
		
		Network newNetwork = LongLinkSplitter.splitNetwork(original, threshold, false);
		LOG.info("------------------------------------------------------------------------");
		LOG.info("Done splitting links.");
		return newNetwork;
	}

}
