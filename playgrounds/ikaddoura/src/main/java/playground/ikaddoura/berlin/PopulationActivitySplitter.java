/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.berlin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class PopulationActivitySplitter {
	
	private static final Logger log = Logger.getLogger(PopulationActivitySplitter.class);

	// input
//	final private String inputPopulatlionFile = "../../../public-svn/matsim/scenarios/countries/de/berlin/car-traffic-only-1pct-2014-08-01/run_160.150.plans_selected.xml.gz";
	final private String inputPopulatlionFile = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/run0/output_plans.xml.gz";
	
	// output
	final private String outputDirectory = "../../../runs-svn/berlin_car-traffic-only-1pct-2014-08-01/run1/input/";
	final private String outputPopulationFile = "run0_output_plans_selected_splitActivityTypes.xml.gz";
	
	// settings
	final private double timeCategorySize = 3600.;
	final private boolean onlySelectedPlans = true;
		
	public static void main(String[] args) {
		PopulationActivitySplitter b = new PopulationActivitySplitter();
		b.run();	
	}

	private void run() {
				
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		log.info("Input Population: " + inputPopulatlionFile);
		log.info("Write out a population which only contains selected plans: " + onlySelectedPlans);

		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPopulatlionFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
						
			for (Plan plan : person.getPlans()) {
				
				double startTime = 0.;
				double endTime = Double.MIN_VALUE;
				
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						endTime = act.getEndTime();
						
						int durationCategory = (int) ((endTime - startTime) / timeCategorySize) + 1;		
						startTime = act.getEndTime();

						String newType = act.getType() + "_" + durationCategory;
						act.setType(newType);						
					}
				}
			}
		}
		
		Population outputPopulation = null;
		if (onlySelectedPlans) {
			outputPopulation = getPopulationWithSelectedPlans(scenario.getPopulation());
		} else {
			outputPopulation = scenario.getPopulation();
		}
		
		analyze(outputPopulation);
				
		PopulationWriter pw = new PopulationWriter(outputPopulation);
		pw.write(outputDirectory + outputPopulationFile);		
	}

	private void analyze(Population outputPopulation) {
		
		final Map<String, Integer> activityType2Counter = new HashMap<>();
		
		int personCounter = 0;
		for (Person person : outputPopulation.getPersons().values()) {
			personCounter++;
			for (Plan plan : person.getPlans()) {				
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						
						if (activityType2Counter.containsKey(act.getType())) {
							activityType2Counter.put(act.getType(), activityType2Counter.get(act.getType()) + 1);
						} else {
							activityType2Counter.put(act.getType(), 1);
						}
						
					}
				}
			}
		}
		
		log.info("Number of persons: " + personCounter);
		
		log.info("----");
		log.info("Activity Type; Counter");
		for (String actType : activityType2Counter.keySet()) {
			log.info(actType + " ; " + activityType2Counter.get(actType));
		}
		log.info("----");
	}

	private Population getPopulationWithSelectedPlans(Population population) {
		log.info("Keep only selected plans.");
		
		Population outputPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		for (Person person : population.getPersons().values()) {
			Person personCopy = outputPopulation.getFactory().createPerson(person.getId());
			personCopy.addPlan(person.getSelectedPlan());
			outputPopulation.addPerson(personCopy);
		}
		
		return outputPopulation;
	}

}

