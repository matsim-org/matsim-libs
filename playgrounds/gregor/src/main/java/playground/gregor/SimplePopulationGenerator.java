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

package playground.gregor;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

public class SimplePopulationGenerator {
	public static void main(String [] args) {
		Config c = ConfigUtils.loadConfig("/Users/laemmel/devel/burgdorf2d2/input/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		int nrAgents = 20000;
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 120;
		for (int i = 0; i < nrAgents/2; i++) {
			Person pers = fac.createPerson(new IdImpl("b"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			if (MatsimRandom.getRandom().nextBoolean()) {
				act0 = fac.createActivityFromLinkId("origin", new IdImpl("sim2d_3_rev_-11078"));
			} else  {
				act0 = fac.createActivityFromLinkId("origin", new IdImpl("sim2d_3_-11078"));
			}
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("sim2d_0_rev_-11064"));
			plan.addActivity(act1);
			pop.addPerson(pers);
//			t += .5;
		}
		t =0;
		for (int i = nrAgents/2; i < nrAgents; i++) {
			Person pers = fac.createPerson(new IdImpl("b"+i));
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			Activity act0;
			double r = MatsimRandom.getRandom().nextDouble();
			if (r < 0.33) {
				act0 = fac.createActivityFromLinkId("origin", new IdImpl("sim2d_2_-11076"));
			} else if (r < 0.66) {
				act0 = fac.createActivityFromLinkId("origin", new IdImpl("sim2d_2_-11076"));
			} else {
				act0 = fac.createActivityFromLinkId("origin", new IdImpl("sim2d_0_-11076"));
			}
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Activity act1 = fac.createActivityFromLinkId("destination", new IdImpl("sim2d_0_rev_-11064"));
			plan.addActivity(act1);
			pop.addPerson(pers);
//			t += .5;
		}
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}
}
