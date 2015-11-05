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
package playground.boescpa.topdad.subscenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author pboesch
 *
 */
public class ReducedLinkCapacity {
	
	public static void main(String[] args) {
		// Get network:
		String path2MATSimNetwork = args[0];
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(path2MATSimNetwork);
		Network network = scenario.getNetwork();
		
		// Change network:
		double speedFactor = Double.valueOf(args[1]);
		double capacityFactor = Double.valueOf(args[2]);
		for (Link link : network.getLinks().values()) {
			// Speed is scaled down according to the given factor
			link.setFreespeed(speedFactor*link.getFreespeed());
			// Capacity is scaled down according to the given factor, but never less than one
			link.setCapacity(Math.max(capacityFactor*link.getCapacity(), 1));
		}
		
		// Write network:
		NetworkWriter netWriter = new NetworkWriter(network);
		netWriter.write(args[3]);
	}
	
}
