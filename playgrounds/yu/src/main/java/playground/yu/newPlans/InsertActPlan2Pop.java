/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.yu.newPlans;

import java.util.Random;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * inserts a new Act and Leg before the last Act of a population file
 *
 * @author yu
 *
 */
public class InsertActPlan2Pop extends NewPopulation implements PlanAlgorithm {
	private final Random r = MatsimRandom.getLocalInstance();

	/**
	 * @param population
	 * @param filename
	 */
	public InsertActPlan2Pop(Network network, Population population, String filename) {
		super(network, population, filename);
	}

	@Override
	public void run(Person person) {
		for (Plan pi : person.getPlans())
			run(pi);
		pw.writePerson(person);
	}

	@Override
	public void run(Plan plan) {
		((PlanImpl) plan).insertLegAct(plan.getPlanElements().size() - 2,
				new LegImpl(TransportMode.car), new ActivityImpl("work",
						new CoordImpl(4597000
								+ ((r.nextDouble() - 0.5 < 0) ? -1 : 1)
								* r.nextDouble() * 500/* x */, 5815000
								+ ((r.nextDouble() - 0.5 < 0) ? -1 : 1)
								* r.nextDouble() * 500)/* y */));
	}

	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/network.multimodal.mini.xml.gz";
		final String plansFilename = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/plan.routedOevModell.BVB344.xml";
		final String outputFilename = "../berlin-bvg09/pt/baseplan_900s_smallnetwork/test/plan.routedOevModell.BVB344.moreLegPlan.xml";

		ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network network = s.getNetwork();
		new MatsimNetworkReader(s).readFile(netFilename);

		Population population = s.getPopulation();
		new MatsimPopulationReader(s).readFile(plansFilename);

		InsertActPlan2Pop npwp = new InsertActPlan2Pop(network, population, outputFilename);
		npwp.run(population);
		npwp.writeEndPlans();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
