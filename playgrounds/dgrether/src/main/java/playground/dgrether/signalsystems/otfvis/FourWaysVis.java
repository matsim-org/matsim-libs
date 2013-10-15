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
package playground.dgrether.signalsystems.otfvis;

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;


public class FourWaysVis {

	public static final String TESTINPUTDIR = "../../matsim/src/test/resources/test/input/org/matsim/signalsystems/TravelTimeFourWaysTest/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String netFile = TESTINPUTDIR + "network.xml.gz";
		String lanesFile  = TESTINPUTDIR + "testLaneDefinitions_v1.1.xml";
		String popFile = TESTINPUTDIR + "plans.xml.gz";
		String signalFile = TESTINPUTDIR + "testSignalSystems_v2.0.xml";
		String signalGroupsFile = TESTINPUTDIR + "testSignalGroups_v2.0.xml";
		String signalControlFile = TESTINPUTDIR + "testSignalControl_v2.0.xml";
		
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(netFile);
		scenario.getConfig().plans().setInputFile(popFile);
		scenario.getConfig().qsim().setSnapshotStyle("queue");
		scenario.getConfig().qsim().setStuckTime(100.0);
		
		scenario.getConfig().network().setLaneDefinitionsFile(lanesFile);
		scenario.getConfig().scenario().setUseLanes(true);
		
		scenario.getConfig().signalSystems().setSignalSystemFile(signalFile);
		scenario.getConfig().signalSystems().setSignalGroupsFile(signalGroupsFile);
		scenario.getConfig().signalSystems().setSignalControlFile(signalControlFile);
		scenario.getConfig().scenario().setUseSignalSystems(true);
		
		scenario.getConfig().otfVis().setAgentSize(130.0f);
		
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();
		
		EventsManager events = EventsUtils.createEventsManager();
		
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(scenario.getConfig().signalSystems());
		SignalsData signalsData = signalsLoader.loadSignalsData();
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME , signalsData);
		FromDataBuilder builder = new FromDataBuilder(scenario, events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);

		
		QSim otfVisQSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		otfVisQSim.addQueueSimulationListeners(engine);
		
		//		client.setConnectionManager(new DgConnectionManagerFactory().createConnectionManager());
//		client.setLaneDefinitions(scenario.getLaneDefinitions());
//		client.setSignalSystems(scenario.getSignalSystems(), scenario.getSignalSystemConfigurations());
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		
		otfVisQSim.run();
		
		
	}

}
