/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package tutorial.programming.ownPrepareForSimExample;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example how to use your own (Person)PrepareForSim.
 * This example replaces xy2Links by using NetworkImpl.getNearestLinkExactly()
 * to determine the nearest link. 
 * 
 * @author tthunig
 *
 */
public class RunOwnPrepareForSimExample {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		
		config.controler().setOutputDirectory("output/ownPrepareForSimExample/");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		
		config.controler().setLastIteration(0);
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);

		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		createPopulation(scenario);
		
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(PrepareForSim.class).to(MyPrepareForSim.class);
			}
		});

		controler.run();
	}

	private static void createPopulation(Scenario scenario) {
		Population pop = scenario.getPopulation();
		PopulationFactory fac = pop.getFactory();
		
		Person person = fac.createPerson(Id.createPersonId(1));
		Plan plan = fac.createPlan();
		Activity dummyAct1 = fac.createActivityFromCoord("dummy", new Coord(0.0, 0.0));
		dummyAct1.setEndTime(0.0);
		plan.addActivity(dummyAct1);
		Leg leg = fac.createLeg(TransportMode.car);
		plan.addLeg(leg);
		Activity dummyAct2 = fac.createActivityFromLinkId("dummy", Id.createLinkId("1_4"));
		plan.addActivity(dummyAct2);
		person.addPlan(plan);
		pop.addPerson(person);
	}

	/**
	 * Create a network like this:
	 * 
	 *       +
	 *        \
	 *         \
	 *          \
	 *           +
	 *     
	 * + ------------------ +
	 *           *
	 * 
	 * where + are nodes and * is the home location of the single agent
	 * 
	 * @param scenario
	 */
	private static void createNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();
		NetworkFactory fac = network.getFactory();

		Node node1 = fac.createNode(Id.createNodeId(1), new Coord(-100.0, 1.0));
		Node node2 = fac.createNode(Id.createNodeId(2), new Coord(100.0, 1.0));
		Node node3 = fac.createNode(Id.createNodeId(3), new Coord(0.0, 10.0));
		Node node4 = fac.createNode(Id.createNodeId(4), new Coord(-10.0, 100.0));
		
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		
		Link link12 = fac.createLink(Id.createLinkId("1_2"), node1, node2);
		Link link21 = fac.createLink(Id.createLinkId("2_1"), node2, node1);
		Link link34 = fac.createLink(Id.createLinkId("3_4"), node3, node4);
		Link link43 = fac.createLink(Id.createLinkId("4_3"), node4, node3);
		// create some links to connect network
		Link link41 = fac.createLink(Id.createLinkId("4_1"), node4, node1);
		Link link14 = fac.createLink(Id.createLinkId("1_4"), node1, node4);
		Link link23 = fac.createLink(Id.createLinkId("2_3"), node2, node3);
		Link link32 = fac.createLink(Id.createLinkId("3_2"), node3, node2);
		
		network.addLink(link12);
		network.addLink(link21);
		network.addLink(link34);
		network.addLink(link43);
		network.addLink(link41);
		network.addLink(link14);
		network.addLink(link23);
		network.addLink(link32);
	}

}
