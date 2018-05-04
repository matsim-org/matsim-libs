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
package org.matsim.contrib.decongestion;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.IntegralApproach;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Starts an interval-based decongestion pricing simulation run.
 * 
 * @author ikaddoura
 *
 */
public class DecongestionRunExample {

	private static final Logger log = Logger.getLogger(DecongestionRunExample.class);

	private static String configFile;
	
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			log.info("Starting simulation run with the following arguments:");

			configFile = args[0];		
			log.info("config file: "+ configFile);

		} else {
			configFile = "path/to/config.xml";
		}
		
		DecongestionRunExample main = new DecongestionRunExample();
		main.run();
		
	}

	private void run() throws IOException {

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setToleratedAverageDelaySec(30.);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
		decongestionSettings.setUpdatePriceInterval(1);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		
//		decongestionSettings.setDecongestionApproach(DecongestionApproach.P_MC);
		
		decongestionSettings.setDecongestionApproach(DecongestionApproach.PID);
		decongestionSettings.setKd(0.005);
		decongestionSettings.setKi(0.005);
		decongestionSettings.setKp(0.005);
		decongestionSettings.setIntegralApproach(IntegralApproach.UnusedHeadway);
		decongestionSettings.setIntegralApproachUnusedHeadwayFactor(10.0);
		decongestionSettings.setIntegralApproachAverageAlpha(0.0);
		
//		decongestionSettings.setDecongestionApproach(DecongestionApproach.BangBang);
//		decongestionSettings.setTOLL_ADJUSTMENT(1.0);
//		decongestionSettings.setINITIAL_TOLL(1.0);
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.addModule(decongestionSettings);
								
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// #############################################################
		
		// congestion toll computation
		
		controler.addOverridingModule(new DecongestionModule(scenario));
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		// #############################################################
	
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
        controler.run();
        
	}
}

