/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.accessibility.other;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;

/**
 * @author dziemke
 */
public class CreatePopulationFromFacilities {
//	private static final Logger LOG = Logger.getLogger(CreatePopulationFromFacilities.class);

	private static String inputOutputDirectory = "../../../shared-svn/projects/maxess/data/nairobi/facilities/2017-04-25_nairobi_central_and_kibera/";
	private static String facilitiesFile = inputOutputDirectory + "2017-04-25_facilities_landuse_buildings.xml";
	private static String populationFile = inputOutputDirectory + "2017-04-25_population_500.xml";
	private static int numberOfPersonsPerFacility = 500;

	public static void main(final String[] args) {
		List <ActivityFacility> homeFacilities = new ArrayList<>();
		List <ActivityFacility> workFacilities = new ArrayList<>();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);	
		MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario);
		reader.readFile(facilitiesFile);

		Population population = scenario.getPopulation();   
		PopulationFactory populationFactory = population.getFactory();

		Random random = MatsimRandom.getLocalInstance(); // Make sure that stream of random variables is reproducible.

		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption activityOption : facility.getActivityOptions().values()) {
				if (activityOption.getType().equals("home")) {
					homeFacilities.add(facility);
				}
				if (activityOption.getType().equals("work")) {
					workFacilities.add(facility);
				}
			}
		}
		System.out.println(homeFacilities.toString());

		for (ActivityFacility homeFacility : homeFacilities) {
			for (int i=0; i < numberOfPersonsPerFacility ; i++) {
				Person person = populationFactory.createPerson(Id.create(homeFacility.getId() + "_" + i, Person.class));
				population.addPerson(person);
	
				Plan plan = populationFactory.createPlan();
				person.addPlan(plan);
	
				Activity homeActivity = populationFactory.createActivityFromCoord("home", homeFacility.getCoord());
				homeActivity.setEndTime(6 * 3600 + 3 * random.nextDouble() * 3600);
				plan.addActivity(homeActivity);
				plan.addLeg(populationFactory.createLeg(TransportMode.pt));
	
				// Choose a random work facility
				ActivityFacility workFacility = workFacilities.get(random.nextInt(workFacilities.size()));
				Activity workActivity = populationFactory.createActivityFromCoord("work", workFacility.getCoord());
				workActivity.setEndTime(15 * 3600 + 3 * random.nextDouble() * 3600);
				plan.addActivity(workActivity);
			}
		}

		MatsimWriter populationWriter = new PopulationWriter(population);
		populationWriter.write(populationFile);
	}
}