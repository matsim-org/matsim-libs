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

package playground.yu.visum.test;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.yu.visum.filter.ActTypeFilter;
import playground.yu.visum.filter.DepTimeFilter;
import playground.yu.visum.filter.PersonFilterAlgorithm;
import playground.yu.visum.filter.PersonIDFilter;
import playground.yu.visum.filter.finalFilters.PersonIDsExporter;

/**
 * @author ychen
 */
public class PersonFilterTest {

	public static void testRunIDandActTypeundDepTimeFilter(Config config) {

		System.out.println("TEST RUN ---FilterTest---:");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		// reading all available input

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(scenario).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		System.out.println("  done.");

		System.out.println("  setting plans algorithms... ");
		PersonIDsExporter pid = new PersonIDsExporter();
		DepTimeFilter dtf = new DepTimeFilter();
		ActTypeFilter atf = new ActTypeFilter();
		PersonIDFilter idf = new PersonIDFilter(10);
		PersonFilterAlgorithm pfa = new PersonFilterAlgorithm();
		pfa.setNextFilter(idf);
		idf.setNextFilter(atf);
		atf.setNextFilter(dtf);
		dtf.setNextFilter(pid);
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  running plans algos ... ");
		pfa.run(plans);
		System.out.println("we have " + pfa.getCount()
				+ "persons at last -- FilterAlgorithm");
		System.out.println("we have " + idf.getCount()
				+ "persons at last -- PersonIDFilter");
		System.out.println("we have " + atf.getCount()
				+ "persons at last -- ActTypeFilter");
		System.out.println("we have " + dtf.getCount()
				+ "persons at last -- DepTimeFilter");
		System.out.println("  done.");
		// writing all available input

		System.out.println("PersonFiterTEST SUCCEEDED.");
		System.out.println();
	}

	/**
	 * @param args
	 *            test/yu/config_hms.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		Config config = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]).loadScenario().getConfig();
		testRunIDandActTypeundDepTimeFilter(config);
		Gbl.printElapsedTime();
	}
}
