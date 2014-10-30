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

package playground.gregor.scenariogen.pantheon;

import java.util.ArrayList;

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

public class PopulationGenerator {


	public static void main (String [] args) {
		String config = "/Users/laemmel/devel/pantheon/input/config.xml";


		Config conf = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(conf);

		ArrayList<Link> sources = computeSources(sc);
		ArrayList<Link> sinks = computeSinks(sc);

		int numPers = 6000;

		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();





		int id = 0;
		for (int i = 0; i < numPers; i++) {



			Person pers = fac.createPerson(Id.create(id++, Person.class));
			pop.addPerson(pers);

			Plan plan = fac.createPlan();
			pers.addPlan(plan);

			

			Link l = sources.get(MatsimRandom.getRandom().nextInt(sources.size()));

			Activity act0 = fac.createActivityFromLinkId("origin", l.getId());
			double time;
			do {
				double offset = MatsimRandom.getRandom().nextGaussian()*500;
				time = 12*3600+offset;
			}while (time < 11*3600 || time > 13*3600);
//			time = Math.round(time);
//			time -= time%60;
//			time = 12*3600;
			act0.setEndTime(time);
			plan.addActivity(act0);

			Leg leg0 = fac.createLeg("car");
			plan.addLeg(leg0);

			Link d = sinks.get(MatsimRandom.getRandom().nextInt(sinks.size()));
			Activity act1 = fac.createActivityFromLinkId("destination", d.getId());
			act1.setEndTime(time+30*60);

			plan.addActivity(act1);

			Leg leg1 = fac.createLeg("car");
			plan.addLeg(leg1);

			Activity act2 = fac.createActivityFromLinkId("origin", l.getId());
			plan.addActivity(act2);
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

	private static ArrayList<Link> computeSinks(Scenario sc) {
		ArrayList<Link> ret = new ArrayList<Link>();
		for (Link l : sc.getNetwork().getLinks().values()) {
			if (l.getToNode().getInLinks().size() == 1) {
				ret.add(l);
			}
		}
		return ret;
	}

}
