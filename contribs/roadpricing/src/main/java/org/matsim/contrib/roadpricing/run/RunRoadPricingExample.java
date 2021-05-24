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
import org.matsim.contrib.roadpricing.RoadPricing;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Basic "script" to use roadpricing.
 *
 * <br><br>
 * <b>Note:</b> Do not change the class name as it is referenced in the book.
 *
 * @author nagel, jwjoubert
 */
public final class RunRoadPricingExample {
	// do not change class name: referenced from book.  kai, dec'14
	
	/**
	 * Executing the road pricing example.
	 * @param args optional arguments, the first which must be a <code>config.xml</code>
	 *             file. If no arguments are passed, the class will use the config, and
	 *             associated files from a test instance.
	 */
	public static void main(String[] args) {

		// load the config:
		Config config = ConfigUtils.loadConfig( args );

		// load the scenario:
		Scenario scenario = ScenarioUtils.loadScenario( config );

		// instantiate the controler:
		Controler controler = new Controler(scenario);

		// use the road pricing module:
		RoadPricing.configure( controler );

		// run the controler:
		controler.run();
	}

}
