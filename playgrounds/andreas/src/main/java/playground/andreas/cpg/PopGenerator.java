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

package playground.andreas.cpg;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.population.io.PopulationReader;
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
		
		String outputDir = "E:/cpg/siouxfalls-2014/input/";
		String networkFilename = outputDir + "network.xml.gz";
		String basePopulation = outputDir + "population.xml.gz";
		int nPersons = 1000;
		
		PopGenerator.createInCommuting(networkFilename, basePopulation, nPersons, outputDir + "pop_in_commuting.xml.gz");
	}
	
	private static void createInCommuting(String networkFilename, String basePopulation, int nPersons, String outFilename) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFilename);
		new PopulationReader(sc).readFile(basePopulation);
		Population pop = sc.getPopulation();
		
		MatsimRandom.reset(4711);
		Random rnd = MatsimRandom.getLocalInstance();
		
		Link fromLink = sc.getNetwork().getLinks().get(Id.create("3_10", Link.class));
		
		String[] toLinkIds = new String[]{"37_4", "34_4", "28_3", "53_2", "56_4"};
		List<Link> toLinks = new LinkedList<Link>();
		for (int i = 0; i < toLinkIds.length; i++) {
			Link toLink = sc.getNetwork().getLinks().get(Id.create(toLinkIds[i], Link.class)); toLinks.add(toLink);
		}
		
		// create trips from node A to node B and trips from node B to node A, 6-10
		createPersons(rnd, pop, nPersons, fromLink, toLinks, 7, 7.5);
		
		new PopulationWriter(pop, null).write(outFilename);		
	}

	private static void createPersons(Random rnd, Population pop, int nPersons, Link fromLink, List<Link> toLinks, int departureIntervalStart, double departureIntervalEnd) {
		int nPersonsCreated = pop.getPersons().size();
		
		for (int i = 0; i < nPersons; i++) {
			nPersonsCreated++;
			Person person = pop.getFactory().createPerson(Id.create("cadytsInCommuting_" + nPersonsCreated, Person.class));
			for (Link toLink : toLinks) {
				Plan plan = pop.getFactory().createPlan();
				
				Activity h1 = pop.getFactory().createActivityFromLinkId("home", fromLink.getId());
				h1.setCoord(fromLink.getToNode().getCoord());
				h1.setEndTime(departureIntervalStart * 3600.0 + rnd.nextDouble() * (departureIntervalEnd - departureIntervalStart) * 3600.0);
				plan.addActivity(h1);
				
				Leg leg = pop.getFactory().createLeg(TransportMode.car);
				plan.addLeg(leg);
				
				Activity h2 = pop.getFactory().createActivityFromLinkId("work", toLink.getId());
				h2.setCoord(toLink.getToNode().getCoord());
				plan.addActivity(h2);
				
				person.addPlan(plan);
			}
			pop.addPerson(person);
		}
	}
}
