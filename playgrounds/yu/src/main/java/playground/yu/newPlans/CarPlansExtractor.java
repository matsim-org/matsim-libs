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

package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 *
 */
public class CarPlansExtractor extends NewPopulation implements PlanAlgorithm {
	private Person person = null;
	private final List<Plan> tmpPersonPlans = new ArrayList<Plan>();

	public CarPlansExtractor(Network network, Population plans) {
		super(network, plans);
	}

	public CarPlansExtractor(Network network, Population population, String filename) {
		super(network, population, filename);
	}

	@Override
	public void run(Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			this.person = person;
			tmpPersonPlans.addAll(person.getPlans());
			for (Plan pl : tmpPersonPlans)
				run(pl);
			tmpPersonPlans.clear();
		}
		pw.writePerson(person);
	}

	public void run(Plan plan) {
		if (PlanModeJudger.usePt(plan)) {
			person.getPlans().remove(plan);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = args[0];
		final String plansFilename = args[1];
		final String outputFilename = args[2];

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);

		CarPlansExtractor cpe = new CarPlansExtractor(network, population,
				outputFilename);
		population.addAlgorithm(cpe);
		new MatsimPopulationReader(scenario).readFile(plansFilename);
		population.runAlgorithms();

		// cpe.run(population);
		cpe.writeEndPlans();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
