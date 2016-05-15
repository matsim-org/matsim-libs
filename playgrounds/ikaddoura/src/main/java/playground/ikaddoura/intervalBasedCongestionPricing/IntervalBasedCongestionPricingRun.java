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
package playground.ikaddoura.intervalBasedCongestionPricing;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Starts an interval-based congestion pricing simulation run.
 * 
 * @author ikaddoura
 *
 */
public class IntervalBasedCongestionPricingRun {

	private static final Logger log = Logger.getLogger(IntervalBasedCongestionPricingRun.class);

	private static String configFile;
	private static double sigma;
	
	public static void main(String[] args) throws IOException {
		log.info("Starting simulation run with the following arguments:");
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			sigma = Double.valueOf(args[1]);		
			log.info("sigma: "+ sigma);

		} else {
			configFile = "../../../runs-svn/intervalBasedCongestionPricing/input/config.xml";
			sigma = 3.0;
		}

		IntervalBasedCongestionPricingRun main = new IntervalBasedCongestionPricingRun();
		main.run();
	}

	private void run() {

		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);

		final Builder factory = new Builder(TransportMode.car, config.planCalcScore());
		factory.setSigma(sigma);
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory );
			}
		});

		controler.addControlerListener(new IntervalBasedCongestionPricing(scenario));
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		
		controler.run();
	}
}

