/* *********************************************************************** *
 * project: org.matsim.*
 * RunCountPossibleSharedRides.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.possiblesharedrides;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Executable wrapper for {@link CountPossibleSharedRides}
 * @author thibautd
 */
public class RunCountPossibleSharedRides {
	private static final Logger log =
		Logger.getLogger(RunCountPossibleSharedRides.class);

	private static final int NUM_TIME_BINS = 100;
	private static final double ACCEPTABLE_DISTANCE = 500d;
	private static final double TIME_WINDOW = 15*60d;

	/**
	 * Usage: RunCountPossibleSharedRides eventFile config outputPath.
	 *
	 * configFile must just define the network and the (output) plan file to consider.
	 */
	public static void main(String[] args) {
		String eventFile;
		String fakeConfig;
		//String networkFile;
		//String plansFile;
		ChartUtil chart;
		String path;
		
		try {
			eventFile = args[0];
			fakeConfig = args[1];
			//networkFile = args[1];
			//plansFile = args[2];
			//path = args[2];
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsAccumulator eventsAccumulator = new EventsAccumulator();
		//Controler dummyControler = new Controler(fakeConfig);
		//dummyControler.setOverwriteFiles(true);
		//dummyControler.run();
		//Network network = dummyControler.getScenario().getNetwork();
		//Population population = dummyControler.getScenario().getPopulation();
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, fakeConfig);
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		//(new MatsimNetworkReader(scenario)).readFile(networkFile);
		//(new MatsimPopulationReader(scenario)).readFile(plansFile);
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		eventsManager.addHandler(eventsAccumulator);
		
		(new MatsimEventsReader(eventsManager)).readFile(eventFile);

		CountPossibleSharedRides countRides = new CountPossibleSharedRides(
				network, eventsAccumulator, population, ACCEPTABLE_DISTANCE, TIME_WINDOW);

		// TODO: process
		countRides.run();

		path = config.controler().getOutputDirectory();
		log.debug("writing charts in "+path+"/ ...");
		chart = countRides.getAvergePerTimeBinChart(NUM_TIME_BINS);
		chart.saveAsPng(path+"/test.png",1024,800);
		log.debug("writing charts... DONE");
		log.info("RunCountPossibleSharedRides... DONE");
	}
}

