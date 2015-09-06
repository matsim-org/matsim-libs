/* *********************************************************************** *
 * project: org.matsim.*
 * CostNavigationTravelTimeLoggerTest.java
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

package playground.christoph.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;

public class CostNavigationTravelTimeLoggerTest extends MatsimTestCase {

	public void testTrustCalculation() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create("p1", Person.class));
		scenario.getPopulation().addPerson(person);

		Node node1 = scenario.getNetwork().getFactory().createNode(Id.create("n1", Node.class), new Coord(0.0, 0.0));
		Node node2 = scenario.getNetwork().getFactory().createNode(Id.create("n2", Node.class), new Coord(1.0, 0.0));
		Link link = scenario.getNetwork().getFactory().createLink(Id.create("l1", Link.class), node1, node2);
		scenario.getNetwork().addNode(node1);
		scenario.getNetwork().addNode(node2);
		scenario.getNetwork().addLink(link);
		
		TravelTime travelTime = new DummyTravelTime();
		CostNavigationTravelTimeLogger logger = new CostNavigationTravelTimeLogger(scenario.getPopulation(), scenario.getNetwork(), travelTime);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(logger);
		
		// no link events so far, therefore
		assertEquals(0.5, logger.getTrust(person.getId()));
		assertEquals(1, logger.getFollowedAndAccepted(person.getId()));
		assertEquals(1, logger.getFollowedAndNotAccepted(person.getId()));
		assertEquals(1, logger.getNotFollowedAndAccepted(person.getId()));
		assertEquals(1, logger.getNotFollowedAndNotAccepted(person.getId()));
		
		// accept a link and set a travel time that is accepted
		eventsManager.processEvent(new LinkEnterEvent(0.0, person.getId(), link.getId(), null));
		logger.setFollowed(person.getId(), true);
		eventsManager.processEvent(new LinkLeaveEvent(100.0, person.getId(), link.getId(), null));
		assertEquals(0.6, logger.getTrust(person.getId()));
		assertEquals(2, logger.getFollowedAndAccepted(person.getId()));
		assertEquals(1, logger.getFollowedAndNotAccepted(person.getId()));
		assertEquals(1, logger.getNotFollowedAndAccepted(person.getId()));
		assertEquals(1, logger.getNotFollowedAndNotAccepted(person.getId()));
		
		// decline a link and set a travel time that is accepted
		eventsManager.processEvent(new LinkEnterEvent(100.0, person.getId(), link.getId(), null));
		logger.setFollowed(person.getId(), false);
		logger.setExpectedAlternativeTravelTime(person.getId(), 10.0);
		eventsManager.processEvent(new LinkLeaveEvent(200.0, person.getId(), link.getId(), null));
//		assertEquals(0.6, logger.getTrust(person.getId()));
		assertEquals(2, logger.getFollowedAndAccepted(person.getId()));
		assertEquals(1, logger.getFollowedAndNotAccepted(person.getId()));
		assertEquals(2, logger.getNotFollowedAndAccepted(person.getId()));
		assertEquals(1, logger.getNotFollowedAndNotAccepted(person.getId()));
		
		// decline a link and set a travel time that is not accepted
		eventsManager.processEvent(new LinkEnterEvent(100.0, person.getId(), link.getId(), null));
		logger.setFollowed(person.getId(), false);
		logger.setExpectedAlternativeTravelTime(person.getId(), 100.0);
		eventsManager.processEvent(new LinkLeaveEvent(200.0, person.getId(), link.getId(), null));
//		assertEquals(0.6, logger.getTrust(person.getId()));
		assertEquals(2, logger.getFollowedAndAccepted(person.getId()));
		assertEquals(1, logger.getFollowedAndNotAccepted(person.getId()));
		assertEquals(2, logger.getNotFollowedAndAccepted(person.getId()));
		assertEquals(2, logger.getNotFollowedAndNotAccepted(person.getId()));
	}
	
	private class DummyTravelTime implements TravelTime {

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return 100.0;
		}
		
	}
}
