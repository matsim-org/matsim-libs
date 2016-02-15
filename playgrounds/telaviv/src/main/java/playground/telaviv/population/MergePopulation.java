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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.telaviv.config.TelAvivConfig;

/*
 * Merging internal and external population together.
 */
public class MergePopulation {
	
	private String internalPopulationFile = TelAvivConfig.basePath + "/population/internal_plans_10.xml.gz";
	private String externalCarPopulationFile = TelAvivConfig.basePath + "/population/external_plans_car_10.xml.gz";
	private String externalTruckPopulationFile = TelAvivConfig.basePath + "/population/external_plans_truck_10.xml.gz";
	private String externalCommercialPopulationFile = TelAvivConfig.basePath + "/population/external_plans_commercial_10.xml.gz";
	private String externalCarPopulationAttributesFile = TelAvivConfig.basePath + "/population/external_plans_attributes_car_10.xml.gz";
	private String externalTruckPopulationAttributesFile = TelAvivConfig.basePath + "/population/external_plans_attributes_truck_10.xml.gz";
	private String externalCommercialPopulationAttributesFile = TelAvivConfig.basePath + "/population/external_plans_attributes_commercial_10.xml.gz";
	
	private String outFile = TelAvivConfig.basePath + "/population/plans_10.xml.gz";
	private String outAttributesFile = TelAvivConfig.basePath + "/population/plans_attributes_10.xml.gz";
	
	private static final Logger log = Logger.getLogger(MergePopulation.class);
	
	public static void main(String[] args) {
		new MergePopulation(((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())));
	}
	
	public MergePopulation(Scenario scenario) {
		
		Config config;
		
		log.info("Loading internal population...");
		config = ConfigUtils.createConfig();
		config.plans().setInputFile(internalPopulationFile);
		Scenario internalScenario = ScenarioUtils.loadScenario(config);
		log.info("Found " + internalScenario.getPopulation().getPersons().size() + " internal persons.");
		
		log.info("Loading external car population...");
		config = ConfigUtils.createConfig();
		config.plans().setInputFile(externalCarPopulationFile);
		config.plans().setInputPersonAttributeFile(externalCarPopulationAttributesFile);
		Scenario externalCarScenario = ScenarioUtils.loadScenario(config);
		log.info("Found " + externalCarScenario.getPopulation().getPersons().size() + " external car persons.");
		
		log.info("Loading external truck population...");
		config = ConfigUtils.createConfig();
		config.plans().setInputFile(externalTruckPopulationFile);
		config.plans().setInputPersonAttributeFile(externalTruckPopulationAttributesFile);
		Scenario externalTruckScenario = ScenarioUtils.loadScenario(config);
		log.info("Found " + externalTruckScenario.getPopulation().getPersons().size() + " external truck persons.");
		
		log.info("Loading external commercial population...");
		config = ConfigUtils.createConfig();
		config.plans().setInputFile(externalCommercialPopulationFile);
		config.plans().setInputPersonAttributeFile(externalCommercialPopulationAttributesFile);
		Scenario externalCommercialScenario = ScenarioUtils.loadScenario(config);
		log.info("Found " + externalCommercialScenario.getPopulation().getPersons().size() + " external commercial persons.");
		
		log.info("Creating MATSim population...");		
		for (Person person : internalScenario.getPopulation().getPersons().values()) scenario.getPopulation().addPerson(person);
		
		for (Person person : externalCarScenario.getPopulation().getPersons().values()) {
			scenario.getPopulation().addPerson(person);

			String subPopulationAttribute = (String) externalCarScenario.getPopulation().getPersonAttributes().getAttribute(
					person.getId().toString(), TelAvivConfig.subPopulationConfigName);
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), 
					TelAvivConfig.subPopulationConfigName, subPopulationAttribute);
			
			String externalTripType = (String) externalCarScenario.getPopulation().getPersonAttributes().getAttribute(
					person.getId().toString(), TelAvivConfig.externalTripType);			
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), 
					TelAvivConfig.externalTripType, externalTripType);
		}
		for (Person person : externalTruckScenario.getPopulation().getPersons().values()) {
			scenario.getPopulation().addPerson(person);

			String subPopulationAttribute = (String) externalTruckScenario.getPopulation().getPersonAttributes().getAttribute(
					person.getId().toString(), TelAvivConfig.subPopulationConfigName);
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), 
					TelAvivConfig.subPopulationConfigName, subPopulationAttribute);
			
			String externalTripType = (String) externalTruckScenario.getPopulation().getPersonAttributes().getAttribute(
					person.getId().toString(), TelAvivConfig.externalTripType);			
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), 
					TelAvivConfig.externalTripType, externalTripType);
		}

		for (Person person : externalCommercialScenario.getPopulation().getPersons().values())  {
			scenario.getPopulation().addPerson(person);

			String subPopulationAttribute = (String) externalCommercialScenario.getPopulation().getPersonAttributes().getAttribute(
					person.getId().toString(), TelAvivConfig.subPopulationConfigName);
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), 
					TelAvivConfig.subPopulationConfigName, subPopulationAttribute);
			
			String externalTripType = (String) externalCommercialScenario.getPopulation().getPersonAttributes().getAttribute(
					person.getId().toString(), TelAvivConfig.externalTripType);			
			scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), 
					TelAvivConfig.externalTripType, externalTripType);
		}
		
		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outFile);
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(outAttributesFile);
		log.info("done.");
	}
}