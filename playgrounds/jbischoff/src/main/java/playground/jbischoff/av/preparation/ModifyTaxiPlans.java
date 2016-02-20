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

package playground.jbischoff.av.preparation;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author  jbischoff
 *
 */
public class ModifyTaxiPlans {

	public static void main(String[] args) {
		Random rnd = MatsimRandom.getRandom();
		// TODO Auto-generated method stub
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/bvg.run132.25pct.output_network.xml.gz");
		new MatsimPopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plansWithCarsR0.10.xml.gz");
		
		for (Person person : scenario.getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			Leg l1 = (Leg) plan.getPlanElements().get(1);
			if (l1.getMode().equals("taxi")){
			Activity act1 = (Activity) plan.getPlanElements().get(0);
			Activity act2 = (Activity) plan.getPlanElements().get(2);
			
			Coord c1 = scenario.getNetwork().getLinks().get(act1.getLinkId()).getCoord();
			Coord c2=  scenario.getNetwork().getLinks().get(act2.getLinkId()).getCoord();
			if (CoordUtils.calcEuclideanDistance(c1, c2)<3000){
				if (rnd.nextDouble()<0.1)
				{		
					Person p2 = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(person.getId().toString()+"_1"));
					p2.addPlan(person.createCopyOfSelectedPlanAndMakeSelected());
					scenario2.getPopulation().addPerson(p2);
				} 
			
			}
	
			
		}
			scenario2.getPopulation().addPerson(person);
		}
		System.out.println(scenario2.getPopulation().getPersons().size()+" / "+scenario.getPopulation().getPersons().size());
		new PopulationWriter(scenario2.getPopulation()).write("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/rainyday/plansWithCarsR0.10.xml.gz");
		
	}

}
