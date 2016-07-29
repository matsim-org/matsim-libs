/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * After creating a config file with 
 * {@link org.matsim.contrib.emissions.example.CreateEmissionConfig CreateEmissionConfig}
 * this class runs a simulation and calculates emissions online. 
 * Results are written into distinct xml-files including emission event files for some iterations (as specified by the config). 
 * <p/>
 * See <a href="{@docRoot}/src-html/org/matsim/contrib/emissions/example/RunEmissionToolOnlineExample.html#line.39">here</a> for the listing.

 *
 * @author benjamin, julia
 */

public class RunEmissionToolOnlineExample {

	private static final String configFile = "./test/input/org/matsim/contrib/emissions/config.xml";
	
	private final Config config ;
	
	public RunEmissionToolOnlineExample( String[] args ) {
		if ( args==null || args.length==0 ) {
			config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup());
		} else {
			config = ConfigUtils.loadConfig( args[0], new EmissionsConfigGroup());
		}
	}	
	public final void run() {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addControlerListener(new EmissionControlerListener());
		controler.run();
	}
	public static void main(String[] args) {
		new RunEmissionToolOnlineExample(args).run();
	}
	public final Config getConfig() {
		return this.config;
	}

}