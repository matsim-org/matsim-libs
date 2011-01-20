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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;

/*
 * Merging internal and external population together.
 */
public class MergePopulation {
	
	private String internalPopulationFile = "../../matsim/mysimulations/telaviv/population/internal_plans_10.xml.gz";
	private String externalPopulationFile = "../../matsim/mysimulations/telaviv/population/external_plans_10.xml.gz";
	private String outFile = "../../matsim/mysimulations/telaviv/population/plans_10.xml.gz";
	
	private static final Logger log = Logger.getLogger(MergePopulation.class);
	
	public static void main(String[] args) {
		new MergePopulation(new ScenarioImpl());
	}
	
	public MergePopulation(Scenario scenario) {
		log.info("Loading internal population...");
		Scenario internalScenario = new ScenarioImpl();
		new MatsimPopulationReader(internalScenario).readFile(internalPopulationFile);
		log.info("Found " + internalScenario.getPopulation().getPersons().size() + " internal Persons.");
		
		log.info("Loading external population...");
		Scenario externalScenario = new ScenarioImpl();
		new MatsimPopulationReader(externalScenario).readFile(externalPopulationFile);
		log.info("Found " + externalScenario.getPopulation().getPersons().size() + " external Persons.");
		
		log.info("Creating MATSim population...");		
		for (Person person : internalScenario.getPopulation().getPersons().values()) scenario.getPopulation().addPerson(person);
		for (Person person : externalScenario.getPopulation().getPersons().values()) scenario.getPopulation().addPerson(person);
		
		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(outFile);
		log.info("done.");
	}
}