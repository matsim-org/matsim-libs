/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.demand.tasks;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.util.XORShiftRandom;
import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.gsv.demand.PopulationTask;

import java.io.IOException;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PlanDepartureTime implements PopulationTask {

	private ChoiceSet<Integer> choiceSet;
	
	private Random random = new XORShiftRandom();
	
	public PlanDepartureTime(ChoiceSet<Integer> choiceSet, Random random) throws IOException {
		this.choiceSet = choiceSet;
		this.random = random;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.demand.PopulationTask#apply(org.matsim.api.core.v01.population.Population)
	 */
	@Override
	public void apply(Population pop) {
		for(Person p : pop.getPersons().values()) {
			int hour = choiceSet.randomWeightedChoice();
			double min = random.nextDouble();
			double time = 60*60*hour + 60*60*min;
			Activity home = (Activity) p.getPlans().get(0).getPlanElements().get(0);
			home.setEndTime(time);
		}
	}
	
	public static void main(String args[]) throws IOException {
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		PopulationReader reader = new MatsimPopulationReader(scenario);
//		reader.readFile("/home/johannes/gsv/netz2030/data/population.xy.xml");
//		
//		PlanDepartureTime distributor = new PlanDepartureTime("/home/johannes/gsv/netz2030/data/raw/Ganglinien_Standardtag.csv");
//		distributor.apply(scenario.getPopulation());
//		
//		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), null);
//		writer.write("/home/johannes/gsv/netz2030/data/population.xy.dt.xml");
	}

}
