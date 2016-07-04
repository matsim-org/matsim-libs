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
package playground.jbischoff.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */

public class WriteTestPopulation {

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory fac = scenario.getPopulation().getFactory();
		int n = 100;
		for (int i = 0 ; i < n; i++){
			Person p = fac.createPerson(Id.createPersonId(i));
			Plan plan = fac.createPlan();
			p.addPlan(plan);
			Activity h1 = fac.createActivityFromLinkId("home", Id.createLinkId(114));
			h1.setEndTime(7*3600+i*60);
			plan.addActivity(h1);
			Leg l1 = fac.createLeg("car");
			plan.addLeg(l1);
			Activity w1 = fac.createActivityFromLinkId("work", Id.createLinkId(349));
			w1.setEndTime(8*3600+i*60);
			plan.addActivity(w1);
			plan.addLeg(fac.createLeg("car"));
			Activity h2 = fac.createActivityFromLinkId("home", Id.createLinkId(114));
			plan.addActivity(h2);
			scenario.getPopulation().addPerson(p);
		}
		new PopulationWriter(scenario.getPopulation()).write("../../../shared-svn/projects/bmw_carsharing/example/population"+n+".xml");
				
	}

}
