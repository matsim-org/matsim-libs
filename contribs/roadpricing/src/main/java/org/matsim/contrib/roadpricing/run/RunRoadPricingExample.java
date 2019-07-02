/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.roadpricing.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Basic "script" to use roadpricing.
 * 
 * @author nagel
 */
public final class RunRoadPricingExample {
	private final String[] args;
	private Config config;
	// to not change class name: referenced from book.  kai, dec'14

	public static void main(String[] args) {
		new RunRoadPricingExample( args ).run() ;
	}

	private RunRoadPricingExample(String[] args) {
		this.args = args ;
	}

	public void run( ){
		if ( config==null ) {
			config = prepareConfig() ;
		}

		// load the scenario:
		Scenario scenario = ScenarioUtils.loadScenario(config ) ;

		// instantiate the controler:
		Controler controler = new Controler(scenario) ;

		// use the road pricing module.
		// (loads the road pricing scheme, uses custom travel disutility including tolls, etc.)
		//        controler.setModules(new ControlerDefaultsWithRoadPricingModule());
		controler.addOverridingModule(RoadPricingUtils.createModule() );

		// run the controler:
		controler.run() ;
	}

	private Config prepareConfig(){
		// load the config, telling it to "materialize" the road pricing section:
		config = ConfigUtils.loadConfig( args[0], RoadPricingUtils.createConfigGroup());
		return config ;
	}

}
