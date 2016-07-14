/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationGenerator.java
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

package playground.gregor.scenariogen.gct;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;

public class PopulationGenerator {

	static double terminal = 1.3 * 3.5 * 3600;
	static double street = 1.3 * 4 * 3600;
	static double counter = 1.3 * .5 * 3600;

	public static void main (String [] args) {
		String config = "/Users/laemmel/devel/gct/input/config.xml";
		String s2config = "/Users/laemmel/devel/gct/input/s2d_config_v0.3.xml";


		Config conf = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(conf);
//
//		Sim2DConfig s2conf = Sim2DConfigUtils.loadConfig(s2config);
//		Sim2DScenario s2sc = Sim2DScenarioUtils.loadSim2DScenario(s2conf);
//		s2sc.connect(sc);
		

		ArrayList<Link> sources = computeSources(sc);
		ArrayList<Link> sinks = computeSinks(sc);
		ArrayList<Link> counters = computeCounter(sc);
		
		int numPers = 375000;
//		int numPers = 75000;

		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();



		

		int id = 0;
		for (int i = 0; i < numPers; i++) {




			

			Link l = sources.get(MatsimRandom.getRandom().nextInt(sources.size()));

//			boolean stre = false;
			if (l.getCapacity() >= street) {
				if (MatsimRandom.getRandom().nextDouble() > 0.33){
					i--;
					continue;
				}
//				stre  = MatsimRandom.getRandom().nextDouble() < 0.13;
			} else if (l.getCapacity() >= terminal) {
				if (MatsimRandom.getRandom().nextDouble() > 0.66){
					i--;
					continue;
				}
			} else {
				i--;
				continue;
			}
			double time;
			do {
				double offset = MatsimRandom.getRandom().nextGaussian()*3600;//3600;
				time = 9*3600+offset;
			}while (time < 6*3600 || time > 18*3600);
//			if (time > 8*3600) {
//				continue;
//			}
			
			
			Person pers = fac.createPerson(Id.create(id++, Person.class));
			pop.addPerson(pers);
			
			Plan plan = fac.createPlan();
			pers.addPlan(plan);
			
			
			Activity act0 = fac.createActivityFromLinkId("origin", l.getId());
//			time = Math.round(time);
//			time -= time%60;
//			time = 12*3600;
			act0.setEndTime(time);
			plan.addActivity(act0);

			Leg leg0 = fac.createLeg("car");
			plan.addLeg(leg0);

//			if (stre) {
//				Link c = counters.get(MatsimRandom.getRandom().nextInt(counters.size()));
//				Activity act1b = fac.createActivityFromLinkId("ticket", c.getId());	
//				act1b.setEndTime(0);
////				act1b.setMaximumDuration(120);
//				plan.addActivity(act1b);
//				Leg leg1b = fac.createLeg("car");
//				plan.addLeg(leg1b);
//				
//			}
			
			
			Link d = sinks.get(MatsimRandom.getRandom().nextInt(sinks.size()));
			Activity act1 = fac.createActivityFromLinkId("destination", d.getId());
			
			double duration;
			do {
				double offset = MatsimRandom.getRandom().nextGaussian()*2000;
				duration = time+8*3600+offset;
			}while (duration < 5*3600 || time > 22*3600);
			
			act1.setEndTime(duration);

			plan.addActivity(act1);

			Leg leg1 = fac.createLeg("car");
			plan.addLeg(leg1);

			Activity act2 = fac.createActivityFromLinkId("origin", l.getId());
			plan.addActivity(act2);
			if (i%1000 == 0) {
				System.out.println(i);
			}
		}


		new PopulationWriter(pop, sc.getNetwork()).write(conf.plans().getInputFile());
	}

	private static ArrayList<Link> computeSources(Scenario sc) {
		ArrayList<Link> ret = new ArrayList<Link>();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getFromNode().getOutLinks().size() == 1 && l.getFreespeed() >= 1.34) {
				ret.add(l);
			}
		}
		return ret;
	}
	

	private static ArrayList<Link> computeCounter(Scenario sc) {
		ArrayList<Link> ret = new ArrayList<Link>();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getToNode().getInLinks().size() == 1 && l.getCapacity() <= counter) {
				ret.add(l);
			}
		}
		return ret;
	}
	
	private static ArrayList<Link> computeSinks(Scenario sc) {
		ArrayList<Link> ret = new ArrayList<Link>();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getToNode().getInLinks().size() == 1 && l.getCapacity() > counter) {
				ret.add(l);
			}
		}
		return ret;
	}

}
