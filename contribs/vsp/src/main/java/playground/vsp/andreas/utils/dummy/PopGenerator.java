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

package playground.vsp.andreas.utils.dummy;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * Creates some simple plans files in a straight forward way.
 * 
 * @author aneumann
 *
 */
public class PopGenerator {
	
	
	public static void main(String[] args) {
		
		String outputDir = "E:/temp/";
		String networkFilename = outputDir + "network_tut.xml";
		int nPersonsPerHour = 72;
		
		PopGenerator.createPopTut(networkFilename, nPersonsPerHour, outputDir + "pop_tut.xml.gz");
		
//		PopGenerator.createPopT1(networkFilename, nPersonsPerHour, outputDir + "pop_corr_t_1.xml.gz");
//		PopGenerator.createPopT2(networkFilename, nPersonsPerHour, outputDir + "pop_corr_t_2.xml.gz");
//		PopGenerator.createPopT3(networkFilename, nPersonsPerHour, outputDir + "pop_corr_t_3.xml.gz");
//		PopGenerator.createPopT4(networkFilename, nPersonsPerHour, outputDir + "pop_corr_t_4.xml.gz");
//		
//		PopGenerator.createPopS1(networkFilename, nPersonsPerHour, outputDir + "pop_corr_s_1.xml.gz");
//		PopGenerator.createPopS2(networkFilename, nPersonsPerHour, outputDir + "pop_corr_s_2.xml.gz");
//		PopGenerator.createPopS3(networkFilename, nPersonsPerHour, outputDir + "pop_corr_s_3.xml.gz");
//		PopGenerator.createPopS4(networkFilename, nPersonsPerHour, outputDir + "pop_corr_s_4.xml.gz");
//		
//		networkFilename = outputDir + "network_cross.xml";
//		nPersonsPerHour = 1000;
//		PopGenerator.createPopCross(networkFilename, nPersonsPerHour, outputDir + "pop_cross.xml.gz");
		
//		networkFilename = outputDir + "network_corridor.xml";
//		nPersonsPerHour = 1000;
//		PopGenerator.createPopVirginiaCorridor(networkFilename, nPersonsPerHour, outputDir + "pop_corridor_1000.xml.gz", 7, 9);
//		PopGenerator.createPopVirginiaCorridor(networkFilename, nPersonsPerHour, outputDir + "pop_corridor_1000_short.xml.gz", 7, 8);
	}
	
	private static void createPopTut(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord nodeACoord = sc.getNetwork().getNodes().get(Id.create("14", Node.class)).getCoord();
		Coord nodeBCoord = sc.getNetwork().getNodes().get(Id.create("44", Node.class)).getCoord();
		Coord nodeCCoord = sc.getNetwork().getNodes().get(Id.create("11", Node.class)).getCoord();
		Coord nodeDCoord = sc.getNetwork().getNodes().get(Id.create("41", Node.class)).getCoord();
		
		// create trips from node A to node B and trips from node B to node A, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeACoord, nodeBCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeBCoord, nodeACoord, 6, 10);
		
		// create trips from node A to node C and trips from node C to node A, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeACoord, nodeCCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeCCoord, nodeACoord, 6, 10);
		
		// create trips from node A to node D and trips from node D to node A, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeACoord, nodeDCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeDCoord, nodeACoord, 6, 10);
		
		// create trips from node B to node C and trips from node C to node B, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeBCoord, nodeCCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeCCoord, nodeBCoord, 6, 10);

		// create trips from node B to node D and trips from node D to node B, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeBCoord, nodeDCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeDCoord, nodeBCoord, 6, 10);
		
		// create trips from node C to node D and trips from node D to node C, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeCCoord, nodeDCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeDCoord, nodeCCoord, 6, 10);
		
		new PopulationWriter(pop, null).write(outFilename);		
	}

	private static void createPopCross(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord nodeACoord = sc.getNetwork().getNodes().get(Id.create("A", Node.class)).getCoord();
		Coord nodeBCoord = sc.getNetwork().getNodes().get(Id.create("B", Node.class)).getCoord();
		
		// create trips from node A to node B and trips from node A to node B, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeACoord, nodeBCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeBCoord, nodeACoord, 6, 10);
		
		Coord nodeCCoord = sc.getNetwork().getNodes().get(Id.create("C", Node.class)).getCoord();
		Coord nodeDCoord = sc.getNetwork().getNodes().get(Id.create("D", Node.class)).getCoord();
		
		// create trips from node C to node D and trips from node C to node D, 6-10
		createPersons(rnd, pop, nPersonsPerHour, nodeCCoord, nodeDCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, nodeDCoord, nodeCCoord, 6, 10);
		
		// create trips from node A to node C and trips from node C to node A, 6-10
		createPersons(rnd, pop, nPersonsPerHour / 10, nodeACoord, nodeCCoord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour / 10, nodeCCoord, nodeACoord, 6, 10);
						
		new PopulationWriter(pop, null).write(outFilename);		
	}

	private static void createPopT1(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node6Coord = sc.getNetwork().getNodes().get(Id.create(6, Node.class)).getCoord();
		
		// create trips from node 2 to node 6 and trips from node 6 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 6, 10);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopT2(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node6Coord = sc.getNetwork().getNodes().get(Id.create(6, Node.class)).getCoord();
		
		// create trips from node 2 to node 6 and trips from node 6 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 6, 10);
		
		// create additional trips from node 2 to node 6 and trips from node 6 to node 2, 16-20
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 16, 20);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 16, 20);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopT3(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node6Coord = sc.getNetwork().getNodes().get(Id.create(6, Node.class)).getCoord();
		
		// create trips from node 2 to node 6 and trips from node 6 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 6, 10);
		
		// create additional trips from node 2 to node 6 and trips from node 6 to node 2, 16-20
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 16, 20);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 16, 20);
		
		// create additional trips from node 2 to node 6 and trips from node 6 to node 2, 6-20
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 6, 20);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 6, 20);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopT4(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node6Coord = sc.getNetwork().getNodes().get(Id.create(6, Node.class)).getCoord();
		
		// create trips from node 2 to node 6 and trips from node 6 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 6, 10);
		
		// create additional trips from node 2 to node 6 and trips from node 6 to node 2, 16-20
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 16, 20);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 16, 20);
		
		// create additional trips from node 2 to node 6 and trips from node 6 to node 2, 6-16
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node6Coord, 6, 16);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node2Coord, 6, 16);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopS1(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node3Coord = sc.getNetwork().getNodes().get(Id.create(3, Node.class)).getCoord();
		
		// create trips from node 2 to node 3 and trips from node 3 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node3Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node3Coord, node2Coord, 6, 10);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopS2(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node3Coord = sc.getNetwork().getNodes().get(Id.create(3, Node.class)).getCoord();
		Coord node5Coord = sc.getNetwork().getNodes().get(Id.create(5, Node.class)).getCoord();
		Coord node6Coord = sc.getNetwork().getNodes().get(Id.create(6, Node.class)).getCoord();
		
		// create trips from node 2 to node 3 and trips from node 3 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node3Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node3Coord, node2Coord, 6, 10);
		
		// create additional trips from node 5 to node 6 and trips from node 6 to node 5, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node5Coord, node6Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node5Coord, 6, 10);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopS3(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node3Coord = sc.getNetwork().getNodes().get(Id.create(3, Node.class)).getCoord();
		Coord node5Coord = sc.getNetwork().getNodes().get(Id.create(5, Node.class)).getCoord();
		Coord node6Coord = sc.getNetwork().getNodes().get(Id.create(6, Node.class)).getCoord();
		
		// create trips from node 2 to node 3 and trips from node 3 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node3Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node3Coord, node2Coord, 6, 10);
		
		// create additional trips from node 5 to node 6 and trips from node 6 to node 5, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node5Coord, node6Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node5Coord, 6, 10);
		
		// create additional trips from node 3 to node 5 and trips from node 5 to node 3, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node3Coord, node5Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node5Coord, node3Coord, 6, 10);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopS4(String networkFilename, int nPersonsPerHour, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create(2, Node.class)).getCoord();
		Coord node3Coord = sc.getNetwork().getNodes().get(Id.create(3, Node.class)).getCoord();
		Coord node5Coord = sc.getNetwork().getNodes().get(Id.create(5, Node.class)).getCoord();
		Coord node6Coord = sc.getNetwork().getNodes().get(Id.create(6, Node.class)).getCoord();
		
		// create trips from node 2 to node 3 and trips from node 3 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node3Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node3Coord, node2Coord, 6, 10);
		
		// create additional trips from node 5 to node 6 and trips from node 6 to node 5, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node5Coord, node6Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node6Coord, node5Coord, 6, 10);
		
		// create additional trips from node 2 to node 5 and trips from node 5 to node 2, 6-10
		createPersons(rnd, pop, nPersonsPerHour, node2Coord, node5Coord, 6, 10);
		createPersons(rnd, pop, nPersonsPerHour, node5Coord, node2Coord, 6, 10);
						
		new PopulationWriter(pop, null).write(outFilename);
	}
	
	private static void createPopVirginiaCorridor(String networkFilename, int nPersonsPerHour, String outFilename, int departureIntervalStart, int departureIntervalEnd) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Coord node1Coord = sc.getNetwork().getNodes().get(Id.create("1", Node.class)).getCoord();
		Coord node2Coord = sc.getNetwork().getNodes().get(Id.create("2", Node.class)).getCoord();
		
		// create trips from node 1 to node 2
		createPersons(rnd, pop, nPersonsPerHour, node1Coord, node2Coord, departureIntervalStart, departureIntervalEnd);
		
		new PopulationWriter(pop, null).write(outFilename);		
	}

	private static void createPersons(Random rnd, Population pop, int nPersonsPerHour, Coord fromCoord, Coord toCoord, int departureIntervalStart, int departureIntervalEnd) {
		int nPersonsCreated = pop.getPersons().size();
		int nPersonsToBeCreated = (departureIntervalEnd - departureIntervalStart) * nPersonsPerHour;
		
		for (int i = 0; i < nPersonsToBeCreated; i++) {
			nPersonsCreated++;
			Person person = pop.getFactory().createPerson(Id.create(nPersonsCreated, Person.class));
			Plan plan = pop.getFactory().createPlan();
			
			Activity h1 = pop.getFactory().createActivityFromCoord("h", fromCoord);
			h1.setEndTime(departureIntervalStart * 3600.0 + rnd.nextDouble() * (departureIntervalEnd - departureIntervalStart) * 3600.0);
			plan.addActivity(h1);

			Leg leg = pop.getFactory().createLeg("pt");
			plan.addLeg(leg);

			Activity h2 = pop.getFactory().createActivityFromCoord("h", toCoord);
			plan.addActivity(h2);

			person.addPlan(plan);
			pop.addPerson(person);
		}
	}
}
