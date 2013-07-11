/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalControlerListenerTest.java
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

package contrib.multimodal;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class MultiModalControlerListenerTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(MultiModalControlerListenerTest.class);
	
	public void testSimpleScenario() {
		log.info("Run test single threaded...");
		runSimpleScenario(1);
		
		log.info("Run test multi threaded...");
		runSimpleScenario(2);
		runSimpleScenario(4);
	}
	
	public void runSimpleScenario(int numberOfThreads) {
		
		Config config = ConfigUtils.createConfig();
		
		config.addQSimConfigGroup(new QSimConfigGroup());
		config.getQSimConfigGroup().setEndTime(24*3600);
		
		config.controler().setLastIteration(0);

        MultiModalConfigGroup multiModalConfigGroup = new MultiModalConfigGroup();
        multiModalConfigGroup.setMultiModalSimulationEnabled(true);
		multiModalConfigGroup.setSimulatedModes("walk,bike,unknown");
		multiModalConfigGroup.setNumberOfThreads(numberOfThreads);
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
		
		Link link0 = scenario.getNetwork().getFactory().createLink(scenario.createId("l0"), node0, node1);
		Link link1 = scenario.getNetwork().getFactory().createLink(scenario.createId("l1"), node1, node2);
		Link link2 = scenario.getNetwork().getFactory().createLink(scenario.createId("l2"), node1, node2);
		Link link3 = scenario.getNetwork().getFactory().createLink(scenario.createId("l3"), node1, node2);
		Link link4 = scenario.getNetwork().getFactory().createLink(scenario.createId("l4"), node1, node2);
		Link link5 = scenario.getNetwork().getFactory().createLink(scenario.createId("l5"), node2, node3);
		
		link0.setLength(1.0);
		link1.setLength(1.0);
		link2.setLength(10.0);
		link3.setLength(100.0);
		link4.setLength(1000.0);
		link5.setLength(1.0);
		
		link0.setAllowedModes(CollectionUtils.stringToSet("car,bike,walk,unknown"));
		link1.setAllowedModes(CollectionUtils.stringToSet("car"));
		link2.setAllowedModes(CollectionUtils.stringToSet("bike"));
		link3.setAllowedModes(CollectionUtils.stringToSet("walk"));
		link4.setAllowedModes(CollectionUtils.stringToSet("unknown"));
		link5.setAllowedModes(CollectionUtils.stringToSet("car,bike,walk,unknown"));
		
		scenario.getNetwork().addNode(node0);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addNode(node3);
		scenario.getNetwork().addLink(link0);
		scenario.getNetwork().addLink(link1);
		scenario.getNetwork().addLink(link2);
		scenario.getNetwork().addLink(link3);
		scenario.getNetwork().addLink(link4);
		scenario.getNetwork().addLink(link5);
				
		scenario.getPopulation().addPerson(createPerson(scenario, "p0", "car"));
		scenario.getPopulation().addPerson(createPerson(scenario, "p1", "bike"));
		scenario.getPopulation().addPerson(createPerson(scenario, "p2", "walk"));
		scenario.getPopulation().addPerson(createPerson(scenario, "p3", "unknown"));
		
		Controler controler = new Controler(scenario);
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.setOverwriteFiles(true);
		
		// controler listener that initializes the multi-modal simulation
		MultiModalControlerListener listener = new MultiModalControlerListener();
		controler.addControlerListener(listener);
		
		LinkModeChecker linkModeChecker = new LinkModeChecker(scenario.getNetwork());
		controler.getEvents().addHandler(linkModeChecker);
		
		controler.run();
		
		// assume that the number of arrival events is correct
		assertEquals(4, linkModeChecker.arrivalCount);
		
		// assume that the number of link left events is correct
		assertEquals(8, linkModeChecker.linkLeftCount);
	}
	
	private Person createPerson(Scenario scenario, String id, String mode) {
		PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(scenario.createId(id));
		
		person.setAge(20);
		person.setSex("m");

		Activity from = scenario.getPopulation().getFactory().createActivityFromLinkId("home", scenario.createId("l0"));
		Leg leg = scenario.getPopulation().getFactory().createLeg(mode);
		Activity to = scenario.getPopulation().getFactory().createActivityFromLinkId("home", scenario.createId("l5"));

		from.setEndTime(8*3600);
		leg.setDepartureTime(8*3600);
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);
		
		person.addPlan(plan);
		
		return person;
	}
	
	private static class LinkModeChecker implements LinkLeaveEventHandler, AgentDepartureEventHandler,
			AgentArrivalEventHandler {

		int arrivalCount = 0;
		int linkLeftCount = 0;
		private final Network network;
		private final Map<Id, String> modes = new HashMap<Id, String>();
		
		public LinkModeChecker(Network network) {
			this.network = network;
		}
		
		@Override
		public void reset(int iteration) {
			// nothing to do here
		}
		
		@Override
		public void handleEvent(AgentDepartureEvent event) {
			this.modes.put(event.getPersonId(), event.getLegMode());
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Link link = this.network.getLinks().get(event.getLinkId());
			
			// assume that the agent is allowed to travel on the link
			assertEquals(true, link.getAllowedModes().contains(this.modes.get(event.getPersonId())));
			
			this.linkLeftCount++;
		}

		@Override
		public void handleEvent(AgentArrivalEvent event) {
			this.arrivalCount++;
		}
	}
}
