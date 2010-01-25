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
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.vis.otfvis.OTFVisQueueSim;


public class FourWaysVisNetworkOnly {

	public static final String TESTINPUTDIR = "test/input/org/matsim/signalsystems/TravelTimeFourWaysTest/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String netFile = TESTINPUTDIR + "network.xml.gz";
		String lanesFile  = TESTINPUTDIR + "testLaneDefinitions_v1.1.xml";
		
		
		String[] netArray = {netFile};
		
		//this is run
//		OTFVis.playNetwork(netArray);
		//this is hack
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);
//		PopulationImpl population = scenario.getPopulation();
		EventsManagerImpl events = new EventsManagerImpl();
		
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions laneDefs = scenario.getLaneDefinitions();
		
		MatsimLaneDefinitionsReader lanesReader = new MatsimLaneDefinitionsReader(laneDefs);
		lanesReader.readFile(lanesFile);
		
		OTFVisQueueSim client = new OTFVisQueueSim(scenario, events);
		client.run();
		
		
	}

}
