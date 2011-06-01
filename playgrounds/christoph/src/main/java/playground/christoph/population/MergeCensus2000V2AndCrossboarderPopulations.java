/* *********************************************************************** *
 * project: org.matsim.*
 * MergeCensus2000V2AndCrossboarderPopulations.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.knowledges.Knowledges;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class MergeCensus2000V2AndCrossboarderPopulations {

	private static final Logger log = Logger.getLogger(MergeCensus2000V2AndCrossboarderPopulations.class);
	
	public static void main(String[] args) {
		if (args.length != 11) return;
		
		new MergeCensus2000V2AndCrossboarderPopulations(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10]);
	}
	
	/**
	 * Merges Census2000V2 Population (including Households) with Crossboarder Population
	 * (also including Households). Additionally, for each Household ObjectAttributes are defined
	 * which are also merged.
	 * 
	 * @param networkFile ... the input network file
	 * @param facilitiesFile ... the input facilities file
	 * @param censusPopulationFile ... the census input population file
	 * @param crossboarderPopulationFile ... the crossboarder input population file
	 * @param censusHouseholdFile ... the census households file
	 * @param crossboarderHouseholdFile ... the crossboarder households file
	 * @param censusObjectAttributesFile ... the census household object attributes file
	 * @param crossboarderObjectAttributesFile ... the crossboarder household object attributes file
	 * @param outPopulationFile ... the merged output population file
	 * @param outHouseholdsFile ... the merged output households file
	 * @param outObjectAttributesFile... the merged output object attributes file
	 */
	public MergeCensus2000V2AndCrossboarderPopulations(String networkFile, String facilitiesFile, String censusPopulationFile, String crossboarderPopulationFile,
			String censusHouseholdFile, String crossboarderHouseholdFile, String censusObjectAttributesFile, String crossboarderObjectAttributesFile,
			String outPopulationFile, String outHouseholdsFile, String outObjectAttributesFile) {
		
		log.info("Loading census population...");
		Config censusConfig = ConfigUtils.createConfig();
		censusConfig.scenario().setUseKnowledge(true);
		censusConfig.scenario().setUseHouseholds(true);
		censusConfig.plans().setInputFile(censusPopulationFile);
		censusConfig.network().setInputFile(networkFile);
		censusConfig.facilities().setInputFile(facilitiesFile);
		censusConfig.households().setInputFile(censusHouseholdFile);
		Scenario censusScenario = ScenarioUtils.loadScenario(censusConfig);
		ObjectAttributes censusObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(censusObjectAttributes).parse(censusObjectAttributesFile);
		log.info("done.");
		
		log.info("Loading crossboarder population...");
		Config crossboarderConfig = ConfigUtils.createConfig();
		crossboarderConfig.scenario().setUseKnowledge(true);
		crossboarderConfig.scenario().setUseHouseholds(true);
		crossboarderConfig.plans().setInputFile(crossboarderPopulationFile);
		crossboarderConfig.network().setInputFile(networkFile);
		crossboarderConfig.facilities().setInputFile(facilitiesFile);
		crossboarderConfig.households().setInputFile(crossboarderHouseholdFile);
		Scenario crossboarderScenario = ScenarioUtils.loadScenario(crossboarderConfig);
		ObjectAttributes crossboarderObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(crossboarderObjectAttributes).parse(crossboarderObjectAttributesFile);
		log.info("done.");
		
		log.info("Merging populations...");
		Config mergedConfig = ConfigUtils.createConfig();
		mergedConfig.scenario().setUseKnowledge(true);
		mergedConfig.scenario().setUseHouseholds(true);
		Scenario mergedScenario = ScenarioUtils.createScenario(mergedConfig);
		ObjectAttributes mergedObjectAttributes = new ObjectAttributes();
		Population population = mergedScenario.getPopulation();
		Households households = ((ScenarioImpl) mergedScenario).getHouseholds();
		Knowledges knowledges = ((ScenarioImpl) mergedScenario).getKnowledges();

		// merge households
		households.getHouseholds().putAll(((ScenarioImpl) censusScenario).getHouseholds().getHouseholds());
		households.getHouseholds().putAll(((ScenarioImpl) crossboarderScenario).getHouseholds().getHouseholds());
		
		// merge knowledges
		knowledges.getKnowledgesByPersonId().putAll(((ScenarioImpl) censusScenario).getKnowledges().getKnowledgesByPersonId());
		knowledges.getKnowledgesByPersonId().putAll(((ScenarioImpl) crossboarderScenario).getKnowledges().getKnowledgesByPersonId());
		
		// merge population
		for (Person person : censusScenario.getPopulation().getPersons().values()) population.addPerson(person);
		for (Person person : crossboarderScenario.getPopulation().getPersons().values()) population.addPerson(person);
		
		// merge object attributes
		for (Household household : ((ScenarioImpl) censusScenario).getHouseholds().getHouseholds().values()) {
			mergedObjectAttributes.putAttribute(household.getId().toString(), "HHTP", censusObjectAttributes.getAttribute(household.getId().toString(), "HHTP"));
			mergedObjectAttributes.putAttribute(household.getId().toString(), "homeFacilityId", censusObjectAttributes.getAttribute(household.getId().toString(), "homeFacilityId"));
			mergedObjectAttributes.putAttribute(household.getId().toString(), "municipality", censusObjectAttributes.getAttribute(household.getId().toString(), "municipality"));
			mergedObjectAttributes.putAttribute(household.getId().toString(), "x", censusObjectAttributes.getAttribute(household.getId().toString(), "x"));
			mergedObjectAttributes.putAttribute(household.getId().toString(), "y", censusObjectAttributes.getAttribute(household.getId().toString(), "y"));
		}
		for (Household household : ((ScenarioImpl) crossboarderScenario).getHouseholds().getHouseholds().values()) {
			mergedObjectAttributes.putAttribute(household.getId().toString(), "HHTP", crossboarderObjectAttributes.getAttribute(household.getId().toString(), "HHTP"));
			mergedObjectAttributes.putAttribute(household.getId().toString(), "homeFacilityId", crossboarderObjectAttributes.getAttribute(household.getId().toString(), "homeFacilityId"));
			mergedObjectAttributes.putAttribute(household.getId().toString(), "x", crossboarderObjectAttributes.getAttribute(household.getId().toString(), "x"));
			mergedObjectAttributes.putAttribute(household.getId().toString(), "y", crossboarderObjectAttributes.getAttribute(household.getId().toString(), "y"));
		}
		log.info("done.");
		
		// write households
		log.info("Writing merged households...");
		new HouseholdsWriterV10(households).writeFile(outHouseholdsFile);
		log.info("done.");
		
		// write population
		log.info("Writing merged population...");
		new PopulationWriter(population, censusScenario.getNetwork(), knowledges).write(outPopulationFile);
		log.info("done.");
		
		// write object attributes
		log.info("Writing merged household object attributes...");
		new ObjectAttributesXmlWriter(mergedObjectAttributes).writeFile(outObjectAttributesFile);
		log.info("done.");
	}
}
