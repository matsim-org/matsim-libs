/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.csberlin.plans.preprocess;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RemoveAgentsWhereFirstAndLastLocationDiffers {
public static void main(String[] args) {
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile("../../../shared-svn/projects/bmw_carsharing/data/scenario/untersuchungsraum-plans.xml.gz");
	for (Person p : scenario.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		Activity lastActivity = null;
		Leg lastLeg = null;
		Activity firstActivityBeforeCarUse = null;
		Activity firstActivityAfterLastCarUse = null;
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Activity){
				lastActivity = (Activity) pe;
				if (lastLeg!=null){
					if (lastLeg.getMode().equals(TransportMode.car)){
						firstActivityAfterLastCarUse = (Activity) pe;
					}
				}
			} else if (pe instanceof Leg){
				Leg leg = (Leg) pe;
				if (leg.getMode().equals(TransportMode.car)){
					if (firstActivityBeforeCarUse==null){
						firstActivityBeforeCarUse = lastActivity;
					}
					lastLeg = leg;
				}
			}
		}
		if ((firstActivityAfterLastCarUse==null)&&(firstActivityBeforeCarUse==null)){
			//nor car used at all
			pop2.addPerson(p);
			
		} else if (CoordUtils.calcEuclideanDistance(firstActivityAfterLastCarUse.getCoord(), firstActivityBeforeCarUse.getCoord())<2000){
			pop2.addPerson(p);
		}
		
	}
	new PopulationWriter(pop2).write("../../../shared-svn/projects/bmw_carsharing/data/scenario/untersuchungsraum-plans_noWeirdCarBehavior.xml.gz");
	
	
}
}
