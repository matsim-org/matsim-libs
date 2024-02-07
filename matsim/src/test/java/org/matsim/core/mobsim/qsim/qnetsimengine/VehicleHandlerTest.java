
/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleHandlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Provides;

public class VehicleHandlerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testVehicleHandler() {
		// This is a test where there is a link with a certain parking capacity. As soon
		// as
		// it is reached the link is blocking, until a vehicle is leaving the link
		// again. In
		// this case there are three agents which do a longer stop on the capacitated
		// link,
		// but they all have the same route and plan. This means that if the capacity is
		// above 3, all agents will perform their plans without any distraction. If the
		// capacity is set to 2, the third agent needs to wait until the first of the
		// previous ones is leaving, and so on ...

		Result result;

		result = runTestScenario(4);
		Assertions.assertEquals(20203.0, result.latestArrivalTime, 1e-3);
		Assertions.assertEquals(3, result.initialCount);

		result = runTestScenario(3);
		Assertions.assertEquals(20203.0, result.latestArrivalTime, 1e-3);
		Assertions.assertEquals(3, result.initialCount);

		result = runTestScenario(2);
		Assertions.assertEquals(23003.0, result.latestArrivalTime, 1e-3);
		Assertions.assertEquals(3, result.initialCount);

		result = runTestScenario(1);
		Assertions.assertEquals(33003.0, result.latestArrivalTime, 1e-3);
		Assertions.assertEquals(3, result.initialCount);
	}

	public Result runTestScenario(long capacity) {
		Scenario scenario = createScenario();
		Controler controler = new Controler(scenario);

		LatestArrivalHandler arrivalHandler = new LatestArrivalHandler();
		BlockingVehicleHandler vehicleHandler = new BlockingVehicleHandler(capacity);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(arrivalHandler);
			}
		});

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
			}

			@Provides
			QNetworkFactory provideQNetworkFactory(EventsManager eventsManager, Scenario scenario) {
				ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(eventsManager, scenario);
				factory.setVehicleHandler(vehicleHandler);
				return factory;
			}
		});

		controler.run();

		Result result = new Result();
		result.latestArrivalTime = arrivalHandler.latestArrivalTime;
		result.initialCount = vehicleHandler.initialCount;
		return result;
	}

	private static class Result {
		double latestArrivalTime;
		long initialCount;
	}

	private static class BlockingVehicleHandler implements VehicleHandler {
		private final long capacity;

		long initialCount = 0;
		long count = 0;

		public BlockingVehicleHandler(long capacity) {
			this.capacity = capacity;
		}

		@Override
		public void handleVehicleDeparture(QVehicle vehicle, Link link) {
			if (link.getId().equals(Id.createLinkId("CD"))) {
				count--;
			}
		}

		@Override
		public boolean handleVehicleArrival(QVehicle vehicle, Link link) {
			if (link.getId().equals(Id.createLinkId("CD"))) {
				if (count >= capacity) {
					return false;
				}

				count++;
			}

			return true;
		}

		@Override
		public void handleInitialVehicleArrival(QVehicle vehicle, Link link) {
			if (link.getId().equals(Id.createLinkId("AB"))) {
				initialCount++;
			} else {
				throw new IllegalStateException("Only AB should have initial vehicles.");
			}
		}
	}

	private static class LatestArrivalHandler implements PersonArrivalEventHandler {
		Double latestArrivalTime = null;

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getLinkId().equals(Id.createLinkId("DE"))) {
				latestArrivalTime = event.getTime();
			}
		}
	}

	private Scenario createScenario() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		ActivityParams genericParams = new ActivityParams("generic");
		genericParams.setTypicalDuration(1.0);

		config.scoring().addActivityParams(genericParams);

		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		Node nodeA = networkFactory.createNode(Id.createNodeId("A"), new Coord(0.0, 0.0));
		Node nodeB = networkFactory.createNode(Id.createNodeId("B"), new Coord(1000.0, 2000.0));
		Node nodeC = networkFactory.createNode(Id.createNodeId("C"), new Coord(1000.0, 3000.0));
		Node nodeD = networkFactory.createNode(Id.createNodeId("D"), new Coord(1000.0, 4000.0));
		Node nodeE = networkFactory.createNode(Id.createNodeId("E"), new Coord(1000.0, 5000.0));

		Link linkAB = networkFactory.createLink(Id.createLinkId("AB"), nodeA, nodeB);
		Link linkBC = networkFactory.createLink(Id.createLinkId("BC"), nodeB, nodeC);
		Link linkCD = networkFactory.createLink(Id.createLinkId("CD"), nodeC, nodeD);
		Link linkDE = networkFactory.createLink(Id.createLinkId("DE"), nodeD, nodeE);

		Arrays.asList(nodeA, nodeB, nodeC, nodeD, nodeE).forEach(network::addNode);
		Arrays.asList(linkAB, linkBC, linkCD, linkDE).forEach(network::addLink);

		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		Person person1 = populationFactory.createPerson(Id.createPersonId("person1"));
		Person person2 = populationFactory.createPerson(Id.createPersonId("person2"));
		Person person3 = populationFactory.createPerson(Id.createPersonId("person3"));

		for (Person person : Arrays.asList(person1, person2, person3)) {
			population.addPerson(person);

			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);

			Activity activity;
			Leg leg;

			activity = populationFactory.createActivityFromLinkId("generic", linkAB.getId());
			activity.setEndTime(0.0);
			plan.addActivity(activity);

			leg = populationFactory.createLeg("car");
			plan.addLeg(leg);

			activity = populationFactory.createActivityFromLinkId("generic", linkCD.getId());
			activity.setMaximumDuration(10000.0);
			plan.addActivity(activity);

			leg = populationFactory.createLeg("car");
			plan.addLeg(leg);

			activity = populationFactory.createActivityFromLinkId("generic", linkDE.getId());
			plan.addActivity(activity);
		}

		return scenario;
	}
}
