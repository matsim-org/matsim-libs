/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlansGeneral.java
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

package playground.mfeil.creators;



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;


/**
 * This class reduces the freespeed of all links of a network by a certain factor
 * @author mfeil
 */
public class NetworkCapacityReducer {
	
	private static final Logger log = Logger.getLogger(NetworkCapacityReducer.class);
	private NetworkImpl network;
	
	public NetworkCapacityReducer(NetworkImpl network){
		this.network = network;
	}
	
	private void run (double factor, String output){
		for (LinkImpl link : this.network.getLinks().values()) {
			link.setFreespeed(link.getFreespeed(0)*factor);
		}
		new NetworkWriter(this.network).writeFile(output);
	}
	

	public static void main(final String [] args) {
		// Scenario files
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		
		// Output file
		final String outputFile = "/home/baug/mfeil/data/Zurich10/network_0.33.xml";	
		
		// Settings
		final double factor = 0.5;
		
	
		
		// Start calculations
		ScenarioImpl scenarioMATSim = new ScenarioImpl();
		new MatsimNetworkReader(scenarioMATSim).readFile(networkFilename);		
		
		NetworkCapacityReducer ncr = new NetworkCapacityReducer(scenarioMATSim.getNetwork());
		ncr.run(factor, outputFile);
		
		log.info("Reduction of network capacity finished.");
	}

}

