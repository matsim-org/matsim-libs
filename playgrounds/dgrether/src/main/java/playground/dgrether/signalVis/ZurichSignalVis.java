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
package playground.dgrether.signalVis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.vis.otfvis.OTFVisQueueSim;

import playground.dgrether.DgPaths;


/**
 * 105691.ol on Link Id 105691 and wants to go on to Link Id 105693 but there is no Lane leading to that Link!
 * @author dgrether
 *
 */
public class ZurichSignalVis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFile = DgPaths.IVTCHNET;
		String lanesFile  = DgPaths.STUDIESDG + "signalSystemsZh/laneDefinitions.xml";
//		String lanesFile  = DgPaths.STUDIESDG + "lsaZurich/laneDefinitions_v1.1.xml";
		String signalDefsFile = DgPaths.STUDIESDG + "signalSystemsZh/signalSystems.xml";
		String signalConfigsFile = DgPaths.STUDIESDG + "signalSystemsZh/signalSystemsConfig.xml";
		
		String popFile = DgPaths.IVTCHBASE + "baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";
		
		String[] netArray = {netFile};
		
		//this is run
//		OTFVis.playNetwork(netArray);
		//this is hack
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFile);
		new MatsimPopulationReader(scenario).readFile(popFile);

		EventsManagerImpl events = new EventsManagerImpl();
		
		scenario.getConfig().scenario().setUseLanes(true);
		LaneDefinitions laneDefs = scenario.getLaneDefinitions();
		MatsimLaneDefinitionsReader lanesReader = new MatsimLaneDefinitionsReader(laneDefs);
		lanesReader.readFile(lanesFile);
		
		scenario.getConfig().scenario().setUseSignalSystems(true);
		SignalSystems signalDefs = scenario.getSignalSystems();
		new MatsimSignalSystemsReader(signalDefs).readFile(signalDefsFile);
		
		SignalSystemConfigurations signalConfigs = scenario.getSignalSystemConfigurations();
		new MatsimSignalSystemConfigurationsReader(signalConfigs).readFile(signalConfigsFile);
		
		OTFVisQueueSim client = new OTFVisQueueSim(scenario, events);
		client.setConnectionManager(new DgConnectionManagerFactory().createConnectionManager());
		client.setLaneDefinitions(laneDefs);
		client.setSignalSystems(signalDefs, signalConfigs);
		client.run();
	}

}
