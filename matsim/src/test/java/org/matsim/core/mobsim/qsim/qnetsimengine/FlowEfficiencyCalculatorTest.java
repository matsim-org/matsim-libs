
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

import org.junit.Assert;
import org.junit.Test;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Provides;

public class FlowEfficiencyCalculatorTest {
	@Test
	public void testFlowEfficiencyCalculator() {
		// In this test we send 1000 vehicles over a link with capacity 500. We then
		// define a custom FlowEfficiencyCalculator that varies the flow efficiency
		// globally. We see that with infinite flow efficiency, vehicles move in
		// freeflow state. For doubled flow efficiency, the longest travel time is
		// around halfed. For halfed flow efficiency, travel time is around doubled.

		double latestArrivalTime;

		latestArrivalTime = runTestScenario(Double.POSITIVE_INFINITY);
		Assert.assertEquals(1003.0, latestArrivalTime, 1e-3);

		latestArrivalTime = runTestScenario(1.0);
		Assert.assertEquals(8195.0, latestArrivalTime, 1e-3);

		latestArrivalTime = runTestScenario(2.0);
		Assert.assertEquals(4599.0, latestArrivalTime, 1e-3);

		latestArrivalTime = runTestScenario(0.5);
		Assert.assertEquals(15388.0, latestArrivalTime, 1e-3);
	}

	public double runTestScenario(double factor) {
		Scenario scenario = createScenario();
		Controler controler = new Controler(scenario);

		LatestArrivalHandler arrivalHandler = new LatestArrivalHandler();
		CustomFlowEfficiencyCalculator flowEfficiencyCalculator = new CustomFlowEfficiencyCalculator(factor);

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
				factory.setFlowEfficiencyCalculator(flowEfficiencyCalculator);
				return factory;
			}
		});

		controler.run();

		return arrivalHandler.latestArrivalTime;
	}

	private class CustomFlowEfficiencyCalculator implements FlowEfficiencyCalculator {
		private final double factor;

		public CustomFlowEfficiencyCalculator(double factor) {
			this.factor = factor;
		}

		@Override
		public double calculateFlowEfficiency(Vehicle vehicle, Link link) {
			return factor;
		}
	}

	private class LatestArrivalHandler implements PersonArrivalEventHandler {
		double latestArrivalTime = Time.getUndefinedTime();

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getLinkId().equals(Id.createLinkId("CD"))) {
				latestArrivalTime = event.getTime();
			}
		}
	}

	private Scenario createScenario() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);

		ActivityParams genericParams = new ActivityParams("generic");
		genericParams.setTypicalDuration(1.0);

		config.planCalcScore().addActivityParams(genericParams);

		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		Node nodeA = networkFactory.createNode(Id.createNodeId("A"), new Coord(0.0, 0.0));
		Node nodeB = networkFactory.createNode(Id.createNodeId("B"), new Coord(10000.0, 20000.0));
		Node nodeC = networkFactory.createNode(Id.createNodeId("C"), new Coord(10000.0, 30000.0));
		Node nodeD = networkFactory.createNode(Id.createNodeId("D"), new Coord(10000.0, 40000.0));

		Link linkAB = networkFactory.createLink(Id.createLinkId("AB"), nodeA, nodeB);
		Link linkBC = networkFactory.createLink(Id.createLinkId("BC"), nodeB, nodeC);
		Link linkCD = networkFactory.createLink(Id.createLinkId("CD"), nodeC, nodeD);

		Arrays.asList(nodeA, nodeB, nodeC, nodeD).forEach(network::addNode);
		Arrays.asList(linkAB, linkBC, linkCD).forEach(network::addLink);

		linkAB.setFreespeed(10000.0);
		linkBC.setFreespeed(10.0);
		linkCD.setFreespeed(10000.0);

		linkAB.setCapacity(1000.0);
		linkBC.setCapacity(500.0);
		linkCD.setCapacity(1000.0);

		Population population = scenario.getPopulation();
		PopulationFactory populationFactory = population.getFactory();

		for (int i = 0; i < 1000; i++) {
			Person person = populationFactory.createPerson(Id.createPersonId("person" + i));
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
			plan.addActivity(activity);
		}

		return scenario;
	}
}
