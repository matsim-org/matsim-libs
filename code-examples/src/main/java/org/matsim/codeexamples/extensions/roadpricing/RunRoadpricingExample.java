/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.codeexamples.extensions.roadpricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;


/**
 * Uses roadpricing extension, therefore needs to create config with RoadPricingConfigGroup
 * 
 * @author vsp-gleich
 *
 */
public class RunRoadpricingExample {

	public static void main(final String[] args) {
		String configFile ;
		if ( args!=null && args.length>=1 ) {
			configFile = args[0] ;
		} else {
			configFile = "scenarios/equil-extended/config-with-roadpricing.xml";
		}

		Config config = ConfigUtils.loadConfig(configFile, new RoadPricingConfigGroup());
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}

}
