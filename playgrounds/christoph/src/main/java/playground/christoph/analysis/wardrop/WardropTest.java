/* *********************************************************************** *
 * project: org.matsim.*
 * WardropTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.analysis.wardrop;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;


public class WardropTest {


	//protected static ScenarioData scenarioData;
	protected static ScenarioImpl scenario;
	protected static Population population;
	protected static NetworkImpl network;
	protected static Config config;
//	protected static NetworkFactory networkFactory = new NetworkFactory();

	private static final Logger log = Logger.getLogger(WardropTest.class);

	/**
	 * This variable is used to store the log4j output before it can be written to
	 * a file. This is needed to set the output directory before logging.
	 */
	private static CollectLogMessagesAppender collectLogMessagesAppender = new CollectLogMessagesAppender();


	public static void main(String[] args)
	{
		initLogging();

		//String configFileName = "mysimulations\\kt-zurich\\config10pct_factor_0.10_replanning.xml";
		//String configFileName = "C:\\Master_Thesis_HLI\\Workspace\\myMATSIM\\mysimulations\\kt-zurich\\config10pct_factor_0.10_replanning.xml";
		String configFileName = "mysimulations/kt-zurich/config10pct_factor_0.10_replanning.xml";

		String dtdFileName = "";

		config = new Config();
		config.addCoreModules();

		new MatsimConfigReader(config).readFile(configFileName);

/*
		try {
			new MatsimConfigReader(config).readFile(configFileName, dtdFileName);
		}
		catch (IOException e)
		{
			log.error("Problem loading the configuration file from " + configFileName);
			throw new RuntimeException(e);
		}
*/

		//scenarioData = new ScenarioImpl(config, networkFactory);
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		network = scenario.getNetwork();
		population = scenario.getPopulation();

		Wardrop wardrop = new Wardrop(network, population);

		wardrop.calcMeanLinksPerTrip();

//		wardrop.fillMatrixFromEventFile("C:\\events.txt.gz");
//		wardrop.getResults();

		// read events from File
		wardrop.fillMatrixFromEventFile("C:\\0.events.txt.gz");

		//wardrop.setActTimesCollector(this.actTimesCollector);
/*
		// with length Correction
		wardrop.setUseLengthCorrection(true);
//		wardrop.fillMatrixFromEventFile("C:\\0.events.txt.gz");
		wardrop.getResults();
		log.info("");
*/

		// without length Correction
		wardrop.setUseLengthCorrection(false);
//		wardrop.fillMatrixFromEventFile("C:\\0.events.txt.gz");
		wardrop.fillMatrixViaActTimesCollectorObject();
		wardrop.getResults();

		log.info("");
		log.info("Done!");

		IOUtils.closeOutputDirLogging();
		IOUtils.renameFile("C:\\" + IOUtils.LOGFILE, "C:\\WardropLog.log");
	}

	/**
	 * Initializes log4j to write log output to files in output directory.
	 */
	private static void initLogging() {
		Logger.getRootLogger().removeAppender(collectLogMessagesAppender);
		try
		{
			IOUtils.initOutputDirLogging("C:\\", collectLogMessagesAppender.getLogEvents());
		}
		catch (IOException e)
		{
			log.error("Cannot create logfiles: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
