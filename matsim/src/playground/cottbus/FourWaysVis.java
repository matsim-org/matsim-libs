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
package playground.cottbus;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.dgrether.signalVis.DgOnTheFlyQueueSimQuad;

/**
 * @author	rschneid-btu
 * [completely taken from dgrether, only data paths customized]
 * experimental visualizer with light signals
 */

public class FourWaysVis {

	public static final String TESTINPUTDIR = "./input/denver/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String netFile = TESTINPUTDIR + "networkDenver.xml";
		String lanesFile  = TESTINPUTDIR + "laneDefinitions.xml";
		String popFile = TESTINPUTDIR + "plans.xml";
		String signalFile = TESTINPUTDIR + "signalSystems.xml";
		String signalConfigFile = TESTINPUTDIR + "signalSystemsConfigT44.xml";
		
		//String[] netArray = {netFile};
		
		//this is run
//		OTFVis.playNetwork(netArray);
		
		
		//this is hack
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().network().setInputFile(netFile);
		scenario.getConfig().plans().setInputFile(popFile);
		
		scenario.getConfig().network().setLaneDefinitionsFile(lanesFile);
		scenario.getConfig().scenario().setUseLanes(true);
		
		scenario.getConfig().signalSystems().setSignalSystemFile(signalFile);
		scenario.getConfig().signalSystems().setSignalSystemConfigFile(signalConfigFile);
		scenario.getConfig().scenario().setUseSignalSystems(true);
		
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();
		
		EventsManagerImpl events = new EventsManagerImpl();
		
		
		DgOnTheFlyQueueSimQuad client = new DgOnTheFlyQueueSimQuad(scenario, events);
		client.setLaneDefinitions(scenario.getLaneDefinitions());
		client.setSignalSystems(scenario.getSignalSystems(), scenario.getSignalSystemConfigurations());
		client.run();
		
		
	}

}
