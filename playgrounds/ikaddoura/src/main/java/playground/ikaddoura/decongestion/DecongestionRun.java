/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
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

/**
 * 
 */
package playground.ikaddoura.decongestion;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripBasicAnalysisMain;
import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.ikaddoura.decongestion.data.DecongestionInfo;

/**
 * Starts an interval-based decongestion pricing simulation run.
 * 
 * @author ikaddoura
 *
 */
public class DecongestionRun {

	private static final Logger log = Logger.getLogger(DecongestionRun.class);

	private static String configFile;
	private static String outputBaseDirectory;
	
	public static void main(String[] args) throws IOException {
		log.info("Starting simulation run with the following arguments:");
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			outputBaseDirectory = args[1];		
			log.info("output directory: "+ outputBaseDirectory);

		} else {
			configFile = "../../../runs-svn/decongestion/input/config.xml";
			outputBaseDirectory = "../../../runs-svn/decongestion/output_final/";
		}
		
		DecongestionRun main = new DecongestionRun();
		main.run();
		
	}

	private void run() throws IOException {

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		
		Config config = ConfigUtils.loadConfig(configFile);	
		config.controler().setOutputDirectory(outputBaseDirectory + "decongestion_total" + config.controler().getLastIteration() +
				"it_" + decongestionSettings.getTOLLING_APPROACH() + "_priceUpdate" + decongestionSettings.getUPDATE_PRICE_INTERVAL() +
				"it_timeBinSize" + config.travelTimeCalculator().getTraveltimeBinSize() + "_adjustment" + decongestionSettings.getTOLL_ADJUSTMENT() +
				"_BrainExpBeta" + config.planCalcScore().getBrainExpBeta() + "_blendFactor" + decongestionSettings.getTOLL_BLEND_FACTOR() +
				"_toleratedDelay" + decongestionSettings.getTOLERATED_AVERAGE_DELAY_SEC()
				+ "_0.3_7200_5plans_test/");
		Scenario scenario = ScenarioUtils.loadScenario(config);
				
		final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
		Decongestion decongestion = new Decongestion(info);
		decongestion.run();
		
		log.info("Analyzing the final iteration...");
		PersonTripBasicAnalysisMain analysis = new PersonTripBasicAnalysisMain(scenario.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		MATSimVideoUtils.createLegHistogramVideo(config.controler().getOutputDirectory());
		MATSimVideoUtils.createVideo(config.controler().getOutputDirectory(), decongestionSettings.getWRITE_OUTPUT_ITERATION(), "tolls");
	}
}

