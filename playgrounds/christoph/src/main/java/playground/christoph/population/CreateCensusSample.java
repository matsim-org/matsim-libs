/* *********************************************************************** *
 * project: org.matsim.*
 * CreateCensusSample.java
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

import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.knowledges.Knowledges;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreateCensusSample {

	private static final Logger log = Logger.getLogger(CreateCensusSample.class);

	public static void main(String[] args) {
		if (args.length != 10) return;
		else new CreateCensusSample(Double.valueOf(args[0]), Integer.valueOf(args[1]), args[2], args[3], args[4], 
				args[5], args[6], args[7], args[8], args[9]);
	}
		
	/**
	 * If we create a sample population from a Census2000V2 population where we
	 * also use households, we cannot just draw a random sample. Instead we have
	 * to draw a sample of households, including all members of a selected
	 * household.
	 * 
	 * However, there are also some large collective households where
	 * we draw a sample of the household members. By doing so, we ensure that
	 * each of those collective households exists also in the sample population.
	 * This might be important for e.g. hospitals or schools.
	 * 
	 * Additionally, this should help to avoid some scaling problems (e.g. a
	 * large household with 1000+ members could be located at a link that is
	 * scaled down to 10%).
	 * 
	 * @param fraction ... the fraction of the to be created population
	 * @param minCollectiveSize ... minimum size of a collective household to be treated as a collective household.
	 * 		Otherwise it is handled as a non collective household.
	 * @param populationFile ... the input population file
	 * @param networkFile ... the input network file
	 * @param facilitiesFile ... the input facilities file
	 * @param householdsFile ... the input households file
	 * @param objectAttributesFile ... the input household object attributes file
	 * @param outPopulationFile ... the output population file
	 * @param outHouseholdsFile ... the output households file
	 * @param outObjectAttributesFile ... the output household object attributes file
	 */
	public CreateCensusSample(double fraction, int minCollectiveSize, String populationFile, String networkFile, String facilitiesFile,
			String householdsFile, String objectAttributesFile, String outPopulationFile, String outHouseholdsFile, String outObjectAttributesFile) {
		
		log.info("Sample fraction: " + fraction);
		log.info("Minimum size of collective households to be sampled within the household: " + minCollectiveSize);
		
		if (fraction <= 0.0 || fraction >= 1.0) {
			log.error("Fraction is expected to be > 0.0 and < 1.0!"); return;
		}
		if (minCollectiveSize < 10) log.warn("Suggest minCollectiveSize not to be smaller than 10!");
		
		Random random = MatsimRandom.getLocalInstance();
		
		Config config = ConfigUtils.createConfig();
		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesFile);
		config.households().setInputFile(householdsFile);
		config.scenario().setUseKnowledge(true);
		config.scenario().setUseHouseholds(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		ObjectAttributes householdAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdAttributes).parse(objectAttributesFile);
		
		int inputPopulation = scenario.getPopulation().getPersons().size();
		log.info("Input population size: " + inputPopulation);
		log.info("Expected output population size: " + Math.round(scenario.getPopulation().getPersons().size() * fraction));
		
		Households households = ((ScenarioImpl) scenario).getHouseholds();
		Knowledges knowledges = ((ScenarioImpl) scenario).getKnowledges();
		
		Counter removedHouseholds = new Counter("Removed households ");
		Counter removedPersons = new Counter("Removed persons ");
		
		Iterator<Household> iter = households.getHouseholds().values().iterator();
		while (iter.hasNext()) {
			Household household = iter.next();
			
			Id householdId = household.getId();
			int HHTP = (Integer) householdAttributes.getAttribute(householdId.toString(), "HHTP"); 
			
			/*
			 * If it is no collective household we either the the entire household or remove the entire household.
			 * If it is a very small collective household, we can handle it like a general household.
			 */
			if (HHTP < 9000 || household.getMemberIds().size() < minCollectiveSize) {
				
				double r = random.nextDouble();
				
				/*
				 * Remove the household and its population?
				 */
				if (r > fraction) {
					// remove household
					iter.remove();
					
					// remove persons
					for (Id personId : household.getMemberIds()) {
						scenario.getPopulation().getPersons().remove(personId);
						removedPersons.incCounter();
					}
					
					// remove object attributes
					householdAttributes.removeAllAttributes(householdId.toString());

					removedHouseholds.incCounter();
				}
				
			} else {
				Iterator<Id> personIter = household.getMemberIds().iterator();
				while (personIter.hasNext()) {
					Id personId = personIter.next();					
					
					double r = random.nextDouble();
					
					/*
					 * Remove the person?
					 */
					if (r > fraction) {
						// remove person from household
						personIter.remove();
						
						// remove person from population
						scenario.getPopulation().getPersons().remove(personId);
						
						removedPersons.incCounter();
					}
				}
			}
		}
		removedPersons.printCounter();
		removedHouseholds.printCounter();
		log.info("Output population size: " + scenario.getPopulation().getPersons().size());
		log.info("Sample size: " + scenario.getPopulation().getPersons().size() * 100.0 / inputPopulation + "%");
		
		log.info("Writing population...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), knowledges).write(outPopulationFile);
		log.info("done.");
		
		log.info("Writing households...");
		new HouseholdsWriterV10(households).writeFile(outHouseholdsFile);
		log.info("done.");
		
		log.info("Writing object attributes...");
		new ObjectAttributesXmlWriter(householdAttributes).writeFile(outObjectAttributesFile);
		log.info("done.");
	}
}
