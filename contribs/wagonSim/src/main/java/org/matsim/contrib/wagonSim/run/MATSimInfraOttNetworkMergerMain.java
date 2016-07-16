/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.wagonSim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author balmermi
 *
 */
public class MATSimInfraOttNetworkMergerMain {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(MATSimInfraOttNetworkMergerMain.class);
	
	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public MATSimInfraOttNetworkMergerMain() {
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void mergeNetworks(Network networkInfra, Network networkOtt) {
		// adding the infra network to the merged network
		Network network = scenario.getNetwork();
		NetworkFactory factory = scenario.getNetwork().getFactory();

		for (Node node : networkInfra.getNodes().values()) {
			Node n = factory.createNode(node.getId(),node.getCoord());
			network.addNode(n);
		}
		for (Link link : networkInfra.getLinks().values()) {
			Link l = factory.createLink(link.getId(),network.getNodes().get(link.getFromNode().getId()),network.getNodes().get(link.getToNode().getId()));
			l.setLength(link.getLength());
			l.setFreespeed(link.getFreespeed());
			l.setCapacity(link.getCapacity());
			l.setNumberOfLanes(link.getNumberOfLanes());
			l.setAllowedModes(link.getAllowedModes());
			network.addLink(l);
		}
		
		// add the links of the ott network
		for (Link link : networkOtt.getLinks().values()) {
			Link l = factory.createLink(link.getId(),network.getNodes().get(link.getFromNode().getId()),network.getNodes().get(link.getToNode().getId()));
			l.setLength(link.getLength());
			l.setFreespeed(link.getFreespeed());
			l.setCapacity(link.getCapacity());
			l.setNumberOfLanes(link.getNumberOfLanes());
			l.setAllowedModes(link.getAllowedModes());
			network.addLink(l);
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	public final Network getMergedNetwork() {
		return this.scenario.getNetwork();
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		args = new String[] {
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/networkNemoInfra/network.infra.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/network.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/networkMerged",
//		};
		
		if (args.length != 3) {
			log.error(MATSimInfraOttNetworkMergerMain.class.getCanonicalName()+" matsimInfraNetworkFile matsimOttNetworkFile outputBase");
			System.exit(-1);
		}
		
		String matsimInfraNetworkFile = args[0];
		String matsimOttNetworkFile = args[1];
		String outputBase = args[2];
		
		log.info("Main: "+MATSimInfraOttNetworkMergerMain.class.getCanonicalName());
		log.info("matsimInfraNetworkFile: "+matsimInfraNetworkFile);
		log.info("matsimOttNetworkFile: "+matsimOttNetworkFile);
		log.info("outputBase: "+outputBase);
		
		Scenario scenarioInfra = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenarioInfra.getNetwork()).readFile(matsimInfraNetworkFile);
		Scenario scenarioOtt = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenarioOtt.getNetwork()).readFile(matsimOttNetworkFile);
		
		MATSimInfraOttNetworkMergerMain merger = new MATSimInfraOttNetworkMergerMain();
		merger.mergeNetworks(scenarioInfra.getNetwork(),scenarioOtt.getNetwork());
		
		if (!Utils.prepareFolder(outputBase)) {
			throw new RuntimeException("Could not prepare output folder for one of the three reasons: (i) folder exists and is not empty, (ii) it's a path to an existing file or (iii) the folder could not be created. Bailing out.");
		}
		
		new NetworkWriter(merger.getMergedNetwork()).write(outputBase+"/network.merged.xml.gz");
	}
}
