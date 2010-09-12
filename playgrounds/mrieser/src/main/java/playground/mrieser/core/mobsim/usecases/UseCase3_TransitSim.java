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

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author mrieser
 */
public class UseCase3_TransitSim {

	public static void main(String[] args) {

		String prefix = "../../MATSim/";

		// load data
		Config config;
		try {
			config = ConfigUtils.loadConfig(prefix + "test/scenarios/equil/config.xml");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ConfigUtils.modifyFilePaths(config, prefix);
		ScenarioLoader loader = new ScenarioLoaderImpl(config);
		Scenario scenario = loader.loadScenario();
		EventsManager events = new EventsManagerImpl();
		EventWriterXML ew = new EventWriterXML("testEvents.xml");
		events.addHandler(ew);

		/* **************************************************************** */

		Simulation sim = new TransitSimFactory().createMobsim(scenario, events);
		sim.run(); // replace with PlanSimulation.runSim();

		/* **************************************************************** */

		ew.closeFile();
	}
}
