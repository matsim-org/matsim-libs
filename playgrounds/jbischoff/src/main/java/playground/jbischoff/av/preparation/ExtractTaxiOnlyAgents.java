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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class ExtractTaxiOnlyAgents {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scrappedpopulation24.xml.gz");
	double scale = 0.1;
	Population pop2 = convertPopulation(scenario.getPopulation(),scale);
	new PopulationWriter(pop2).write("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/singleLegPopulationTaxiOnly"+scale+".xml.gz");
}

private static Population convertPopulation(Population population, double scale) {
Population newPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
Random r = MatsimRandom.getRandom();
	for (Person p : population.getPersons().values()){
		for (Plan plan : p.getPlans()){
			Activity previous = null;
			Leg previousLeg = null;
			boolean write = false;
			int i = 0;
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity){
					
					if (previous == null){
						previous = (Activity) pe;
					} else{
						if (write){
							
						Person p1 = newPopulation.getFactory().createPerson(Id.createPersonId((p.getId().toString()+"_"+i)));
						i++;
						Plan plan1 = newPopulation.getFactory().createPlan();
						plan1.addActivity(previous);
						plan1.addLeg(previousLeg);
						
						plan1.addActivity((Activity)pe);
						p1.addPlan(plan1);
						if (r.nextDouble()<scale){
						newPopulation.addPerson(p1);
						}
						write = false;
					} }
					
				}
				else if (pe instanceof Leg){
					previousLeg = (Leg) pe;
					if (previousLeg.getMode().equals("taxi")) write = true;
					}
				}
				
			}
		}
	
	return newPopulation;
}
}
