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

package playground.christoph.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.knowledges.KnowledgeImpl;

/*
 * Merging internal and external population together.
 */
public class MergePopulation {
	
	private static final Logger log = Logger.getLogger(MergePopulation.class);

	private String internalPopulationFile = "../../matsim/mysimulations/crossboarder/plans_25viaGrep.xml.gz";
	private String externalPopulationFile = "../../matsim/mysimulations/crossboarder/plansCB_updated_25pct_with_facilities.xml.gz";
	private String networkFile = "../../matsim/mysimulations/crossboarder/network.xml.gz";
	private String facilitiesFile = "../../matsim/mysimulations/crossboarder/facilities.xml.gz";
	private String outFile = "../../matsim/mysimulations/crossboarder/plans_25_with_TTA.xml.gz";
		
	public static void main(String[] args) {
		new MergePopulation(new ScenarioImpl());
	}
	
	public MergePopulation(Scenario scenario) {
		
		Scenario internalScenario = new ScenarioImpl();

		log.info("Read Network File for internal population...");
		new MatsimNetworkReader(internalScenario).readFile(networkFile);
		log.info("done.");
		
		log.info("Reading facilities file for internal population...");
		new MatsimFacilitiesReader((ScenarioImpl)internalScenario).readFile(facilitiesFile);
		log.info("done.");
		
		log.info("Loading internal population...");
		new MatsimPopulationReader(internalScenario).readFile(internalPopulationFile);
		log.info("Found " + internalScenario.getPopulation().getPersons().size() + " internal Persons.");

		Scenario externalScenario = new ScenarioImpl();
		
		log.info("Read Network File for external population...");
		new MatsimNetworkReader(externalScenario).readFile(networkFile);
		log.info("done.");
		
		log.info("Reading facilities file for external population...");
		new MatsimFacilitiesReader((ScenarioImpl)externalScenario).readFile(facilitiesFile);
		log.info("done.");
		
		log.info("Loading external population...");
		new MatsimPopulationReader(externalScenario).readFile(externalPopulationFile);
		log.info("Found " + externalScenario.getPopulation().getPersons().size() + " external Persons.");
		
		log.info("Creating MATSim population...");	
		for (Person person : internalScenario.getPopulation().getPersons().values()){
			scenario.getPopulation().addPerson(person);
			KnowledgeImpl knowledge = ((ScenarioImpl)internalScenario).getKnowledges().getKnowledgesByPersonId().get(person.getId());
			((ScenarioImpl)scenario).getKnowledges().getKnowledgesByPersonId().put(person.getId(), knowledge);
		}
		for (Person person : externalScenario.getPopulation().getPersons().values()) {
			scenario.getPopulation().addPerson(person);
			KnowledgeImpl knowledge = ((ScenarioImpl)externalScenario).getKnowledges().getKnowledgesByPersonId().get(person.getId());
			((ScenarioImpl)scenario).getKnowledges().getKnowledgesByPersonId().put(person.getId(), knowledge);
		}
		log.info("done.");
				
		log.info("Writing MATSim population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), ((ScenarioImpl)scenario).getKnowledges()).write(outFile);
		log.info("done.");
	}
}
