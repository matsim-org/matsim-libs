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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripBasicAnalysisMain;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingV0;

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
			outputBaseDirectory = "../../../runs-svn/decongestion/output/decongestion_100it_V0_10it_timeBin10_0.0/";
		}
		
		DecongestionRun main = new DecongestionRun();
		main.run();
		
	}

	private void run() {

		Config config = ConfigUtils.loadConfig(configFile);	
		config.controler().setOutputDirectory(outputBaseDirectory);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
				
		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);

		// decongestion pricing
		final DecongestionControlerListener decongestion = new DecongestionControlerListener(info, new DecongestionTollingV0(info));		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().toInstance(decongestion);
			}
		});
		
		// toll-adjusted routing
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory(info, config.planCalcScore());
		travelDisutilityFactory.setSigma(0.);
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		}); 
				
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		log.info("Analyzing the final iteration...");
		PersonTripBasicAnalysisMain analysis = new PersonTripBasicAnalysisMain(scenario.getConfig().controler().getOutputDirectory());
		analysis.run();
	}
}

