/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareBackgroundPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;

/*
 * Prepares Burgdorf background population by:
 * - removing non-selected plans
 * - scaling the population (if necessary)
 * - runs a test iteration to check the resulting counts
 */
public class PrepareBackgroundPopulation {

	private int scaleFactor = 10;
	private double maxDepartureTimeShift = 300.0;
	
//	private static String day = "freitag";
//	private static String day = "samstag";
	private static String day = "sonntag";
	private static String configFile = "../../matsim/mysimulations/burgdorf/config_cadyts.xml";
	private static String countsFile = "../../matsim/mysimulations/burgdorf/input/counts_burgdorf_" + day +".xml";
	private static String populationFile = "../../matsim/mysimulations/burgdorf/output_cadyts_" + day + "_10pct/output_plans.xml.gz";
	private static String outputDirectory = "../../matsim/mysimulations/burgdorf/output_cadyts_" + day + "_prepared";
	
	public static void main(String[] args) {
		
		// load and adapt config
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setRunId(day);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(outputDirectory);
		config.plans().setInputFile(populationFile);
		config.counts().setAverageCountsOverIterations(1);
		config.counts().setWriteCountsInterval(1);
		config.counts().setCountsScaleFactor(1.0);
		config.counts().setCountsFileName(countsFile);
		config.qsim().setNumberOfThreads(8);
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(1.0);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new PrepareBackgroundPopulation(scenario);
	}
	
	public PrepareBackgroundPopulation(Scenario scenario) {
		
		// remove unselected plans from population
		for (Person person : scenario.getPopulation().getPersons().values()) {
			PersonImpl.removeUnselectedPlans(((PersonImpl) person));
		}
		
		// remove persons which have a "one activity only" plan selected
		Iterator<Person> iter = (Iterator<Person>) scenario.getPopulation().getPersons().values().iterator();
		while (iter.hasNext()) {
			Person person = iter.next();
			Plan plan = person.getSelectedPlan();
			Activity startActivity = (Activity) plan.getPlanElements().get(0);
			Activity endActivity = (Activity) plan.getPlanElements().get(2);
			if (startActivity.getLinkId().equals(endActivity.getLinkId())) iter.remove();
		}
		
		// create additional agents
		if (scaleFactor > 1) {
			PopulationFactory factory = scenario.getPopulation().getFactory();
			List<Person> newPersons = new ArrayList<Person>();
			
			for (Person person : scenario.getPopulation().getPersons().values())
			for (int i = 1; i < scaleFactor; i++) {
				Id<Person> id = Id.create(person.getId().toString() + "_clone_" + i, Person.class);
				Person newPerson = factory.createPerson(id);
			
				Plan plan = factory.createPlan();
				((PlanImpl) plan).copyFrom(person.getSelectedPlan());
				
				// shift departure time
				double shiftValue = (MatsimRandom.getRandom().nextDouble() * maxDepartureTimeShift);
				shiftValue = shiftValue - maxDepartureTimeShift*0.5;
				shiftValue = Math.round(shiftValue);
				
				Activity activity = (Activity) plan.getPlanElements().get(0);
				Leg leg = (Leg) plan.getPlanElements().get(1);
				
				activity.setEndTime(activity.getEndTime() + shiftValue);
				leg.setDepartureTime(leg.getDepartureTime() + shiftValue);
				
				plan.setPerson(newPerson);
				newPerson.addPlan(plan);
								
				newPersons.add(newPerson);
			}
			
			for (Person newPerson : newPersons) {
				scenario.getPopulation().addPerson(newPerson);
			}
		}
		
		// create and run controller
		new Controler(scenario).run();
	}
	
}
