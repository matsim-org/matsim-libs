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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.ptproject.qsim.QSimFactory;

/**
 * @author mrieser
 */
public class UseCase2_RefSim {

	public static void main(final String[] args) {

		String prefix = "../../MATSim/";

		// load data
		Config config;
		try {
			config = ConfigUtils.loadConfig(prefix + "test/scenarios/berlin/config.xml");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		config.plans().setInputFile("test/scenarios/equil/plans1.xml");
//		config.plans().setInputFile("test/scenarios/berlin/plans_hwh_sample.xml");
		ConfigUtils.modifyFilePaths(config, prefix);
		ScenarioLoader loader = new ScenarioLoaderImpl(config);
		Scenario scenario = loader.loadScenario();
		System.out.println("# persons: " + scenario.getPopulation().getPersons().size());
		EventsManager events = new EventsManagerImpl();
		EventWriterXML ew;
		/* **************************************************************** */

		if (true) {
			ew = new EventWriterXML("testEventsNewBln.xml");
			events.addHandler(ew);
			Simulation sim = new RefSimFactory().createMobsim(scenario, events);
			sim.run(); // replace with PlanSimulation.runSim();
		} else {
			ew = new EventWriterXML("testEventsOldBln.xml");
			events.addHandler(ew);
			config.setQSimConfigGroup(new QSimConfigGroup());
//			config.getQSimConfigGroup().setEndTime(10.0 * 3600);
			Simulation oldSim = new QSimFactory().createMobsim(scenario, events);
			oldSim.run();
		}

		/* **************************************************************** */

		ew.closeFile();
	}
}
