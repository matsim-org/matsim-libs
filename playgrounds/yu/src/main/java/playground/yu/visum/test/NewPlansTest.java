/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.yu.visum.filter.PersonFilterAlgorithm;
import playground.yu.visum.filter.PersonRouteFilter;
import playground.yu.visum.filter.finalFilters.NewPlansWriter;

/**
 * @author ychen
 */
public class NewPlansTest {

	public static void testRun(Config config) {

		System.out.println("TEST RUN ---FilterTest---:");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(config);
		// reading all available input

		System.out.println("  reading network xml file... ");
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");

		System.out.println("  creating plans object... ");
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		System.out.println("  done.");

		System.out.println("  setting plans algorithms... ");
		PersonFilterAlgorithm pfa = new PersonFilterAlgorithm();
		List<Id<Link>> linkIDs = new ArrayList<>();
		List<Id<Node>> nodeIDs = new ArrayList<>();
		linkIDs.add(Id.create(101589, Link.class));
		linkIDs.add(Id.create( 27469, Link.class));
		linkIDs.add(Id.create(102086, Link.class));
		linkIDs.add(Id.create(101867, Link.class));
		linkIDs.add(Id.create(102235, Link.class));
		linkIDs.add(Id.create(101944, Link.class));
		linkIDs.add(Id.create(101866, Link.class));
		linkIDs.add(Id.create(102224, Link.class));
		linkIDs.add(Id.create(102015, Link.class));
		linkIDs.add(Id.create(101346, Link.class));
		linkIDs.add(Id.create(101845, Link.class));
		linkIDs.add(Id.create(101487, Link.class));
		linkIDs.add(Id.create(102016, Link.class));
		linkIDs.add(Id.create(101417, Link.class));
		linkIDs.add(Id.create(102225, Link.class));
		linkIDs.add(Id.create(100970, Link.class));
		linkIDs.add(Id.create(102234, Link.class));
		linkIDs.add(Id.create(101588, Link.class));
		linkIDs.add(Id.create(101945, Link.class));
		linkIDs.add(Id.create(102087, Link.class));
		linkIDs.add(Id.create(100971, Link.class));
		linkIDs.add(Id.create(102017, Link.class));
		linkIDs.add(Id.create(102226, Link.class));
		linkIDs.add(Id.create(102160, Link.class));
		linkIDs.add(Id.create( 27470, Link.class));
		linkIDs.add(Id.create(101804, Link.class));
		linkIDs.add(Id.create(101416, Link.class));
		linkIDs.add(Id.create(102083, Link.class));
		linkIDs.add(Id.create(102004, Link.class));
		linkIDs.add(Id.create(102014, Link.class));
		linkIDs.add(Id.create(102227, Link.class));
		linkIDs.add(Id.create( 27789, Link.class));
		linkIDs.add(Id.create(102170, Link.class));
		linkIDs.add(Id.create(100936, Link.class));
		linkIDs.add(Id.create(101347, Link.class));
		linkIDs.add(Id.create(101805, Link.class));
		linkIDs.add(Id.create(101844, Link.class));
		linkIDs.add(Id.create(102082, Link.class));
		linkIDs.add(Id.create(102171, Link.class));
		linkIDs.add(Id.create(102161, Link.class));
		linkIDs.add(Id.create(100937, Link.class));
		linkIDs.add(Id.create(102131, Link.class));
		linkIDs.add(Id.create(101784, Link.class));
		linkIDs.add(Id.create(102176, Link.class));
		linkIDs.add(Id.create( 27736, Link.class));
		linkIDs.add(Id.create(101785, Link.class));
		linkIDs.add(Id.create( 27790, Link.class));
		linkIDs.add(Id.create(102130, Link.class));
		linkIDs.add(Id.create( 27735, Link.class));
		linkIDs.add(Id.create(102177, Link.class));
		linkIDs.add(Id.create(102005, Link.class));
		linkIDs.add(Id.create(101486, Link.class));

		nodeIDs.add(Id.create(990262, Node.class));
		nodeIDs.add(Id.create(990340, Node.class));
		nodeIDs.add(Id.create(630401, Node.class));
		nodeIDs.add(Id.create(990253, Node.class));
		nodeIDs.add(Id.create(680303, Node.class));
		nodeIDs.add(Id.create(990218, Node.class));
		nodeIDs.add(Id.create(720464, Node.class));
		nodeIDs.add(Id.create(610604, Node.class));
		nodeIDs.add(Id.create(690447, Node.class));
		nodeIDs.add(Id.create(660374, Node.class));
		nodeIDs.add(Id.create(530030, Node.class));
		nodeIDs.add(Id.create(990266, Node.class));
		nodeIDs.add(Id.create(990285, Node.class));
		nodeIDs.add(Id.create(990311, Node.class));
		nodeIDs.add(Id.create(990370, Node.class));
		nodeIDs.add(Id.create(690611, Node.class));
		nodeIDs.add(Id.create(990378, Node.class));
		nodeIDs.add(Id.create(990683, Node.class));
		nodeIDs.add(Id.create(990204, Node.class));
		nodeIDs.add(Id.create(710012, Node.class));
		nodeIDs.add(Id.create(990222, Node.class));
		nodeIDs.add(Id.create(990217, Node.class));

		PersonRouteFilter prf = new PersonRouteFilter(linkIDs, nodeIDs, network);
		NewPlansWriter npw = new NewPlansWriter(plans, network);
		pfa.setNextFilter(prf);
		prf.setNextFilter(npw);
		System.out.println("  done.");

		System.out.println("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(config.plans().getInputFile());
		System.out.println("  done.");

		System.out.println("  running plans algos ... ");
		pfa.run(plans);
		npw.writeEndPlans();
		// //////////////////////////////////////////////////////////////////////////////////////////////////

		System.out.println("we have " + pfa.getCount()
				+ "persons at last -- PersonFilterAlgorithm.");
		System.out.println("we have " + prf.getCount()
				+ "persons at last -- PersonRouteFilter.");
		System.out.println("we have " + npw.getCount()
				+ "persons at last -- NewPlansWriter.");
		System.out.println("  done.");
		// writing all available input

		System.out.println("NewPlansWriter SUCCEEDED.");
	}

	/**
	 * @param args
	 *            test/yu/config_newPlan.xml config_v1.dtd
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		Gbl.startMeasurement();
		Config config = ScenarioLoaderImpl
				.createScenarioLoaderImplAndResetRandomSeed(args[0])
				.loadScenario().getConfig();
		testRun(config);
		Gbl.printElapsedTime();
	}
}
