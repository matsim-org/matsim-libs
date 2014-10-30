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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;


public class SimplePopulationGenerator {
	private static final Set<Integer> excl = new HashSet<Integer>();
	static {
		excl.add(180);
		excl.add(175);
		excl.add(196);
		excl.add(99);
		excl.add(25);
		excl.add(158);
		excl.add(6);
		excl.add(192);
	}
	public static void main(String [] args) {
		Config c = ConfigUtils.loadConfig("/Users/laemmel/devel/burgdorf2d/input/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		Sim2DConfig s2dc = Sim2DConfigUtils.loadConfig("/Users/laemmel/devel/burgdorf2d/input/s2d_config.xml");
		Sim2DScenario s2dsc = Sim2DScenarioUtils.loadSim2DScenario(s2dc);
		
		Id<Link> target = Id.create("sim2d_0_174673140", Link.class);
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		int a = 0;
		for (Sim2DEnvironment env : s2dsc.getSim2DEnvironments()) {
			Network net = env.getEnvironmentNetwork();
			for ( Link l : net.getLinks().values()) {
				
				if (l.getToNode().getOutLinks().size() == 1) {
//					if (MatsimRandom.getRandom().nextBoolean()) {
//						continue;
//					}
					double time = 0;
					boolean exclude = true;
					if (excl.contains(a)){
						time = 14.5;
						exclude = false;
					}
					if (a == 57) {
						exclude = false;
					}
					if (a == 192) {
						time = 11.5;
					}
					Person pers = fac.createPerson(Id.create("b"+a++, Person.class));
					Plan plan = fac.createPlan();
					pers.addPlan(plan);
					Activity act0;
					act0 = fac.createActivityFromLinkId("origin", l.getId());
					
					act0.setEndTime(time);
					plan.addActivity(act0);
					Leg leg = fac.createLeg("car");
					plan.addLeg(leg);
					Activity act1 = fac.createActivityFromLinkId("destination", target);
					plan.addActivity(act1);
					if (!exclude){pop.addPerson(pers);};
				}
			}
		}
		new PopulationWriter(pop, sc.getNetwork()).write(c.plans().getInputFile());

	}
}
