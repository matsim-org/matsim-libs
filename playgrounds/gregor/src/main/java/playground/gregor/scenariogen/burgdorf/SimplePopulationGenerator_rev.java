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

public class SimplePopulationGenerator_rev {
	public static void main(String [] args) {
		Config c = ConfigUtils.loadConfig("/Users/laemmel/devel/burgdorf2d3/input/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		int nrAgents = 48000;
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		double t = 15*3600;;
		for (int i = 0; i < nrAgents; i++) {
			Person pers = fac.createPerson(Id.create("b"+i, Person.class));
			Plan plan = fac.createPlan();
			
			
			
			pers.addPlan(plan);
			Activity act0;
			int mod = MatsimRandom.getRandom().nextInt(4);
			String rev = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
			act0 = fac.createActivityFromLinkId("origin", Id.create("sim2d_"+ mod + "_" + rev + "-11046", Link.class));
			act0.setEndTime(t);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			
			
			Activity act1;// = fac.createActivityFromLinkId("destination", Id.create("sim2d_0_rev_-11026"));
			mod = MatsimRandom.getRandom().nextInt(11);
			rev = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
			act1 = fac.createActivityFromLinkId("destination", Id.create("sim2d_"+ mod + "_" + rev + "-11060", Link.class));
			plan.addActivity(act1);
			
			
			
			Plan plan2 = fac.createPlan();
			pers.addPlan(plan2);
			Activity act02;
			int mod2 = MatsimRandom.getRandom().nextInt(4);
			String rev2 = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
			act02 = fac.createActivityFromLinkId("origin", Id.create("sim2d_"+ mod2 + "_" + rev2 + "-11046", Link.class));
			act02.setEndTime(t);
			plan2.addActivity(act02);
			Leg leg2 = fac.createLeg("car");
			plan2.addLeg(leg2);
			
			
			Activity act12;// = fac.createActivityFromLinkId("destination", Id.create("sim2d_0_rev_-11026"));
			mod2 = MatsimRandom.getRandom().nextInt(11);
			rev2 = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
			act12 = fac.createActivityFromLinkId("destination", Id.create("sim2d_"+ mod2 + "_" + rev2 + "-11058", Link.class));
			plan2.addActivity(act12);
			
			
			if (MatsimRandom.getRandom().nextBoolean()) {
				pers.setSelectedPlan(plan);
			} else {
				pers.setSelectedPlan(plan2);
			}
			
			pop.addPerson(pers);
//			t += .5;
//			if ((i+1)%(960) == 0) {
//				t += 5*60; //3*60+12;
//			}
		}
////		t = 5*3600+32*60+30;
//		for (int i = nrAgents/2; i < nrAgents; i++) {
//				Person pers = fac.createPerson(Id.create("b"+i));
//				Plan plan = fac.createPlan();
//				pers.addPlan(plan);
//				Activity act0;
//				int mod = MatsimRandom.getRandom().nextInt(4);
//				String rev = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
//				act0 = fac.createActivityFromLinkId("origin", Id.create("sim2d_"+ mod + "_" + rev + "-19535"));
//				act0.setEndTime(t);
//				plan.addActivity(act0);
//				Leg leg = fac.createLeg("car");
//				plan.addLeg(leg);
//				
//				
//				Activity act1;// = fac.createActivityFromLinkId("destination", Id.create("sim2d_0_rev_-11026"));
//				mod = MatsimRandom.getRandom().nextInt(11);
//				rev = MatsimRandom.getRandom().nextBoolean() ? "rev_" : "";
//				act1 = fac.createActivityFromLinkId("destination", Id.create("sim2d_"+ mod + "_" + rev + "-11260"));
//				plan.addActivity(act1);
//				pop.addPerson(pers);
////				t += .5;
////				if ((i+1)%(960) == 0) {
////					t += 5*60; //3*60+12;
////				}
//			}
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}
}
