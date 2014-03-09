/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs.LegHandler;

public class EventsToLegsTest {

	@Test
	public void testCreatesLeg() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.setLegHandler(lh);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, new IdImpl("1"), new IdImpl("l1"), "walk"));
		eventsToLegs.handleEvent(new TeleportationArrivalEvent(30.0, new IdImpl("1"), 50.0));
		eventsToLegs.handleEvent(new PersonArrivalEvent(30.0, new IdImpl("1"), new IdImpl("l2"), "walk"));
		Assert.assertNotNull(lh.handledLeg);
		Assert.assertEquals(10.0, lh.handledLeg.getDepartureTime(), 1e-9);
		Assert.assertEquals(20.0, lh.handledLeg.getTravelTime(), 1e-9);
		Assert.assertEquals(50.0, lh.handledLeg.getRoute().getDistance(), 1e-9);
	}

	@Test
	public void testCreatesLegWithRoute() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.setLegHandler(lh);
		IdImpl agentId = new IdImpl("1");
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, agentId, new IdImpl("l1"), "car"));
		eventsToLegs.handleEvent(new LinkLeaveEvent(10.0, agentId, new IdImpl("l1"), null));
		eventsToLegs.handleEvent(new LinkEnterEvent(11.0, agentId, new IdImpl("l2"), null));
		eventsToLegs.handleEvent(new LinkLeaveEvent(15.0, agentId, new IdImpl("l2"), null));
		eventsToLegs.handleEvent(new LinkEnterEvent(16.0, agentId, new IdImpl("l3"), null));
		eventsToLegs.handleEvent(new PersonArrivalEvent(30.0, agentId, new IdImpl("l3"), "car"));
		Assert.assertNotNull(lh.handledLeg);
		Assert.assertEquals(10.0,lh.handledLeg.getDepartureTime(), 1e-9);
		Assert.assertEquals(20.0,lh.handledLeg.getTravelTime(), 1e-9);
		Assert.assertEquals(20.0,lh.handledLeg.getRoute().getTravelTime(), 1e-9);
		
		// Don't know if it makes sense, but according to specification,
		// the length of a route still does not include first and last link.
		Assert.assertEquals(500.0,lh.handledLeg.getRoute().getDistance(), 1e-9);
	}
	
	private static Scenario createTriangularNetwork() {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		// add nodes
		Node node1 = network.createAndAddNode(new IdImpl("n1"), scenario.createCoord(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("n2"), scenario.createCoord(50, 100));
		Node node3 = network.createAndAddNode(new IdImpl("n3"), scenario.createCoord(50, 0));
		Node node4 = network.createAndAddNode(new IdImpl("n4"), scenario.createCoord(100, 0));

		// add links
		network.createAndAddLink(new IdImpl("l1"), node1, node2, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("l2"), node2, node3, 500.0, 10.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("l3"), node3, node4, 50.0, 0.1, 3600.0, 1);
		network.createAndAddLink(new IdImpl("l4"), node4, node1, 50.0, 0.1, 3600.0, 1);
		
		return scenario;
	}
	
	private static class RememberingLegHandler implements LegHandler {

		/*package*/ Leg handledLeg = null;
		
		@Override
		public void handleLeg(Id agentId, Leg leg) {
			this.handledLeg = leg;
		}
	}

}
