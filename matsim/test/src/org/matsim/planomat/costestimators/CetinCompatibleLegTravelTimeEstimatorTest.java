/* *********************************************************************** *
 * project: org.matsim.*
 * CetinCompatibleLegTravelTimeEstimatorTest.java
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

package org.matsim.planomat.costestimators;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.BasicEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.Events;
import org.matsim.network.Link;
import org.matsim.population.Route;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.misc.Time;

public class CetinCompatibleLegTravelTimeEstimatorTest extends FixedRouteLegTravelTimeEstimatorTest {

	private CetinCompatibleLegTravelTimeEstimator testee = null;

//	private static Logger log = Logger.getLogger(CetinCompatibleLegTravelTimeEstimatorTest.class);

	protected void setUp() throws Exception {

		super.setUp();

	}

	public void testGetLegTravelTimeEstimation() {

		DepartureDelayAverageCalculator tDepDelayCalc = super.getTDepDelayCalc();
		TravelTimeCalculator linkTravelTimeEstimator = super.getLinkTravelTimeEstimator();
		
		testee = new CetinCompatibleLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);

		Events events = new Events();
		events.addHandler(tDepDelayCalc);
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		Route route = testLeg.getRoute();
		Link[] links = route.getLinkRoute();

		// let's test a route without events first
		// should result in free speed travel time, without departure delay
		double departureTime = Time.parseTime("06:03:00");
		double legTravelTime = testee.getLegTravelTimeEstimation(
				testPerson.getId(), 
				departureTime, 
				originAct.getLink(), 
				destinationAct.getLink(), 
				route, 
				testLeg.getMode());

		double expectedLegEndTime = departureTime;
		for (Link link : links) {
			expectedLegEndTime += link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
		}
		expectedLegEndTime += destinationAct.getLink().getFreespeedTravelTime(Time.UNDEFINED_TIME);
		assertEquals(expectedLegEndTime, departureTime + legTravelTime);

		// next, a departure delay of 5s at the origin link is added
		departureTime = Time.parseTime("06:05:00");
		double depDelay = Time.parseTime("00:00:05");
		AgentDepartureEvent depEvent = new AgentDepartureEvent(
				departureTime, 
				TEST_PERSON_ID, 
				TEST_LEG_NR, 
				originAct.getLink().getId().toString());
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(departureTime + depDelay, testPerson.getId().toString(), 0, originAct.getLink().getId().toString());

		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		legTravelTime = testee.getLegTravelTimeEstimation(
				new IdImpl(TEST_PERSON_ID), 
				departureTime, 
				originAct.getLink(), 
				destinationAct.getLink(), 
				route, 
				testLeg.getMode());

		expectedLegEndTime = departureTime;
		expectedLegEndTime += depDelay;
		for (Link link : links) {
			expectedLegEndTime += link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
		}
		expectedLegEndTime += destinationAct.getLink().getFreespeedTravelTime(Time.UNDEFINED_TIME);
		assertEquals(expectedLegEndTime, departureTime + legTravelTime);

		// now let's add some travel events
		String[][] eventTimes = new String[][]{
				new String[]{"06:05:00", "06:07:00", "06:09:00"},
				new String[]{"06:16:00", "06:21:00", "06:26:00"}
		};

		BasicEvent event = null;
		for (int eventTimesCnt = 0; eventTimesCnt < eventTimes.length; eventTimesCnt++) {
			for (int linkCnt = 0; linkCnt < links.length; linkCnt++) {
				event = new LinkEnterEnter(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt]), 
						TEST_PERSON_ID, 
						testLeg.getNum(), 
						links[linkCnt].getId().toString());
				events.processEvent(event);
				event = new LinkLeaveEvent(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt + 1]), 
						TEST_PERSON_ID, 
						testLeg.getNum(), 
						links[linkCnt].getId().toString());
				events.processEvent(event);
			}
		}

		// test a start time where all link departures will be in the first time bin
		departureTime = Time.parseTime("06:10:00");
		legTravelTime = testee.getLegTravelTimeEstimation(
				new IdImpl(TEST_PERSON_ID), 
				departureTime, 
				originAct.getLink(), 
				destinationAct.getLink(), 
				route, 
				testLeg.getMode());
		expectedLegEndTime = departureTime;
		expectedLegEndTime += depDelay;
		expectedLegEndTime = testee.processRouteTravelTime(route, expectedLegEndTime);
		expectedLegEndTime = testee.processLink(destinationAct.getLink(), expectedLegEndTime);
		
		assertEquals(expectedLegEndTime, departureTime + legTravelTime);

	}

}
