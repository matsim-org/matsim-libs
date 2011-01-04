/* *********************************************************************** *
 * project: org.matsim.*
 * PlanTimeWatcher.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
/**
 * finds activities taking place after 30:00 hr.
 */
public class PlanTimeWatcher {
	final double hr30= 108000.0;

	void run(Population population){
		for (Person person: population.getPersons().values()){
			for (Plan plan : person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (  act.getStartTime()> hr30 || act.getEndTime()> hr30 ){
							System.out.println( "startTime: " +  act.getStartTime() +  " endTime: " + act.getEndTime());
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String populationFile = "../playgrounds/mmoyo/output/test/doubleMinTransfersRoutes_plan2.xml.gz";
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
	
		ScenarioImpl scenario = new DataLoader().readNetwork_Population(networkFile, populationFile );
		
		new PlanTimeWatcher().run(scenario.getPopulation());

	}

}
