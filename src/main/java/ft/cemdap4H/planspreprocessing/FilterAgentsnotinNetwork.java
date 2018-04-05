/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package ft.cemdap4H.planspreprocessing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FilterAgentsnotinNetwork {

	public static void main(String[] args) {
	
		final String networkFile = "D:/cemdap-vw/input/networkcar-jun17.xml.gz";
		final String inputPlans = "D:/cemdap-vw/Output/mergedplans_dur.xml.gz";
		final String outputPlans = "D:/cemdap-vw/Output/mergedplans_filtered.xml.gz";
		new FilterAgentsnotinNetwork().run(networkFile, inputPlans, outputPlans,2000);
	}
	public void run(String networkFile, String inputPlans, String outputPlans, double threshold){
				Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new PopulationReader(scenario).readFile(inputPlans);
		Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for (Person p : scenario.getPopulation().getPersons().values()){
			Person p2 = pop2.getFactory().createPerson(p.getId());
			for (Plan plan : p.getPlans()){
				boolean copyPlan = true;
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						Link l = NetworkUtils.getNearestLink(scenario.getNetwork(), ((Activity) pe).getCoord());
						if (DistanceUtils.calculateSquaredDistance(l.getCoord(), ((Activity) pe).getCoord())>(threshold*threshold)){
							copyPlan = false;
							break;
						}
					}
				}
			if (copyPlan){
				p2.addPlan(plan);
			}
			}
			if (p2.getPlans().size()>0){
				pop2.addPerson(p2);
			}
		}
		new PopulationWriter(pop2).write(outputPlans);
	}
	
}
