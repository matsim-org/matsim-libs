/* *********************************************************************** *
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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

public class StuckAgentTest {

	private static final Logger log = Logger.getLogger(StuckAgentTest.class);
	
	@Rule 
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testStuckEvents() {
		Config config = ConfigUtils.createConfig();
		
		config.qsim().setEndTime(24*3600);
		
		config.controler().setLastIteration(0);

        MultiModalConfigGroup multiModalConfigGroup = new MultiModalConfigGroup();
        multiModalConfigGroup.setMultiModalSimulationEnabled(true);
		multiModalConfigGroup.setSimulatedModes("walk,bike,unknown");
		config.addModule(multiModalConfigGroup);

		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*3600);
		config.planCalcScore().addActivityParams(homeParams);
		
		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed);
		
		// set default bike speed; Parkin and Rotheram according to 6.01 [m/s]
		double defaultBikeSpeed = 6.01;
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.bike, defaultBikeSpeed);
		
		// set unkown mode speed
		double unknownModeSpeed = 2.0;
		config.plansCalcRoute().setTeleportedModeSpeed("unknown", unknownModeSpeed);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Node node0 = scenario.getNetwork().getFactory().createNode(scenario.createId("n0"), new CoordImpl(0.0, 0.0));
		Node node1 = scenario.getNetwork().getFactory().createNode(scenario.createId("n1"), new CoordImpl(1.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(scenario.createId("n2"), new CoordImpl(2.0, 0.0));
		Node node3 = scenario.getNetwork().getFactory().createNode(scenario.createId("n3"), new CoordImpl(3.0, 0.0));
		Node node4 = scenario.getNetwork().getFactory().createNode(scenario.createId("n4"), new CoordImpl(4.0, 0.0));
		
		Link link0 = scenario.getNetwork().getFactory().createLink(scenario.createId("l0"), node0, node1);
		Link link1 = scenario.getNetwork().getFactory().createLink(scenario.createId("l1"), node1, node2);
		Link link2 = scenario.getNetwork().getFactory().createLink(scenario.createId("l2"), node2, node3);
		Link link3 = scenario.getNetwork().getFactory().createLink(scenario.createId("l3"), node3, node4);
		
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
		Route route0 = routeFactory.createRoute(scenario.createId("l0"), scenario.createId("l3"));	// missing l1 & l2
		Route route1 = routeFactory.createRoute(scenario.createId("l0"), scenario.createId("l3"));	// missing l2
		List<Id> linkIds = new ArrayList<Id>();
		linkIds.add(scenario.createId("l0"));
		((NetworkRoute) route1).setLinkIds(scenario.createId("l0"), linkIds, scenario.createId("l3"));

		scenario.getPopulation().addPerson(createPerson(scenario, "p0", "bike", route0, 6.5*3600));	// stuck
		scenario.getPopulation().addPerson(createPerson(scenario, "p1", "walk", route1, 7.5*3600));	// stuck
		scenario.getPopulation().addPerson(createPerson(scenario, "p2", "walk", null, 8.5*3600));	// regular
		scenario.getPopulation().addPerson(createPerson(scenario, "p3", "walk", null, 23.5*3600));	// en-route when simulation ends
		scenario.getPopulation().addPerson(createPerson(scenario, "p4", "walk", null, 24.5*3600));	// departs after simulation has ended
		
		Controler controler = new Controler(scenario);
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.setOverwriteFiles(true);
		
		// controler listener that initializes the multi-modal simulation
		MultiModalControlerListener listener = new MultiModalControlerListener();
		controler.addControlerListener(listener);
		
		EventsCollector collector = new EventsCollector();
		controler.getEvents().addHandler(collector);
		controler.getEvents().addHandler(new EventsPrinter());
		
		controler.run();
		
		int stuckCnt = 0;
		int stuckBeforeSimulationEnd = 0;
		for (Event e : collector.getEvents()) {
			if (e instanceof PersonStuckEvent) {
				stuckCnt++;
				
				if (e.getTime() < config.qsim().getEndTime()) stuckBeforeSimulationEnd++;
			}
		}

		Assert.assertEquals(2, stuckBeforeSimulationEnd);
		Assert.assertEquals(4, stuckCnt);
	}
	
	private Person createPerson(Scenario scenario, String id, String mode, Route route, double departureTime) {
		PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(scenario.createId(id));
		
		person.setAge(20);
		person.setSex("m");

		Activity from = scenario.getPopulation().getFactory().createActivityFromLinkId("home", scenario.createId("l0"));
		Leg leg = scenario.getPopulation().getFactory().createLeg(mode);
		leg.setRoute(route);
		Activity to = scenario.getPopulation().getFactory().createActivityFromLinkId("home", scenario.createId("l3"));

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