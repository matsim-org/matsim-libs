/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.usecases;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis2.OTFVisClient;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

/**
 * @author mrieser
 */
public class UseCase5_OptimizedWithVis {

	public static void main(final String[] args) {

		String prefix = "../../MATSim/";

		// load data
		Config config;
		config = ConfigUtils.loadConfig(prefix + "test/scenarios/berlin/config.xml");
		ConfigUtils.modifyFilePaths(config, prefix);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		Scenario scenario = loader.loadScenario();
		System.out.println("# persons: " + scenario.getPopulation().getPersons().size());
		EventsManager events = new EventsManagerImpl();

		/* **************************************************************** */

		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("foobar", server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(true);
		client.run();

		OptimizedCarSimFactory simFactory = new OptimizedCarSimFactory(2);
		simFactory.setTeleportedModes(new String[] {TransportMode.bike, TransportMode.pt, TransportMode.ride, TransportMode.walk});
		simFactory.setOtfvisServer(server);
		Simulation sim = simFactory.createMobsim(scenario, events);
		sim.run(); // replace with PlanSimulation.runMobsim();


		/* **************************************************************** */

	}
}
