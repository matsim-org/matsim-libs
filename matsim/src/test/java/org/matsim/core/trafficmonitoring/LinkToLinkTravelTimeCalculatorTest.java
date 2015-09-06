/* *********************************************************************** *
 * project: org.matsim.*
 * LinkToLinkTravelTimeCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.trafficmonitoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class LinkToLinkTravelTimeCalculatorTest extends MatsimTestCase {

	/**
	 * @author mrieser 
	 */
	public void testLongTravelTimeInEmptySlot() {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(loadConfig(null));
    scenario.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new CoordImpl(1000, 0));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new CoordImpl(2000, 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new CoordImpl(1000, 1000));
		Link link1 = network.createAndAddLink(Id.create(1, Link.class), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(Id.create(2, Link.class), node2, node3, 1000.0, 100.0, 3600.0, 1.0);
		Link link3 = network.createAndAddLink(Id.create(3, Link.class), node2, node4, 1000.0, 100.0, 3600.0, 1.0);

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		Person person1 = PersonImpl.createPerson(Id.create(1, Person.class)); // person 1 travels link1 + link2
		Person person2 = PersonImpl.createPerson(Id.create(2, Person.class)); // person 2 travels link1 + link2
		Person person3 = PersonImpl.createPerson(Id.create(3, Person.class)); // person 3 travels link1 + link3
		
		// generate some events that suggest a really long travel time
		double linkEnterTime1 = Time.parseTime("07:00:10");
		double linkTravelTime1 = 50.0 * 60; // 50 minutes!
		double linkEnterTime2 = Time.parseTime("07:45:10");
		double linkTravelTime2 = 10.0 * 60; // 10 minutes
		double linkTravelTime3 = 16.0 * 60; // 16 minutes
		
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1, person1.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime1 + linkTravelTime1, person1.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1 + linkTravelTime1, person1.getId(), link2.getId(), null));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, person2.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, person3.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime2, person2.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2 + linkTravelTime2, person2.getId(), link2.getId(), null));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime3, person3.getId(), link1.getId(), null));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2 + linkTravelTime3, person3.getId(), link3.getId(), null));

		assertEquals(50 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60, null, null), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 1*timeBinSize, null, null), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 2*timeBinSize, null, null), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(13 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 3*timeBinSize, null, null), EPSILON);  // (linkTravelTime2+linkTravelTime2b)/2.0 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 4*timeBinSize, null, null), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 5*timeBinSize, null, null), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize
		
		assertEquals(50 * 60, ttcalc.getLinkToLinkTravelTime(link1.getId(), link2.getId(), 7.0 * 3600 + 5 * 60), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkToLinkTravelTime(link1.getId(), link2.getId(), 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkToLinkTravelTime(link1.getId(), link2.getId(), 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(10 * 60, ttcalc.getLinkToLinkTravelTime(link1.getId(), link2.getId(), 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime2 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1.getId(), link2.getId(), 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1.getId(), link2.getId(), 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize

		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1.getId(), link3.getId(), 7.0 * 3600 + 5 * 60), EPSILON); // freespeed travel time
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1.getId(), link3.getId(), 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // freespeed
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1.getId(), link3.getId(), 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // freespeed
		assertEquals(16 * 60, ttcalc.getLinkToLinkTravelTime(link1.getId(), link3.getId(), 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime3
		assertEquals( 1 * 60, ttcalc.getLinkToLinkTravelTime(link1.getId(), link3.getId(), 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // linkTravelTime3 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1.getId(), link3.getId(), 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2b - 2*timeBinSize
	}
}
