/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * return different scenarios used in almost every analysis code.
 * @author amit
 */
public class LoadMyScenarios {
	
	/**
	 * Returns scenario by reading input config file and inserting location of plans and network file.
	 */
	public static Scenario loadScenario(String populationFile, String networkFile, String configFile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
	
	/**
	 * Returns scenario by creating new config and inserting location of plans and network file.
	 */
	public static Scenario loadScenario(String populationFile, String networkFile) {
		Config config = new Config();
		config.addCoreModules();
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
	
	/**
	 * Returns last iterations (int) by reading input config file only.
	 */
	public static int getLastIteration(String configFile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		return config.controler().getLastIteration();
	}
	
}
