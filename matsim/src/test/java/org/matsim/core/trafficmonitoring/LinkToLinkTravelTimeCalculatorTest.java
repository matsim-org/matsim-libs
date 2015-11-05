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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;

public class LinkToLinkTravelTimeCalculatorTest extends MatsimTestCase {

	/**
	 * @author mrieser 
	 */
	public void testLongTravelTimeInEmptySlot() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(loadConfig(null));
    scenario.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 2000, (double) 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 1000, (double) 1000));
		Link link1 = network.createAndAddLink(Id.create(1, Link.class), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(Id.create(2, Link.class), node2, node3, 1000.0, 100.0, 3600.0, 1.0);
		Link link3 = network.createAndAddLink(Id.create(3, Link.class), node2, node4, 1000.0, 100.0, 3600.0, 1.0);

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		Id<Person> agId1 = Id.create(1, Person.class); // person 1 travels link1 + link2
		Id<Person> agId2 = Id.create(2, Person.class); // person 2 travels link1 + link2
		Id<Person> agId3 = Id.create(3, Person.class); // person 3 travels link1 + link3
		Id<Vehicle> vehId1 = Id.create(11, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(12, Vehicle.class);
		Id<Vehicle> vehId3 = Id.create(13, Vehicle.class);		
		
		// generate some events that suggest a really long travel time
		double linkEnterTime1 = Time.parseTime("07:00:10");
		double linkTravelTime1 = 50.0 * 60; // 50 minutes!
		double linkEnterTime2 = Time.parseTime("07:45:10");
		double linkTravelTime2 = 10.0 * 60; // 10 minutes
		double linkTravelTime3 = 16.0 * 60; // 16 minutes
		
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1, agId1, link1.getId(), vehId1));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime1 + linkTravelTime1, agId1, link1.getId(), vehId1));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1 + linkTravelTime1, agId1, link2.getId(), vehId1));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, agId2, link1.getId(), vehId2));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, agId3, link1.getId(), vehId3));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime2, agId2, link1.getId(), vehId2));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2 + linkTravelTime2, agId2, link2.getId(), vehId2));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime3, agId3, link1.getId(), vehId3));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2 + linkTravelTime3, agId3, link3.getId(), vehId3));

		assertEquals(50 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60, null, null), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 1*timeBinSize, null, null), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 2*timeBinSize, null, null), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(13 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 3*timeBinSize, null, null), EPSILON);  // (linkTravelTime2+linkTravelTime2b)/2.0 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 4*timeBinSize, null, null), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 5*timeBinSize, null, null), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize
		
		assertEquals(50 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(10 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime2 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize

		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60), EPSILON); // freespeed travel time
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // freespeed
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // freespeed
		assertEquals(16 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime3
		assertEquals( 1 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // linkTravelTime3 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2b - 2*timeBinSize
	}
}
