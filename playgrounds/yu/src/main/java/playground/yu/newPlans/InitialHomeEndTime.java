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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author yu
 *
 */
public class InitialHomeEndTime extends NewPopulation implements PlanAlgorithm {

	/**
	 * @param population
	 * @param filename
	 */
	public InitialHomeEndTime(final Network network, final Population population, final String filename) {
		super(network, population, filename);
	}

	@Override
	public void run(final Person person) {
		for (Plan pl : person.getPlans())
			run(pl);
		this.pw.writePerson(person);
	}

	@Override
	public void run(final Plan plan) {
		((PlanImpl) plan).getFirstActivity().setEndTime(21600.0);
	}

	public static void main(final String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../schweiz-ivtch-SVN/baseCase/plans/plans_all_zrh30km_transitincl_10pct.xml.gz";
		final String outputPlansFilename = "output/plans_all_zrh30km_transitincl_10pct_home_end_6h.xml.gz";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(plansFilename);

		InitialHomeEndTime ihet = new InitialHomeEndTime(network, population, outputPlansFilename);
		ihet.run(population);
		ihet.writeEndPlans();
	}

}
