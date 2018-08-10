/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.binder.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Minimal example how to start your matsim run with traffic signals
 * 
 * @author tthunig
 */
public class RunSignalSystemsExample {
	// do not change name of class; matsim book refers to it.  theresa, apr'18

	/**
	 * @param args the path to your config file
	 */
	public static void main(String[] args) {
		if (args.length==0 || args.length>1) {
			throw new RuntimeException("Please provide exactly one argument -- the path to your config.xml file.") ;
		}
		// --- create the config
		Config config = ConfigUtils.loadConfig(args[0]);

		run(config); // The run method is extracted so that a test can operate on it.
	}
	
	public static void run(Config config) {
		// --- create the scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// load the information about signals data (i.e. fill the SignalsData object) and add it to the scenario as scenario element
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		// --- create the controler
		Controler c = new Controler(scenario);
		// add the signals module to the simulation such that SignalsData is not only contained in the scenario but also used in the simulation
		c.addOverridingModule(new SignalsModule());

		// --- run the simulation
		c.run();
	}

}
