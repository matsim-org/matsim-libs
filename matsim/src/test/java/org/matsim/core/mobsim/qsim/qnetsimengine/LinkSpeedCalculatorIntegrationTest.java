/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.testcases.utils.EventsLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mrieser / Senozon AG
 */
public class LinkSpeedCalculatorIntegrationTest {

	@Rule public MatsimTestUtils helper = new MatsimTestUtils();
	
	@Test
	public void testIntegration_Default() {
		Fixture f = new Fixture();
		EventsCollector collector = new EventsCollector();
		f.events.addHandler(collector);
		f.events.addHandler(new EventsLogger());

		QSim qsim = configureQSim(f, null);
		qsim.run();
		
		List<Event> events = collector.getEvents();
		Assert.assertTrue(events.get(5) instanceof LinkEnterEvent);
		LinkEnterEvent lee = (LinkEnterEvent) events.get(5);
		Assert.assertEquals("1", lee.getPersonId().toString());
		Assert.assertEquals("2", lee.getLinkId().toString());

		Assert.assertTrue(events.get(6) instanceof LinkLeaveEvent);
		LinkLeaveEvent lle = (LinkLeaveEvent) events.get(6);
		Assert.assertEquals("1", lle.getPersonId().toString());
		Assert.assertEquals("2", lle.getLinkId().toString());
		
		// by default, the link takes 10 seconds to travel along, plus 1 second in the buffer, makes total of 11 seconds
		Assert.assertEquals(11, lle.getTime() - lee.getTime(), 1e-8);
	}
	
	@Test
	public void testIntegration_Slow() {
		Fixture f = new Fixture();
		EventsCollector collector = new EventsCollector();
		f.events.addHandler(collector);
		f.events.addHandler(new EventsLogger());

	// Use a vehicle link speed of 5.0.
		QSim qsim = configureQSim(f, new CustomLinkSpeedCalculator(5.0));
		qsim.run();
		
		List<Event> events = collector.getEvents();
		Assert.assertTrue(events.get(5) instanceof LinkEnterEvent);
		LinkEnterEvent lee = (LinkEnterEvent) events.get(5);
		Assert.assertEquals("1", lee.getPersonId().toString());
		Assert.assertEquals("2", lee.getLinkId().toString());

		Assert.assertTrue(events.get(6) instanceof LinkLeaveEvent);
		LinkLeaveEvent lle = (LinkLeaveEvent) events.get(6);
		Assert.assertEquals("1", lle.getPersonId().toString());
		Assert.assertEquals("2", lle.getLinkId().toString());
		
		// with 5 per second, the link takes 20 seconds to travel along, plus 1 second in the buffer, makes total of 21 seconds
		Assert.assertEquals(21, lle.getTime() - lee.getTime(), 1e-8);
	}
	
	@Test
	public void testIntegration_Fast() {
		Fixture f = new Fixture();
		EventsCollector collector = new EventsCollector();
		f.events.addHandler(collector);
		f.events.addHandler(new EventsLogger());

		QSim qsim = configureQSim(f, new CustomLinkSpeedCalculator(20.0));
		qsim.run();
		
		List<Event> events = collector.getEvents();
		Assert.assertTrue(events.get(5) instanceof LinkEnterEvent);
		LinkEnterEvent lee = (LinkEnterEvent) events.get(5);
		Assert.assertEquals("1", lee.getPersonId().toString());
		Assert.assertEquals("2", lee.getLinkId().toString());

		Assert.assertTrue(events.get(6) instanceof LinkLeaveEvent);
		LinkLeaveEvent lle = (LinkLeaveEvent) events.get(6);
		Assert.assertEquals("1", lle.getPersonId().toString());
		Assert.assertEquals("2", lle.getLinkId().toString());
		
		// the link should take 5 seconds to travel along, plus 1 second in the buffer, makes total of 6 seconds
		Assert.assertEquals(6, lle.getTime() - lee.getTime(), 1e-8);
	}
	
	private QSim configureQSim(Fixture f, LinkSpeedCalculator linkSpeedCalculator) {
		QSim qsim = new QSim(f.scenario, f.events);
		
		// handle activities
		ActivityEngine activityEngine = new ActivityEngine(f.events, qsim.getAgentCounter());
		qsim.addMobsimEngine(activityEngine);
		qsim.addActivityHandler(activityEngine);

        QNetsimEngine netsimEngine = new QNetsimEngine(qsim);
		if (linkSpeedCalculator != null) {
			netsimEngine.setLinkSpeedCalculator(linkSpeedCalculator);
		}
		qsim.addMobsimEngine(netsimEngine);
		qsim.addDepartureHandler(netsimEngine.getDepartureHandler());
		
		PopulationAgentSource agentSource = new PopulationAgentSource(f.scenario.getPopulation(), new DefaultAgentFactory(qsim), qsim);
		qsim.addAgentSource(agentSource);
		
		return qsim;
	}
	
	static class CustomLinkSpeedCalculator implements LinkSpeedCalculator {

		final double maxSpeed;
		
		public CustomLinkSpeedCalculator(final double maxSpeed) {
			this.maxSpeed = maxSpeed;
		}
		
		@Override
		public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
			return this.maxSpeed;
		}
		
	}
	
	/**
	 * Creates a simple network (3 links in a row) and a single person travelling from the first link to the third.
	 * 
	 * @author mrieser / Senozon AG
	 */
	static class Fixture {
		EventsManager events = new EventsManagerImpl();
		Scenario scenario;

		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

			Id<Node>[] nodeIds = new Id[5];
			for (int i = 0; i < nodeIds.length; i++) {
				nodeIds[i] = Id.create(i, Node.class);
			}
			Id<Link>[] linkIds = new Id[4];
			for (int i = 0; i < linkIds.length; i++) {
				linkIds[i] = Id.create(i, Link.class);
			}
	
			/* create Network */
			Network network = this.scenario.getNetwork();
			NetworkFactory nf = network.getFactory();

			Node n1 = nf.createNode(nodeIds[1], this.scenario.createCoord(0, 0));
			Node n2 = nf.createNode(nodeIds[2], this.scenario.createCoord(100, 0));
			Node n3 = nf.createNode(nodeIds[3], this.scenario.createCoord(200, 0));
			Node n4 = nf.createNode(nodeIds[4], this.scenario.createCoord(300, 0));

			network.addNode(n1);
			network.addNode(n2);
			network.addNode(n3);
			network.addNode(n4);

			Link l1 = nf.createLink(linkIds[1], n1, n2);
			Link l2 = nf.createLink(linkIds[2], n2, n3);
			Link l3 = nf.createLink(linkIds[3], n3, n4);
			
			Set<String> modes = new HashSet<String>();
			modes.add("car");
			for (Link l : new Link[] {l1, l2, l3}) {
				l.setLength(100);
				l.setFreespeed(10.0);
				l.setAllowedModes(modes);
				l.setCapacity(1800);
				network.addLink(l);
			}

			/* create Person */
			Population population = this.scenario.getPopulation();
			PopulationFactory pf = population.getFactory();

			Person person = pf.createPerson(Id.create(1, Person.class));
			Plan plan = pf.createPlan();
			Activity homeAct = pf.createActivityFromLinkId("home", linkIds[1]);
			homeAct.setEndTime(7*3600);
			Leg leg = pf.createLeg("car");
			Route route = new LinkNetworkRouteImpl(linkIds[1], new Id[] { linkIds[2] }, linkIds[3]);
			leg.setRoute(route);
			Activity workAct = pf.createActivityFromLinkId("work", linkIds[1]);

			plan.addActivity(homeAct);
			plan.addLeg(leg);
			plan.addActivity(workAct);

			person.addPlan(plan);
			
			population.addPerson(person);
		}
	}

}
