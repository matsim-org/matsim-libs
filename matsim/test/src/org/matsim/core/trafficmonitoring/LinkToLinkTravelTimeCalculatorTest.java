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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class LinkToLinkTravelTimeCalculatorTest extends MatsimTestCase {

	/**
	 * @author mrieser 
	 */
	public void testLongTravelTimeInEmptySlot() {
		ScenarioImpl scenario = new ScenarioImpl(loadConfig(null));
    scenario.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		NetworkLayer network = (NetworkLayer) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		NodeImpl node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		NodeImpl node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createNode(new IdImpl(3), new CoordImpl(2000, 0));
		NodeImpl node4 = network.createNode(new IdImpl(4), new CoordImpl(1000, 1000));
		LinkImpl link1 = network.createLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		LinkImpl link2 = network.createLink(new IdImpl(2), node2, node3, 1000.0, 100.0, 3600.0, 1.0);
		LinkImpl link3 = network.createLink(new IdImpl(3), node2, node4, 1000.0, 100.0, 3600.0, 1.0);

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		PersonImpl person1 = new PersonImpl(new IdImpl(1)); // person 1 travels link1 + link2
		PersonImpl person2 = new PersonImpl(new IdImpl(2)); // person 2 travels link1 + link2
		PersonImpl person3 = new PersonImpl(new IdImpl(3)); // person 3 travels link1 + link3
		
		// generate some events that suggest a really long travel time
		double linkEnterTime1 = Time.parseTime("07:00:10");
		double linkTravelTime1 = 50.0 * 60; // 50 minutes!
		double linkEnterTime2 = Time.parseTime("07:45:10");
		double linkTravelTime2 = 10.0 * 60; // 10 minutes
		double linkTravelTime3 = 16.0 * 60; // 16 minutes
		
		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime1, person1, link1));
		ttcalc.handleEvent(new LinkLeaveEventImpl(linkEnterTime1 + linkTravelTime1, person1, link1));
		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime1 + linkTravelTime1, person1, link2));
		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime2, person2, link1));
		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime2, person3, link1));
		ttcalc.handleEvent(new LinkLeaveEventImpl(linkEnterTime2 + linkTravelTime2, person2, link1));
		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime2 + linkTravelTime2, person2, link2));
		ttcalc.handleEvent(new LinkLeaveEventImpl(linkEnterTime2 + linkTravelTime3, person3, link1));
		ttcalc.handleEvent(new LinkEnterEventImpl(linkEnterTime2 + linkTravelTime3, person3, link3));

		assertEquals(50 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(13 * 60, ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // (linkTravelTime2+linkTravelTime2b)/2.0 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize
		
		assertEquals(50 * 60, ttcalc.getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60), EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(10 * 60, ttcalc.getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime2 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize

		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60), EPSILON); // freespeed travel time
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 1*timeBinSize), EPSILON);  // freespeed
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 2*timeBinSize), EPSILON);  // freespeed
		assertEquals(16 * 60, ttcalc.getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 3*timeBinSize), EPSILON);  // linkTravelTime3
		assertEquals( 1 * 60, ttcalc.getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 4*timeBinSize), EPSILON);  // linkTravelTime3 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 5*timeBinSize), EPSILON);  // freespeedTravelTime > linkTravelTime2b - 2*timeBinSize
	}
}
