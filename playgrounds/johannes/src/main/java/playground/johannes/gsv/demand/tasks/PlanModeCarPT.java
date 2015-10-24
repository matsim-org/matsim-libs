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

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import playground.johannes.gsv.demand.PopulationTask;

import java.util.Random;

/**
 * @author johannes
 *
 */
public class PlanModeCarPT implements PopulationTask {

	private final Random random;
	
	private final double pcar;
	
	public PlanModeCarPT(double pcar, Random random) {
		this.pcar = pcar;
		this.random = random;
	}
	
	@Override
	public void apply(Population pop) {
		for(Person p : pop.getPersons().values()) {
			Plan plan = p.getPlans().get(0);
			Leg leg = (Leg) plan.getPlanElements().get(1);
			if(random.nextDouble() < pcar) {
				leg.setMode("car");
			} else {
				leg.setMode("pt");
			}
		}

	}
	
	public final static void main(String[] args) {
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		PopulationReader reader = new MatsimPopulationReader(scenario);
//		reader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/population.linked.gk3.xml");
//		
//		PlanModeCarPT selector = new PlanModeCarPT();
//		selector.apply(scenario.getPopulation());
//		
//		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), null);
//		writer.write("/home/johannes/gsv/matsim/studies/netz2030/data/population.linked.gk3.xml");
	}

}
