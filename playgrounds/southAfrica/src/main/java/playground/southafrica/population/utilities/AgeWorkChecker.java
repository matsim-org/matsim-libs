/* *********************************************************************** *
 * project: org.matsim.*
 * AgeWorkChecker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.population.utilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.PersonAlgorithm;

public class AgeWorkChecker {
	private PersonAlgorithm algorithm;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String population = args[0];
		
		AgeWorkChecker awc = new AgeWorkChecker();
		awc.checkForWorkingKids(population);

	}
	
	public AgeWorkChecker() {
		this.algorithm = new PersonAlgorithm() {
			private Logger log = Logger.getLogger(PersonAlgorithm.class);
			private Counter workingKidCounter = new Counter(" kids # ");
			
			@Override
			public void run(Person person) {
				int age = PersonImpl.getAge(person);
				if(age < 12 && hasWork(person)){
//					log.error("Person " + person.getId() + ": age " + age);
					workingKidCounter.incCounter();
				}
			}
			
			private boolean hasWork(Person person){
				if(person.getPlans().size() > 0){
					for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
						if(pe instanceof ActivityImpl){
							ActivityImpl activity = (ActivityImpl) pe;
							if(activity.getType().contains("w")){
								return true;
							}
						}
					}
				}
				return false;
			}
			
			public void printCounter(){
				this.workingKidCounter.printCounter();
			}
			
		};
	}
	
	public void checkForWorkingKids(String population){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(this.algorithm);
		((PopulationImpl)scenario.getPopulation()).setIsStreaming(true);
		
		MatsimPopulationReader pr = new MatsimPopulationReader(scenario);
		pr.readFile(population);
	}

}

