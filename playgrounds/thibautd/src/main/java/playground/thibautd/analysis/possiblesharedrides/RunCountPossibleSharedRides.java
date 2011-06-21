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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Executable wrapper for {@link CountPossibleSharedRides}
 * @author thibautd
 */
public class RunCountPossibleSharedRides {
	private static final Logger log =
		Logger.getLogger(RunCountPossibleSharedRides.class);

	private static final int NUM_TIME_BINS = 100;
	private static final int NUM_BOXES_PER_DAY = 24*4;
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

		// load config
		Config config = ConfigUtils.loadConfig(fakeConfig);

		// init logger
		try {
			// create directory if does not exist
			path = config.controler().getOutputDirectory();
			if (!path.substring(path.length() - 1, path.length()).equals("/")) {
				path += "/";
			}
			File outputDir = new File(path);
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}

			// init logFile
			CollectLogMessagesAppender appender = new CollectLogMessagesAppender();
			Logger.getRootLogger().addAppender(appender);

			IOUtils.initOutputDirLogging(
				config.controler().getOutputDirectory(),
				appender.getLogEvents());
		} catch (IOException e) {
			//log.error("could not create log file");
			// do NOT continue without proper logging!
			throw new RuntimeException("error while creating log file",e);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsAccumulator eventsAccumulator = new EventsAccumulator();
		eventsManager.addHandler(eventsAccumulator);


		
		(new MatsimEventsReader(eventsManager)).readFile(eventFile);

		CountPossibleSharedRides countRides = new CountPossibleSharedRides(
				network, eventsAccumulator, population, ACCEPTABLE_DISTANCE, TIME_WINDOW);

		// TODO: process
		countRides.run();

		log.debug("writing charts in "+path+" ...");
		chart = countRides.getAveragePerTimeBinChart(NUM_TIME_BINS);
		chart.saveAsPng(path+"histogramm.png",1024,800);
		chart = countRides.getBoxAndWhiskersPerTimeBin(NUM_BOXES_PER_DAY);
		chart.saveAsPng(path+"boxAndWhisker.png",1024,800);
		log.debug("writing charts... DONE");

		log.debug("writing text file...");
		BufferedWriter writer = IOUtils.getBufferedWriter(path+"rawData.txt.gz");
		countRides.writeRawData(writer);
		try {
			writer.close();
		} catch (IOException e) {
			log.error("an error accoured while writing to file");
			e.printStackTrace();
		}
		log.debug("writing text file... DONE");

		log.info("RunCountPossibleSharedRides... DONE");

		IOUtils.closeOutputDirLogging();
	}
}

