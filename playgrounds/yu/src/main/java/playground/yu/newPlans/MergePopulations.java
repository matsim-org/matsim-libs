/* *********************************************************************** *
 * project: org.matsim.*
 * MergePopulations.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * merge populations with different agents
 * 
 * @author ychen
 * 
 */
public class MergePopulations {
	public static class CopyPlans extends AbstractPersonAlgorithm {
		private final PopulationWriter writer;

		public CopyPlans(final PopulationWriter writer) {
			this.writer = writer;
		}

		@Override
		public void run(final Person person) {
			writer.writePerson(person);
		}
	}

	private static final class PersonIdCopyPlans extends CopyPlans {
		private final int lower_limit;

		public PersonIdCopyPlans(final PopulationWriter writer,
				final int lower_limit) {
			super(writer);
			this.lower_limit = lower_limit;
		}

		@Override
		public void run(final Person person) {
			if (Integer.parseInt(person.getId().toString()) >= lower_limit) {
				super.run(person);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String path = "../data/ivtch/input/";
		final String netFilename = path + "ivtch-osm.xml";
		final String plansFilenameA = path + "plans_all_zrh30km_100pct.xml.gz";
		final String plansFilenameB = path
				+ "plans_miv_zrh30km_transitincl_100pct_not_direct_2_use.xml.gz";
		final String outputPlansFilename = path
				+ "plans_all_zrh30km_transitincl_100pct.xml.gz";

		// final String path = "test/yu/equil_test/";
		// final String netFilename = path + "equil_net.xml";
		// final String plansFilenameA = path + "plans100pt.xml";
		// final String plansFilenameB = path + "plans300.xml";
		// final String outputPlansFilename = path +
		// "sum_plans_100pt_201-300.xml";

		final int lower_limit = 1000000000;

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		ScenarioImpl scenarioA = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenarioA.setNetwork(network);
		PopulationImpl plansA = (PopulationImpl) scenarioA.getPopulation();
		plansA.setIsStreaming(true);
		PopulationWriter pw = new PopulationWriter(plansA, network);
		pw.startStreaming(outputPlansFilename);
		new MatsimPopulationReader(scenarioA).readFile(plansFilenameA);
		new CopyPlans(pw).run(plansA);

		ScenarioImpl scenarioB = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenarioB.setNetwork(network);
		PopulationImpl plansB = (PopulationImpl) scenarioB.getPopulation();
		plansB.setIsStreaming(true);
		new MatsimPopulationReader(scenarioB).readFile(plansFilenameB);
		new PersonIdCopyPlans(pw, lower_limit).run(plansB);
		pw.closeStreaming();
	}
}
