/* *********************************************************************** *
 * project: org.matsim.*
 * ZurichVisNetworkOnly
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
package playground.dgrether.signalsystems.otfvis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class ZurichVisNetworkOnly {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFile = DgPaths.IVTCHNET;
		String lanesFile  = DgPaths.STUDIESDG + "signalSystemsZh/laneDefinitions.xml";
//		String lanesFile  = DgPaths.STUDIESDG + "lsaZurich/laneDefinitions_v1.1.xml";
		
		String[] netArray = {netFile};
		
		//this is run
//		OTFVis.playNetwork(netArray);
		//this is hack
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);
//		PopulationImpl population = scenario.getPopulation();
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions laneDefs = scenario.getLaneDefinitions();
		
		MatsimLaneDefinitionsReader lanesReader = new MatsimLaneDefinitionsReader(laneDefs);
		lanesReader.readFile(lanesFile);
		QSim otfVisQSim = new QSim(scenario, events);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		
		otfVisQSim.run();
	}

}
