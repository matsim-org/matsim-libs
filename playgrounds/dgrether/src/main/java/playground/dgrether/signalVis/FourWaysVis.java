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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.vis.otfvis.OTFVisQueueSim;


public class FourWaysVis {

	public static final String TESTINPUTDIR = "../matsim/test/input/org/matsim/signalsystems/TravelTimeFourWaysTest/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String netFile = TESTINPUTDIR + "network.xml.gz";
		String lanesFile  = TESTINPUTDIR + "testLaneDefinitions_v1.1.xml";
		String popFile = TESTINPUTDIR + "plans.xml.gz";
		String signalFile = TESTINPUTDIR + "testSignalSystems_v1.1.xml";
		String signalConfigFile = TESTINPUTDIR + "testSignalSystemConfigurations_v1.1.xml";
		
		String[] netArray = {netFile};
		
		//this is run
//		OTFVis.playNetwork(netArray);
		
		
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().network().setInputFile(netFile);
		scenario.getConfig().plans().setInputFile(popFile);
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		scenario.getConfig().getQSimConfigGroup().setStuckTime(100.0);
		
		scenario.getConfig().network().setLaneDefinitionsFile(lanesFile);
		scenario.getConfig().scenario().setUseLanes(true);
		
		scenario.getConfig().signalSystems().setSignalSystemFile(signalFile);
		scenario.getConfig().signalSystems().setSignalSystemConfigFile(signalConfigFile);
//		scenario.getConfig().scenario().setUseSignalSystems(true);
		
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();
		
		EventsManager events = new EventsManagerImpl();
		
		OTFVisQueueSim client = new OTFVisQueueSim(scenario, events);
//		client.setConnectionManager(new DgConnectionManagerFactory().createConnectionManager());
		client.setLaneDefinitions(scenario.getLaneDefinitions());
		client.setSignalSystems(scenario.getSignalSystems(), scenario.getSignalSystemConfigurations());
		client.run();
		
		
	}

}
