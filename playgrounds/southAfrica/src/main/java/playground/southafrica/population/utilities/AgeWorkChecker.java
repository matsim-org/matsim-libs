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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

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
				int age = PersonUtils.getAge(person);
				if(age < 12 && hasWork(person)){
//					log.error("Person " + person.getId() + ": age " + age);
					workingKidCounter.incCounter();
				}
			}
			
			private boolean hasWork(Person person){
				if(person.getPlans().size() > 0){
					for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
						if(pe instanceof Activity){
							Activity activity = (Activity) pe;
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
		final PersonAlgorithm algo = this.algorithm;
//		final Population population2 = (Population)scenario.getPopulation();
		StreamingPopulationReader population2 = new StreamingPopulationReader( scenario ) ;
		population2.addAlgorithm(algo);
		StreamingUtils.setIsStreaming(population2, true);
		
//		MatsimPopulationReader pr = new MatsimPopulationReader(scenario);
		population2.readFile(population);
	}

}

