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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class LinkToLinkTravelTimeCalculatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * @author mrieser
	 */
	@Test
	void testLongTravelTimeInEmptySlot() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(utils.loadConfig((String)null));
    scenario.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		Network network = (Network) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 2000, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 1000, (double) 1000));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 100.0, 3600.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, 1000.0, 100.0, 3600.0, 1.0 );
		final Node fromNode2 = node2;
		final Node toNode2 = node4;
		Link link3 = NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), fromNode2, toNode2, 1000.0, 100.0, 3600.0, 1.0 );

		int timeBinSize = 15*60;
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 12*3600, scenario.getConfig().travelTimeCalculator());

		Id<Vehicle> vehId1 = Id.create(11, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(12, Vehicle.class);
		Id<Vehicle> vehId3 = Id.create(13, Vehicle.class);

		// generate some events that suggest a really long travel time
		double linkEnterTime1 = Time.parseTime("07:00:10");
		double linkTravelTime1 = 50.0 * 60; // 50 minutes!
		double linkEnterTime2 = Time.parseTime("07:45:10");
		double linkTravelTime2 = 10.0 * 60; // 10 minutes
		double linkTravelTime3 = 16.0 * 60; // 16 minutes

		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1, vehId1, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime1 + linkTravelTime1, vehId1, link1.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime1 + linkTravelTime1, vehId1, link2.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, vehId2, link1.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2, vehId3, link1.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime2, vehId2, link1.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2 + linkTravelTime2, vehId2, link2.getId()));
		ttcalc.handleEvent(new LinkLeaveEvent(linkEnterTime2 + linkTravelTime3, vehId3, link1.getId()));
		ttcalc.handleEvent(new LinkEnterEvent(linkEnterTime2 + linkTravelTime3, vehId3, link3.getId()));

		assertEquals(50 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60, null, null), MatsimTestUtils.EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 1*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 2*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(13 * 60, ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 3*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // (linkTravelTime2+linkTravelTime2b)/2.0 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 4*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkTravelTimes().getLinkTravelTime(link1, 7.0 * 3600 + 5 * 60 + 5*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize

		assertEquals(50 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60, null, null), MatsimTestUtils.EPSILON); // linkTravelTime1
		assertEquals(35 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 1*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // linkTravelTime1 - 1*timeBinSize
		assertEquals(20 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 2*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // linkTravelTime1 - 2*timeBinSize
		assertEquals(10 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 3*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // linkTravelTime2 > linkTravelTime1 - 3*timeBinSize !
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 4*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // freespeedTravelTime > linkTravelTime2 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link2, 7.0 * 3600 + 5 * 60 + 5*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // freespeedTravelTime > linkTravelTime2 - 2*timeBinSize

		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60, null, null), MatsimTestUtils.EPSILON); // freespeed travel time
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 1*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // freespeed
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 2*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // freespeed
		assertEquals(16 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 3*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // linkTravelTime3
		assertEquals( 1 * 60, ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 4*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // linkTravelTime3 - 1*timeBinSize
		assertEquals(10     , ttcalc.getLinkToLinkTravelTimes().getLinkToLinkTravelTime(link1, link3, 7.0 * 3600 + 5 * 60 + 5*timeBinSize, null, null), MatsimTestUtils.EPSILON);  // freespeedTravelTime > linkTravelTime2b - 2*timeBinSize
	}
}
