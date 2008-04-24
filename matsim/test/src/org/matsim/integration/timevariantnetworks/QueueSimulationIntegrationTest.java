/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantNetwork_QueueSimulation_IntegrationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.integration.timevariantnetworks;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.Route;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;

/**
 * Tests that the QueueSimulation takes a TimeVariant Network into account.
 *
 * @author mrieser
 */
public class QueueSimulationIntegrationTest extends MatsimTestCase {

	public void testFreespeed() throws Exception {
		loadConfig(null);

		// create a network
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);
		Gbl.getWorld().setNetworkLayer(network);

		// the network has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		network.createNode("3", "200", "0", null);
		network.createNode("4", "300", "0", null);
		Link link1 = network.createLink("1", "1", "2", "100", "10", "3600", "1", null, null);
		TimeVariantLinkImpl link2 = (TimeVariantLinkImpl)network.createLink("2", "2", "3", "100", "10", "3600", "1", null, null);
		Link link3 = network.createLink("3", "3", "4", "100", "10", "3600", "1", null, null);

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		link2.applyEvent(change);

		// create a population
		Plans plans = new Plans(Plans.NO_STREAMING);

		// create a person that travels before the network change happens
		Person person1 = new Person(new IdImpl(1), "m", 40, "yes", "always", "yes");
		plans.addPerson(person1);
		Plan plan1 = person1.createPlan(null, "yes");
		plan1.createAct("h", 0, 0, link1, 0.0, 7*3600.0, 7*3600.0, false);
		Leg leg1 = plan1.createLeg(0, "car", 7*3600.0, 10, 7*3600.0 + 10);
		Route route = new Route();
		route.setRoute("2 3");
		leg1.setRoute(route);
		plan1.createAct("w", 300, 0, link3, 7*3600.0+10, Time.UNDEFINED_TIME, Time.UNDEFINED_TIME, true);

		// create a person that travels after the network change happens
		Person person2 = new Person(new IdImpl(2), "f", 40, "yes", "always", "yes");
		plans.addPerson(person2);
		Plan plan2 = person2.createPlan(null, "yes");
		plan2.createAct("h", 0, 0, link1, 0.0, 9*3600.0, 9*3600.0, false);
		Leg leg2 = plan2.createLeg(0, "car", 9*3600.0, 10, 9*3600.0 + 10);
		Route route2 = new Route();
		route2.setRoute("2 3");
		leg2.setRoute(route2);
		plan2.createAct("w", 300, 0, link3, 9*3600.0+10, Time.UNDEFINED_TIME, Time.UNDEFINED_TIME, true);

		// run the simulation with the timevariant network and the two persons
		Events events = new Events();
		TestTravelTimeCalculator ttcalc = new TestTravelTimeCalculator(person1, person2, link2);
		events.addHandler(ttcalc);
		QueueSimulation qsim = new QueueSimulation(network, plans, events);
		qsim.run();

		// check that we get the expected result
		assertEquals("Person 1 should travel for 11 seconds.", 10.0 + 1.0, ttcalc.person1leaveTime - ttcalc.person1enterTime, EPSILON);
		assertEquals("Person 2 should travel for 6 seconds.", 5.0 + 1.0, ttcalc.person2leaveTime - ttcalc.person2enterTime, EPSILON);
	}

	/**
	 * A special EventHandler to get the link enter and link leave time for two persons on one specific link.
	 *
	 * @author mrieser
	 */
	private static class TestTravelTimeCalculator implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI {

		private final Person person1;
		private final Person person2;
		private final Link link;
		public double person1enterTime = Time.UNDEFINED_TIME;
		public double person1leaveTime = Time.UNDEFINED_TIME;
		public double person2enterTime = Time.UNDEFINED_TIME;
		public double person2leaveTime = Time.UNDEFINED_TIME;

		public TestTravelTimeCalculator(final Person person1, final Person person2, final Link link) {
			this.person1 = person1;
			this.person2 = person2;
			this.link = link;
		}

		public void handleEvent(final EventLinkEnter event) {
			if (event.link != this.link) {
				return;
			}
			if (event.agent == this.person1) {
				this.person1enterTime = event.time;
			} else if (event.agent == this.person2) {
				this.person2enterTime = event.time;
			}
		}

		public void handleEvent(final EventLinkLeave event) {
			if (event.link != this.link) {
				return;
			}
			if (event.agent == this.person1) {
				this.person1leaveTime = event.time;
			} else if (event.agent == this.person2) {
				this.person2leaveTime = event.time;
			}
		}

		public void reset(final int iteration) {
			// nothing to do
		}

	}
}
