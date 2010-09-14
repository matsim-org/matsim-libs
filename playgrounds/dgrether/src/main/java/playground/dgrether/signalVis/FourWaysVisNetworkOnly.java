/* *********************************************************************** *
 * project: org.matsim.*
 * FourWaysVis
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
package playground.dgrether.signalVis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;


public class FourWaysVisNetworkOnly {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String netFile = FourWaysVis.TESTINPUTDIR+ "network.xml.gz";
		String lanesFile  = FourWaysVis.TESTINPUTDIR + "testLaneDefinitions_v1.1.xml";
		
		
		String[] netArray = {netFile};
		
		//this is run
//		OTFVis.playNetwork(netArray);
		//this is hack
		ScenarioImpl scenario = new ScenarioImpl();
    scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);
//		PopulationImpl population = scenario.getPopulation();
		EventsManagerImpl events = new EventsManagerImpl();
		
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions laneDefs = scenario.getLaneDefinitions();
		
		MatsimLaneDefinitionsReader lanesReader = new MatsimLaneDefinitionsReader(laneDefs);
		lanesReader.readFile(lanesFile);
		QSim otfVisQSim = new QSim(scenario, events);
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(otfVisQSim);
		otfVisQSim.addFeature(queueSimulationFeature);
		queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
		
		QSim client = otfVisQSim;
		client.run();
		
		
	}

}
