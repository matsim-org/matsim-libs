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
package playground.johannes.gsv.demand;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class ModeSelector implements PopulationTask {

	private Random random = new XORShiftRandom();
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.demand.PopulationTask#apply(org.matsim.api.core.v01.population.Population)
	 */
	@Override
	public void apply(Population pop) {
		for(Person p : pop.getPersons().values()) {
			Plan plan = p.getPlans().get(0);
			Leg leg = (Leg) plan.getPlanElements().get(1);
			if(random.nextDouble() < 0.5) {
				leg.setMode("car");
			} else {
				leg.setMode("pt");
			}
		}

	}
	
	public final static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/population.linked.gk3.xml");
		
		ModeSelector selector = new ModeSelector();
		selector.apply(scenario.getPopulation());
		
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), null);
		writer.write("/home/johannes/gsv/matsim/studies/netz2030/data/population.linked.gk3.xml");
	}

}
