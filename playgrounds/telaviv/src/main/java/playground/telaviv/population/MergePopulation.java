/* *********************************************************************** *
 * project: org.matsim.*
 * MergePopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.telaviv.config.TelAvivConfig;

/*
 * Merging internal and external population together.
 */
public class MergePopulation {
	
	private String internalPopulationFile = TelAvivConfig.basePath + "/population/internal_plans_10.xml.gz";
	private String externalCarPopulationFile = TelAvivConfig.basePath + "/population/external_plans_car_10.xml.gz";
	private String externalTruckPopulationFile = TelAvivConfig.basePath + "/population/external_plans_truck_10.xml.gz";
	private String externalCommercialPopulationFile = TelAvivConfig.basePath + "/population/external_plans_commercial_10.xml.gz";
	private String outFile = TelAvivConfig.basePath + "/population/plans_10.xml.gz";
	
	private static final Logger log = Logger.getLogger(MergePopulation.class);
	
	public static void main(String[] args) {
		new MergePopulation(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
	}
	
	public MergePopulation(Scenario scenario) {
		log.info("Loading internal population...");
		Scenario internalScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(internalScenario).readFile(internalPopulationFile);
		log.info("Found " + internalScenario.getPopulation().getPersons().size() + " internal persons.");
		
		log.info("Loading external car population...");
		Scenario externalCarScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(externalCarScenario).readFile(externalCarPopulationFile);
		log.info("Found " + externalCarScenario.getPopulation().getPersons().size() + " external car persons.");
		
		log.info("Loading external truck population...");
		Scenario externalTruckScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(externalTruckScenario).readFile(externalTruckPopulationFile);
		log.info("Found " + externalTruckScenario.getPopulation().getPersons().size() + " external truck persons.");
		
		log.info("Loading external commercial population...");
		Scenario externalCommercialScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(externalCommercialScenario).readFile(externalCommercialPopulationFile);
		log.info("Found " + externalCommercialScenario.getPopulation().getPersons().size() + " external commercial persons.");
		
		log.info("Creating MATSim population...");		
		for (Person person : internalScenario.getPopulation().getPersons().values()) scenario.getPopulation().addPerson(person);
		for (Person person : externalCarScenario.getPopulation().getPersons().values()) scenario.getPopulation().addPerson(person);
		for (Person person : externalTruckScenario.getPopulation().getPersons().values()) scenario.getPopulation().addPerson(person);
		for (Person person : externalCommercialScenario.getPopulation().getPersons().values()) scenario.getPopulation().addPerson(person);
		
		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outFile);
		log.info("done.");
	}
}