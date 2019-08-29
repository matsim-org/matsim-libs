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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.vehicles.Vehicle;

public class EventsToLegsTest {

	@Test
	public void testCreatesLeg() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, Id.create("1", Person.class), Id.create("l1", Link.class), "walk"));
		eventsToLegs.handleEvent(new TeleportationArrivalEvent(30.0, Id.create("1", Person.class), 50.0));
		eventsToLegs.handleEvent(new PersonArrivalEvent(30.0, Id.create("1", Person.class), Id.create("l2", Link.class), "walk"));
		Assert.assertNotNull(lh.handledLeg);
		Assert.assertEquals(10.0, lh.handledLeg.getLeg().getDepartureTime(), 1e-9);
		Assert.assertEquals(20.0, lh.handledLeg.getLeg().getTravelTime(), 1e-9);
		Assert.assertEquals(50.0, lh.handledLeg.getLeg().getRoute().getDistance(), 1e-9);
	}

	@Test
	public void testCreatesLegWithRoute() {
		Scenario scenario = createTriangularNetwork();
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		RememberingLegHandler lh = new RememberingLegHandler();
		eventsToLegs.addLegHandler(lh);
		Id<Person> agentId = Id.create("1", Person.class);
		Id<Vehicle> vehId = Id.create("veh1", Vehicle.class);
		eventsToLegs.handleEvent(new PersonDepartureEvent(10.0, agentId, Id.createLinkId("l1"), "car"));
		eventsToLegs.handleEvent(new PersonEntersVehicleEvent(10.0, agentId, vehId));
		eventsToLegs.handleEvent(new VehicleEntersTrafficEvent(10.0, agentId, Id.createLinkId("l1"), vehId, "car", 1.0));
		eventsToLegs.handleEvent(new LinkEnterEvent(11.0, vehId, Id.createLinkId("l2")));
		eventsToLegs.handleEvent(new LinkEnterEvent(16.0, vehId, Id.createLinkId("l3")));
		eventsToLegs.handleEvent(new VehicleLeavesTrafficEvent(30.0, agentId, Id.createLinkId("l3"), vehId, "car", 1.0));
		eventsToLegs.handleEvent(new PersonArrivalEvent(30.0, agentId, Id.createLinkId("l3"), "car"));
		Assert.assertNotNull(lh.handledLeg);
		Assert.assertEquals(10.0,lh.handledLeg.getLeg().getDepartureTime(), 1e-9);
		Assert.assertEquals(20.0,lh.handledLeg.getLeg().getTravelTime(), 1e-9);
		Assert.assertEquals(20.0,lh.handledLeg.getLeg().getRoute().getTravelTime(), 1e-9);
		Assert.assertEquals(550.0,lh.handledLeg.getLeg().getRoute().getDistance(), 1e-9);
	}
	
	private static Scenario createTriangularNetwork() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Network network = scenario.getNetwork();
		
		// add nodes
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("n1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("n2", Node.class), new Coord((double) 50, (double) 100));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("n3", Node.class), new Coord((double) 50, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("n4", Node.class), new Coord((double) 100, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;

		// add links
		NetworkUtils.createAndAddLink(network,Id.create("l1", Link.class), fromNode, toNode, 500.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("l2", Link.class), fromNode1, toNode1, 500.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("l3", Link.class), fromNode2, toNode2, 50.0, 0.1, 3600.0, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node1;
		NetworkUtils.createAndAddLink(network,Id.create("l4", Link.class), fromNode3, toNode3, 50.0, 0.1, 3600.0, (double) 1 );
		
		return scenario;
	}
	
	private static class RememberingLegHandler implements LegHandler {

		/*package*/ PersonExperiencedLeg handledLeg = null;
		
		@Override
		public void handleLeg(PersonExperiencedLeg leg) {
			this.handledLeg = leg;
		}
	}

}
