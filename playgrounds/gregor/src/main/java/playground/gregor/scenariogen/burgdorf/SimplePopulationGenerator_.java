/* *********************************************************************** *
 * project: org.matsim.*
 * SimplePopulationGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.scenariogen.burgdorf;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

public class SimplePopulationGenerator_ {
	public static void main(String [] args) {
		Config c = ConfigUtils.loadConfig("/Users/laemmel/devel/burgdorf2d2/input/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		int nrAgents = 48000;
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 5*3600+30*60;
		for (int i = 0; i < nrAgents/2; i++) {
			Person pers = fac.createPerson(Id.create("b"+i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			int mod = i % 11;
			String rev = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
			act0 = fac.createActivityFromLinkId("origin", Id.create("sim2d_"+ mod + "_" + rev + "-11014", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("sim2d_0_rev_-11026", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
//			t += .5;
			if ((i+1)%(960) == 0) {
//				t += 3*60+12;
				t += 5*60;
			}
		}
		t = 5*3600+32*60+30;
		for (int i = nrAgents/2; i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("b"+i, Person.class));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			
			int mod = i % 11;
			String rev = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
//			if (mod <= 5) {
				act0 = fac.createActivityFromLinkId("origin", Id.create("sim2d_"+ mod + "_" + rev + "-11012", Link.class));
//			} else {
//				mod -= 6;
//				act0 = fac.createActivityFromLinkId("origin", Id.create("sim2d_"+ mod + "_" + rev + "-51481"));
//			}
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", Id.create("sim2d_0_rev_-11026", Link.class));
			plan.addActivity(act1);
			pop.addPerson(pers);
//			t += .5;
			if ((i+1)%(960) == 0) {
//				t += 3*60+12;
				t += 5*60;
			}
		}
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}
}
