/* *********************************************************************** *
 * project: org.matsim.*
 * FixedRouteLegTravelTimeEstimatorTest.java
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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.BasicEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.Route;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.misc.Time;

public class FixedRouteLegTravelTimeEstimatorTest extends MatsimTestCase {

	private NetworkLayer network = null;
	private Population population = null;
	
	public static final String TEST_PERSON_ID = "1";
	public static final int TEST_PLAN_NR = 0;
	public static final int TEST_LEG_NR = 0;
	private static final int TIME_BIN_SIZE = 900;
	
	protected Person testPerson = null;
	protected Plan testPlan = null;
	protected Leg testLeg = null;
	protected Act originAct = null;
	protected Act destinationAct = null;
	
	private TravelTimeCalculator linkTravelTimeEstimator = null;
	private DepartureDelayAverageCalculator tDepDelayCalc = null;
	private FixedRouteLegTravelTimeEstimator testee = null;
	
	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	
	private static Logger log = Logger.getLogger(FixedRouteLegTravelTimeEstimatorTest.class);
	
	protected void setUp() throws Exception {

		super.setUp();
		
		super.loadConfig(FixedRouteLegTravelTimeEstimatorTest.CONFIGFILE);

		log.info("Reading network xml file...");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		population = new Population(Population.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		log.info("Reading plans xml file...done.");

		// the estimator is tested on the central route alternative through equil-net
		// first person
		testPerson = population.getPerson(FixedRouteLegTravelTimeEstimatorTest.TEST_PERSON_ID);
		// only plan of that person
		testPlan = testPerson.getPlans().get(FixedRouteLegTravelTimeEstimatorTest.TEST_PLAN_NR);
		// first leg
		ArrayList<Object> actsLegs = testPlan.getActsLegs();
		testLeg = (Leg) actsLegs.get(TEST_LEG_NR + 1);
		// activities before and after leg
		originAct = (Act) actsLegs.get(TEST_LEG_NR);
		destinationAct = (Act) actsLegs.get(TEST_LEG_NR + 2);
		
		tDepDelayCalc = new DepartureDelayAverageCalculator(network, TIME_BIN_SIZE);
		linkTravelTimeEstimator = new TravelTimeCalculator(network, TIME_BIN_SIZE);
		
	}

	public void testGetLegTravelTimeEstimation() {
		
		testee = new FixedRouteLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
		
		double legTravelTimeEstimation = Time.UNDEFINED_TIME;
		
		// this method does not do something useful, therefore the meaningless parameters
		legTravelTimeEstimation = testee.getLegTravelTimeEstimation(new IdImpl("1"), 0.0, null, null, null, "dummy mode");
		
		assertEquals(legTravelTimeEstimation, 0.0);
	}

	public void testProcessDeparture() {
		
		testee = new FixedRouteLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
		Id linkId = originAct.getLinkId();
		
		Events events = new Events();
		events.addHandler(tDepDelayCalc);
		events.printEventHandlers();
		
		// this gives a delay of 36s (1/100th of an hour)
		AgentDepartureEvent depEvent = new AgentDepartureEvent(6.03 * 3600, TEST_PERSON_ID, originAct.getLinkId().toString(), 0);
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(6.04 * 3600, TEST_PERSON_ID, originAct.getLinkId().toString(), 0);
		
		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}
		
		double startTime = 6.00 * 3600;
		double delayEndTime = testee.processDeparture(network.getLink(new IdImpl("1")), startTime);
		assertEquals(delayEndTime, startTime + 36.0);
		
		// let's add another delay of 72s, should result in an average of 54s
		depEvent = new AgentDepartureEvent(6.02 * 3600, TEST_PERSON_ID, linkId.toString(), 0);
		leaveEvent = new LinkLeaveEvent(6.04 * 3600, TEST_PERSON_ID, linkId.toString(), 0);
		
		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		startTime = 6.00 * 3600;
		delayEndTime = testee.processDeparture(network.getLink(linkId), startTime);
		assertEquals(delayEndTime, startTime + (36.0 + 72.0) / 2);

		// the time interval for the previously tested events was for departure times from 6.00 to 6.25
		// for other time intervals, we don't have event information, so estimated delay should be 0s
		
		startTime = 5.9 * 3600;
		delayEndTime = testee.processDeparture(network.getLink(linkId), startTime);
		assertEquals(delayEndTime, startTime);

		startTime = 6.26 * 3600;
		delayEndTime = testee.processDeparture(network.getLink(linkId), 6.26 * 3600);
		assertEquals(delayEndTime, startTime);

	}

	public void testProcessRouteTravelTime() {
		
		testee = new FixedRouteLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
		
		Events events = new Events();
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		Route route = testLeg.getRoute();
		log.info(route.toString());
		
		// generate some travel times
		BasicEvent event = null;
		
		Link[] links = route.getLinkRoute();
		System.out.println(links.length);
		
		String[][] eventTimes = new String[][]{
			new String[]{"06:05:00", "06:07:00", "06:09:00"},
			new String[]{"06:16:00", "06:21:00", "06:26:00"}
		};
		
		for (int eventTimesCnt = 0; eventTimesCnt < eventTimes.length; eventTimesCnt++) {
			for (int linkCnt = 0; linkCnt < links.length; linkCnt++) {
				event = new LinkEnterEvent(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt]), 
						testPerson.getId().toString(), 
						links[linkCnt].getId().toString(), 
						testLeg.getNum());
				events.processEvent(event);
				event = new LinkLeaveEvent(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt + 1]), 
						testPerson.getId().toString(), 
						links[linkCnt].getId().toString(), 
						testLeg.getNum());
				events.processEvent(event);
			}
		}
		
		// test a start time where all link departures will be in the first time bin
		double startTime = Time.parseTime("06:10:00");
		double routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:14:00"), routeEndTime);
		
		// test a start time where all link departures will be in the second time bin
		startTime = Time.parseTime("06:20:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:30:00"), routeEndTime);
		
		// test a start time in the first bin where one link departure is in the first bin, one in the second bin
		startTime = Time.parseTime("06:13:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:20:00"), routeEndTime);
		
		// test a start time in a free speed bin, having second departure in the first bin
		startTime = Time.parseTime("05:59:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(
				testee.processLink(links[1], startTime + network.getLink(links[0].getId()).getFreespeedTravelTime(Time.UNDEFINED_TIME)), 
				routeEndTime);

		// test a start time in the second bin, having second departure in the free speed bin
		startTime = Time.parseTime("06:28:00");
		routeEndTime = testee.processRouteTravelTime(route, startTime);
		assertEquals(
				testee.processLink(links[0], startTime) + network.getLink(links[1].getId()).getFreespeedTravelTime(Time.UNDEFINED_TIME),
				routeEndTime
				);

	}

	public void testProcessLink() {

		testee = new FixedRouteLegTravelTimeEstimator(linkTravelTimeEstimator, tDepDelayCalc);
		Id linkId = testLeg.getRoute().getLinkRoute()[0].getId();

		Events events = new Events();
		events.addHandler(linkTravelTimeEstimator);
		events.printEventHandlers();

		// we have one agent on this link, taking 1 minute and 48 seconds
		LinkEnterEvent enterEvent = new LinkEnterEvent(Time.parseTime("06:05:00"), TEST_PERSON_ID, linkId.toString(), 0);
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(Time.parseTime("06:06:48"), TEST_PERSON_ID, linkId.toString(), 0);

		for (BasicEvent event : new BasicEvent[]{enterEvent, leaveEvent}) {
			events.processEvent(event);
		}

		// for start times inside the time bin, the predicted travel time is always the same
		double startTime = Time.parseTime("06:10:00");
		double linkEndTime = testee.processLink(network.getLink(linkId), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:11:48"));

		startTime = Time.parseTime("06:01:00");
		linkEndTime = testee.processLink(network.getLink(linkId), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:02:48"));
		
		// for start times outside the time bin, the free speed travel time is returned
		double freeSpeedTravelTime = network.getLink(linkId.toString()).getFreespeedTravelTime(Time.UNDEFINED_TIME);
		
		startTime = Time.parseTime("05:59:00");
		linkEndTime = testee.processLink(network.getLink(linkId), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime);
		
		startTime = Time.parseTime("08:12:00");
		linkEndTime = testee.processLink(network.getLink(linkId), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime);
		
	}

	public TravelTimeCalculator getLinkTravelTimeEstimator() {
		return linkTravelTimeEstimator;
	}

	public DepartureDelayAverageCalculator getTDepDelayCalc() {
		return tDepDelayCalc;
	}

	public Population getPopulation() {
		return population;
	}

}
