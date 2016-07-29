/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.pt.tutorial;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;

public class PopulationGenerator {

	private static final double loc1X = 2000;
	private static final double loc1Y = 3000;

	private static final double loc2X = 4000;
	private static final double loc2Y = 1000;

	private static void makePopulation(final int nOfPersonFromEachHome, final String networkFilename, final String filename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop = sc.getPopulation();
		PopulationFactory pf = pop.getFactory();

		Random r = new Random(4711);
		int pId = 0;

		double baseX = 1000;
		double baseY = 1000;
		for (int i = 0; i < nOfPersonFromEachHome; i++) {
			pId++;
			String mode = (r.nextDouble() < 0.50 ? TransportMode.car : TransportMode.pt);

			Person person = pf.createPerson(Id.create(pId, Person.class));
			Plan plan = pf.createPlan();
			Coord homeCoord = new Coord((double) (int) (baseX - 450 + 900 * r.nextDouble()), (double) (int) (baseY - 450 + 900 * r.nextDouble()));
			fillPlan(plan, mode, r, pf, homeCoord);
			person.addPlan(plan);
			pop.addPerson(person);
		}

		baseX = 1000;
		baseY = 4000;
		for (int i = 0; i < nOfPersonFromEachHome; i++) {
			pId++;
			String mode = (r.nextDouble() < 0.50 ? TransportMode.car : TransportMode.pt);

			Person person = pf.createPerson(Id.create(pId, Person.class));
			Plan plan = pf.createPlan();
			Coord homeCoord = new Coord((double) (int) (baseX - 450 + 900 * r.nextDouble()), (double) (int) (baseY - 450 + 900 * r.nextDouble()));
			fillPlan(plan, mode, r, pf, homeCoord);
			person.addPlan(plan);
			pop.addPerson(person);
		}

		baseX = 4000;
		baseY = 4000;
		for (int i = 0; i < nOfPersonFromEachHome; i++) {
			pId++;
			String mode = (r.nextDouble() < 0.50 ? TransportMode.car : TransportMode.pt);

			Person person = pf.createPerson(Id.create(pId, Person.class));
			Plan plan = pf.createPlan();
			Coord homeCoord = new Coord((double) (int) (baseX - 450 + 900.0 * r.nextDouble()), (double) (int) (baseY - 450 + 900.0 * r.nextDouble()));
			fillPlan(plan, mode, r, pf, homeCoord);
			person.addPlan(plan);
			pop.addPerson(person);
		}

		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Network carNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(sc.getNetwork()).filter(carNetwork, CollectionUtils.stringToSet(TransportMode.car));
		new XY2Links(carNetwork, null).run(pop);

		new PopulationWriter(pop, null).write(filename);
	}

	private static void fillPlan(final Plan plan, final String mode, final Random r, final PopulationFactory pf, Coord homeCoord) {
		Activity h1 = pf.createActivityFromCoord("h", homeCoord);
		h1.setEndTime(7.0*3600 + r.nextDouble()*3600.0);
		plan.addActivity(h1);

		Leg leg1 = pf.createLeg(mode);
		plan.addLeg(leg1);

		Coord workCoord;
		if (r.nextDouble() < 0.5) {
			workCoord = new Coord((double) (int) (loc1X - 450 + 900.0 * r.nextDouble()), (double) (int) (loc1Y - 450 + 900.0 * r.nextDouble()));
		} else {
			workCoord = new Coord((double) (int) (loc2X - 450 + 900.0 * r.nextDouble()), (double) (int) (loc2Y - 450 + 900.0 * r.nextDouble()));
		}
		Activity w = pf.createActivityFromCoord("w", workCoord);
		w.setEndTime(17.0*3600 + r.nextDouble()*3600.0);
		plan.addActivity(w);

		Leg leg2 = pf.createLeg(mode);
		plan.addLeg(leg2);

		if (r.nextDouble() < 0.5) {
			// add shop activity
			Coord shopCoord;
			if (r.nextDouble() < 0.5) {
				shopCoord = new Coord((double) (int) (loc1X - 450 + 900.0 * r.nextDouble()), (double) (int) (loc1Y - 450 + 900.0 * r.nextDouble()));
			} else {
				shopCoord = new Coord((double) (int) (loc2X - 450 + 900.0 * r.nextDouble()), (double) (int) (loc2Y - 450 + 900.0 * r.nextDouble()));
			}
			Activity s = pf.createActivityFromCoord("s", shopCoord);
			s.setEndTime(w.getEndTime() + r.nextDouble()*3600.0);
			plan.addActivity(s);

			Leg leg3 = pf.createLeg(mode);
			plan.addLeg(leg3);
		}

		Activity h2 = pf.createActivityFromCoord("h", homeCoord);
		plan.addActivity(h2);
	}

	public static void main(String[] args) {
		makePopulation(300, "pt-tutorial/multimodalnetwork.xml", "pt-tutorial/population.xml");
	}
}
