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
package playground.agarwalamit.utils;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * return different scenarios used in almost every analysis code.
 * @author amit
 */
public final class LoadMyScenarios {
	
	private LoadMyScenarios(){}
	
	private final static Logger LOG = Logger.getLogger(LoadMyScenarios.class);
	
	/**
	 * Returns scenario specified plans, network and config file.
	 */
	public static Scenario loadScenarioFromPlansNetworkAndConfig(String populationFile, String networkFile, String configFile) {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		config.plans().setInputFile(populationFile);
		config.plans().setInputPersonAttributeFile(null);
		config.vehicles().setVehiclesFile(null);
		config.network().setInputFile(networkFile);
        return ScenarioUtils.loadScenario(config);
	}
	
	/**
	 * Returns scenario using standard output files from specified outputdir. 
	 */
	public static Scenario loadScenarioFromOutputDir(String outputDir) {
		String configFile = outputDir+"/output_config.xml";
		String plansFile = outputDir+"/output_plans.xml.gz";
		String networkFile = outputDir+"/output_network.xml.gz";
		
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		config.plans().setInputFile(plansFile);
		config.network().setInputFile(networkFile);
		
		String personAttributeFile = outputDir+"/output_personAttributes.xml.gz";
		if ( new File(personAttributeFile).exists() )  {
			config.plans().setInputPersonAttributeFile(personAttributeFile);
		}
		
		String vehicleFile = outputDir+"/output_vehicles.xml.gz";
		if(new File(vehicleFile).exists()) {
			config.vehicles().setVehiclesFile(vehicleFile);
		}
		
		Scenario sc = ScenarioUtils.loadScenario(config);
		sc.getConfig().controler().setOutputDirectory(outputDir);
		return sc;
	}
	
	/**
	 * Returns scenario from specified plans and network file.
	 */
	public static Scenario loadScenarioFromPlansAndNetwork(String populationFile, String networkFile) {
		Config config = new Config();
		config.addCoreModules();
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
        return ScenarioUtils.loadScenario(config);
	}
	
	/**
	 * Returns scenario from specified network and config file.
	 */
	public static Scenario loadScenarioFromNetworkAndConfig(String networkFile, String configFile) {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(null);
		config.plans().setInputPersonAttributeFile(null);
		config.vehicles().setVehiclesFile(null);
        return ScenarioUtils.loadScenario(config);
	}
	
	
	/**
	 * Returns last iterations (int) by reading input config file only and without loading scenario.
	 */
	public static int getLastIteration(String configFile) {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		return config.controler().getLastIteration();
	}

	/**
	 * Returns scenario containing only network file location.
	 */
	public static Scenario loadScenarioFromNetwork(String networkFile) {
		Config config = new Config();
		config.addCoreModules();
		config.network().setInputFile(networkFile);
        return ScenarioUtils.loadScenario(config);
	}
	
	/**
	 * Returns scenario containing only plans file location.
	 */
	public static Scenario loadScenarioFromPlans(String plansFile) {
		Config config = new Config();
		config.addCoreModules();
		config.plans().setInputFile(plansFile);
        return ScenarioUtils.loadScenario(config);
	}
	
	/**
	 * Returns scenario from plans and config.
	 */
	public static Scenario loadScenarioFromPlansAndConfig(String plansFile, String configFile) {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		config.plans().setInputFile(plansFile);
		config.plans().setInputPersonAttributeFile(null);
		config.network().setInputFile(null);
		config.vehicles().setVehiclesFile(null);
        return ScenarioUtils.loadScenario(config);
	}
	
	/**
	 * Returns simulation end time by reading config file and without loading scenario.
	 */
	public static Double getSimulationEndTime(String configfile) {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		LOG.info("Simulation end time is: " + endTime / 3600 + " hours.");
		return endTime;
	}
	
	/**
	 * Returns config by storing network and plans file locations and reading config files.
	 */
	public static Config getConfigFromPlansNetworkAndConfigFiles(String populationFile, String networkFile, String configFile) {
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
		config.plans().setInputFile(populationFile);
		config.plans().setInputPersonAttributeFile(null);
		config.network().setInputFile(networkFile);
		return config;
	}
}
