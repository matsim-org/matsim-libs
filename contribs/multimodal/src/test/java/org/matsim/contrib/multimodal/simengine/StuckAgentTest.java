/* *********************t************************************************** *
 * project: org.matsim.*
 * MultiModalQLinkExtensionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.simengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.multimodal.MultiModalModule;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

public class StuckAgentTest {

	private static final Logger log = LogManager.getLogger(StuckAgentTest.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testStuckEvents() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.qsim().setEndTime(24*3600);

		config.controller().setLastIteration(0);
		// doesn't matter - MultiModalModule sets the mobsim unconditionally. it just can't be something
		// which the ControlerDefaultsModule knows about. Try it, you will get an error. Quite safe.
		config.controller().setMobsim("myMobsim");


		MultiModalConfigGroup multiModalConfigGroup = new MultiModalConfigGroup();
        multiModalConfigGroup.setMultiModalSimulationEnabled(true);
		multiModalConfigGroup.setSimulatedModes("walk,bike,other");
		config.addModule(multiModalConfigGroup);

		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*3600);
		config.scoring().addActivityParams(homeParams);

		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		config.routing().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed);

		// set default bike speed; Parkin and Rotheram according to 6.01 [m/s]
		double defaultBikeSpeed = 6.01;
		config.routing().setTeleportedModeSpeed(TransportMode.bike, defaultBikeSpeed);

		// set unkown mode speed
		double unknownModeSpeed = 2.0;
		config.routing().setTeleportedModeSpeed("other", unknownModeSpeed);

        config.travelTimeCalculator().setFilterModes(true);

		Scenario scenario = ScenarioUtils.createScenario(config);

		Node node0 = scenario.getNetwork().getFactory().createNode(Id.create("n0", Node.class), new Coord(0.0, 0.0));
		Node node1 = scenario.getNetwork().getFactory().createNode(Id.create("n1", Node.class), new Coord(1.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(Id.create("n2", Node.class), new Coord(2.0, 0.0));
		Node node3 = scenario.getNetwork().getFactory().createNode(Id.create("n3", Node.class), new Coord(3.0, 0.0));
		Node node4 = scenario.getNetwork().getFactory().createNode(Id.create("n4", Node.class), new Coord(4.0, 0.0));

		Link link0 = scenario.getNetwork().getFactory().createLink(Id.create("l0", Link.class), node0, node1);
		Link link1 = scenario.getNetwork().getFactory().createLink(Id.create("l1", Link.class), node1, node2);
		Link link2 = scenario.getNetwork().getFactory().createLink(Id.create("l2", Link.class), node2, node3);
		Link link3 = scenario.getNetwork().getFactory().createLink(Id.create("l3", Link.class), node3, node4);

		link0.setLength(10000.0);
		link1.setLength(10000.0);
		link2.setLength(10000.0);
		link3.setLength(10000.0);

		link0.setAllowedModes(CollectionUtils.stringToSet("bike,walk"));
		link1.setAllowedModes(CollectionUtils.stringToSet("bike,walk"));
		link2.setAllowedModes(CollectionUtils.stringToSet("bike,walk"));
		link3.setAllowedModes(CollectionUtils.stringToSet("bike,walk"));

		scenario.getNetwork().addNode(node0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addNode(node3);
		scenario.getNetwork().addNode(node4);
		scenario.getNetwork().addLink(link0);
		scenario.getNetwork().addLink(link1);
		scenario.getNetwork().addLink(link2);
		scenario.getNetwork().addLink(link3);

		RouteFactory routeFactory = new LinkNetworkRouteFactory();
		Route route0 = routeFactory.createRoute(Id.create("l0", Link.class), Id.create("l3", Link.class));	// missing l1 & l2
		Route route1 = routeFactory.createRoute(Id.create("l0", Link.class), Id.create("l3", Link.class));	// missing l2
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(Id.create("l0", Link.class));
		((NetworkRoute) route1).setLinkIds(Id.create("l0", Link.class), linkIds, Id.create("l3", Link.class));

		scenario.getPopulation().addPerson(createPerson(scenario, "p0", "bike", route0, 6.5*3600));	// stuck
		scenario.getPopulation().addPerson(createPerson(scenario, "p1", "walk", route1, 7.5*3600));	// stuck
		scenario.getPopulation().addPerson(createPerson(scenario, "p2", "walk", null, 8.5*3600));	// regular
		scenario.getPopulation().addPerson(createPerson(scenario, "p3", "walk", null, 23.5*3600));	// en-route when simulation ends
		scenario.getPopulation().addPerson(createPerson(scenario, "p4", "walk", null, 24.5*3600));	// departs after simulation has ended

		Controler controler = new Controler(scenario);
        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);

        controler.addOverridingModule(new MultiModalModule());

        EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);
		controler.getEvents().addHandler(new EventsPrinter());

		controler.run();

		int stuckCnt = 0;
		int stuckBeforeSimulationEnd = 0;
		for (Event e : collector.getEvents()) {
			if (e instanceof PersonStuckEvent) {
				stuckCnt++;

				if (e.getTime() < config.qsim().getEndTime().seconds()) stuckBeforeSimulationEnd++;
			}
		}

		Assertions.assertEquals(2, stuckBeforeSimulationEnd);
		Assertions.assertEquals(4, stuckCnt);
	}

	private Person createPerson(Scenario scenario, String id, String mode, Route route, double departureTime) {
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create(id, Person.class));

		PersonUtils.setAge(person, 20);
		PersonUtils.setSex(person, "m");

		Activity from = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("l0", Link.class));
		Leg leg = scenario.getPopulation().getFactory().createLeg(mode);
		leg.setRoute(route);
		Activity to = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("l3", Link.class));

		from.setEndTime(departureTime);
		leg.setDepartureTime(departureTime);

		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);

		person.addPlan(plan);

		return person;
	}

	// for debugging
	private static class EventsPrinter implements BasicEventHandler {

		@Override
		public void reset(final int iter) {
		}

		@Override
		public void handleEvent(final Event event) {
			StringBuilder eventXML = new StringBuilder(180);
			eventXML.append("\t<event ");
			Map<String, String> attr = event.getAttributes();
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				eventXML.append(entry.getKey());
				eventXML.append("=\"");
				eventXML.append(entry.getValue());
				eventXML.append("\" ");
			}
			eventXML.append("/>");

			log.info(eventXML.toString());
		}
	}
}
