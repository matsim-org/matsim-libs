/* *********************************************************************** *
 * project: org.matsim.*
 * ArgumentParserTest.java
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

package org.matsim.core.utils.timing;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup.TripDurationHandling;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class TimeInterpretationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testIgnoreDelays() {
		Config config = ConfigUtils.createConfig();
		config.plans().setTripDurationHandling(TripDurationHandling.ignoreDelays);

		Controler controller = prepareController(config);
		controller.run();

		Person person = controller.getScenario().getPopulation().getPersons().values().iterator().next();
		List<? extends PlanElement> elements = person.getSelectedPlan().getPlanElements();

		Leg firstLeg = (Leg) elements.get(1);
		Leg secondLeg = (Leg) elements.get(3);

		assertEquals(15600.0, firstLeg.getTravelTime().seconds(), 0);
		assertEquals(28800.0, firstLeg.getDepartureTime().seconds(), 0);
		assertEquals(43200.0, secondLeg.getDepartureTime().seconds(), 0);
		// End time was NOT shifted (although arrival is later), second departure is assumed at 12:00
	}


	@Test
	void testShiftActivityEndTime() {
		Config config = ConfigUtils.createConfig();
		config.plans().setTripDurationHandling(TripDurationHandling.shiftActivityEndTimes);

		Controler controller = prepareController(config);
		controller.run();

		Person person = controller.getScenario().getPopulation().getPersons().values().iterator().next();
		List<? extends PlanElement> elements = person.getSelectedPlan().getPlanElements();

		Leg firstLeg = (Leg) elements.get(1);
		Leg secondLeg = (Leg) elements.get(3);

		assertEquals(15600.0, firstLeg.getTravelTime().seconds(), 0);
		assertEquals(28800.0, firstLeg.getDepartureTime().seconds(), 0);
		assertEquals(44400.0, secondLeg.getDepartureTime().seconds(), 0);
		// End time WAS shifted (because arrival is later), second departure is assumed at 12:20
	}

	private Controler prepareController(Config config) {
		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(0);

		ActivityParams genericParams = new ActivityParams("generic");
		genericParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(genericParams);

		Scenario scenario = ScenarioUtils.createScenario(config);

		Node firstNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId("firstNode"),
				new Coord(0.0, 0.0));
		Node secondNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId("secondeNode"),
				new Coord(0.0, 0.0));
		Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId("link"), firstNode, secondNode);

		scenario.getNetwork().addNode(firstNode);
		scenario.getNetwork().addNode(secondNode);
		scenario.getNetwork().addLink(link);

		PopulationFactory factory = scenario.getPopulation().getFactory();

		Person person = factory.createPerson(Id.createPersonId("person"));
		scenario.getPopulation().addPerson(person);

		Plan plan = factory.createPlan();
		person.addPlan(plan);

		Activity firstActivity = factory.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		firstActivity.setEndTime(8.0 * 3600.0);
		plan.addActivity(firstActivity);

		Leg firstLeg = factory.createLeg("walk");
		plan.addLeg(firstLeg);

		Activity secondActivity = factory.createActivityFromCoord("generic", new Coord(10.0 * 1e3, 0.0)); // 10km
		secondActivity.setEndTime(12.0 * 3600.0);
		plan.addActivity(secondActivity);

		Leg secondLeg = factory.createLeg("walk");
		plan.addLeg(secondLeg);

		Activity thirdActivity = factory.createActivityFromCoord("generic", new Coord(0.0, 0.0));
		plan.addActivity(thirdActivity);

		return new Controler(scenario);
	}
}
