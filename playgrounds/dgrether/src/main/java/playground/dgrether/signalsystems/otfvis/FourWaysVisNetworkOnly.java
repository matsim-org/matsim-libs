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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;


public class FourWaysVisNetworkOnly {

	public static void main(String[] args) {

		String netFile = FourWaysVis.TESTINPUTDIR+ "network.xml.gz";
		String lanesFile  = FourWaysVis.TESTINPUTDIR + "testLaneDefinitions_v2.0.xml";
		
		String[] netArray = {netFile};

		Config config = ConfigUtils.createConfig();
		config.scenario().setUseLanes(true);
		config.network().setLaneDefinitionsFile(lanesFile);

		//this is run
//		OTFVis.playNetwork(netArray);
		//this is hack
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
//		PopulationImpl population = scenario.getPopulation();
		EventsManager events = EventsUtils.createEventsManager();


		QSim otfVisQSim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, otfVisQSim);
		OTFClientLive.run(scenario.getConfig(), server);
		otfVisQSim.run();
		
		
	}

}
