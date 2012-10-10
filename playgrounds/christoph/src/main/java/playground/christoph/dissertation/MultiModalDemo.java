/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalDemo.java
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

package playground.christoph.dissertation;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Demonstrate the influence of age and gender on persons' walk speed.
 * 
 * Scenario:
 * <ul>
 * 	<li>Have two routes which connect a start and an end link.</li>
 * 	<li>One is car (long), one is walk (short).</li>
 * 	<li>Choose their length so that a 30 year old male's is walk travel time
 * 		exactly matches the car travel time.</li> 
 * 	<li>Create 1000 agents with random age between 1 and 100 and random gender.</li>
 * 	<li>Create two initial populations: one with initial car routes, one with
 * 		initial walk route.</li>
 * 	<li>Repeat the experiment but replace walk by bike.</li>
 * </ul>
 *  @author cdobler
 */
public class MultiModalDemo {
	
	private static final Logger log = Logger.getLogger(MultiModalDemo.class);
	
	private static int numPersons = 1000;
	private static String initialLegMode = TransportMode.car; 
	
	public static void main(String[] args) {
		
		// create and initialze config
		Config config = ConfigUtils.createConfig();
		
		QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
		qSimConfigGroup.setNumberOfThreads(1);
		qSimConfigGroup.setStartTime(0.0);
		qSimConfigGroup.setEndTime(86400.0);
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setRemoveStuckVehicles(false);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setVehicleBehavior(QSimConfigGroup.VEHICLE_BEHAVIOR_EXCEPTION);
		config.addQSimConfigGroup(qSimConfigGroup);
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(500);
		config.controler().setMobsim(ControlerConfigGroup.MobsimType.qsim.toString());
		config.controler().setOutputDirectory("D:/output");
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		createPopulation(scenario);
		
		Controler controler = new Controler(scenario);
		controler.run();
	}
	
	private static void createNetwork(Scenario scenario) {
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		
		Node n1 = networkFactory.createNode(scenario.createId("n1"), scenario.createCoord(0.0, 0.0));
		Node n2 = networkFactory.createNode(scenario.createId("n2"), scenario.createCoord(100.0, 0.0));
		Node n3 = networkFactory.createNode(scenario.createId("n3"), scenario.createCoord(100.0, 1000.0));
		Node n4 = networkFactory.createNode(scenario.createId("n4"), scenario.createCoord(1100.0, 1000.0));
		Node n5 = networkFactory.createNode(scenario.createId("n5"), scenario.createCoord(1100.0, 0.0));
		Node n6 = networkFactory.createNode(scenario.createId("n6"), scenario.createCoord(1200.0, 0.0));
		
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		scenario.getNetwork().addNode(n4);
		scenario.getNetwork().addNode(n5);
		scenario.getNetwork().addNode(n6);
		
		Set<String> carMode = new HashSet<String>();
		Set<String> multiMode = new HashSet<String>();
		Set<String> nonCarMode = new HashSet<String>();
		
		carMode.add(TransportMode.car);
		nonCarMode.add(TransportMode.bike);
		nonCarMode.add(TransportMode.walk);
		multiMode.add(TransportMode.car);
		multiMode.add(TransportMode.bike);
		multiMode.add(TransportMode.walk);
		
		Link l1 = networkFactory.createLink(scenario.createId("l1"), n1, n2);
		l1.setLength(100.0);
		l1.setCapacity(Double.MAX_VALUE);
		l1.setFreespeed(50.0/3.6);
		l1.setAllowedModes(multiMode);
		
		Link l2 = networkFactory.createLink(scenario.createId("l2"), n2, n3);
		l2.setLength(1000.0);
		l2.setCapacity(Double.MAX_VALUE);
		l2.setFreespeed(50.0/3.6);
		l2.setAllowedModes(carMode);
		
		Link l3 = networkFactory.createLink(scenario.createId("l3"), n3, n4);
		l3.setLength(1000.0);
		l3.setCapacity(Double.MAX_VALUE);
		l3.setFreespeed(50.0/3.6);
		l3.setAllowedModes(carMode);

		Link l4 = networkFactory.createLink(scenario.createId("l4"), n4, n5);
		l4.setLength(1000.0);
		l4.setCapacity(Double.MAX_VALUE);
		l4.setFreespeed(50.0/3.6);
		l4.setAllowedModes(carMode);

		Link l5 = networkFactory.createLink(scenario.createId("l5"), n2, n5);
		l5.setLength(1000.0);
		l5.setCapacity(Double.MAX_VALUE);
		l5.setFreespeed(50.0/3.6);
		l5.setAllowedModes(nonCarMode);
		
		Link l6 = networkFactory.createLink(scenario.createId("l6"), n5, n6);
		l6.setLength(1000.0);
		l6.setCapacity(Double.MAX_VALUE);
		l6.setFreespeed(50.0/3.6);
		l6.setAllowedModes(multiMode);
		
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		scenario.getNetwork().addLink(l4);
		scenario.getNetwork().addLink(l5);
		scenario.getNetwork().addLink(l6);
	}
	
	private static void createPopulation(Scenario scenario) {
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		for (int i = 0; i < numPersons; i++) {
			Person person = populationFactory.createPerson(scenario.createId(String.valueOf(i)));
			
			Plan plan = populationFactory.createPlan();
			
			Activity a1 = populationFactory.createActivityFromLinkId("home", scenario.createId("l1"));
			a1.setEndTime(i * 20);
			
			Leg l1 = populationFactory.createLeg(initialLegMode);
			l1.setDepartureTime(i * 20);
			
			Activity a2 = populationFactory.createActivityFromLinkId("home", scenario.createId("l6"));
			
			plan.addActivity(a1);
			plan.addLeg(l1);
			plan.addActivity(a2);
			
			person.addPlan(plan);
			
			scenario.getPopulation().addPerson(person);
		}
	}
}
