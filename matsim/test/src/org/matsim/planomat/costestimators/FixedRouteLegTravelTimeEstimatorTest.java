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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.BasicEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.misc.Time;

public class FixedRouteLegTravelTimeEstimatorTest extends MatsimTestCase {

	protected NetworkLayer network = null;
	protected Population population = null;

	protected static final Id TEST_PERSON_ID = new IdImpl("1");
	private static final int TEST_PLAN_NR = 0;
	protected static final int TEST_LEG_NR = 0;
	private static final int TIME_BIN_SIZE = 900;

	protected Person testPerson = null;
	protected Plan testPlan = null;
	protected Leg testLeg = null;
	protected Act originAct = null;
	protected Act destinationAct = null;

	protected TravelTimeCalculator linkTravelTimeEstimator = null;
	protected TravelCost linkTravelCostEstimator = null;
	protected DepartureDelayAverageCalculator tDepDelayCalc = null;
	private FixedRouteLegTravelTimeEstimator testee = null;

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";

	private static final Logger log = Logger.getLogger(FixedRouteLegTravelTimeEstimatorTest.class);

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		Config config = super.loadConfig(CONFIGFILE);

		log.info("Reading network xml file...");
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(config.network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		this.population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(this.population, this.network);
		plansReader.readFile(config.plans().getInputFile());
		this.population.printPlansCount();
		log.info("Reading plans xml file...done.");

		// the estimator is tested on the central route alternative through equil-net
		// first person
		this.testPerson = this.population.getPerson(TEST_PERSON_ID);
		// only plan of that person
		this.testPlan = this.testPerson.getPlans().get(TEST_PLAN_NR);
		// first leg
		ArrayList<Object> actsLegs = this.testPlan.getActsLegs();
		this.testLeg = (Leg) actsLegs.get(TEST_LEG_NR + 1);
		// activities before and after leg
		this.originAct = (Act) actsLegs.get(TEST_LEG_NR);
		this.destinationAct = (Act) actsLegs.get(TEST_LEG_NR + 2);

		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network, TIME_BIN_SIZE);
		this.linkTravelTimeEstimator = new TravelTimeCalculator(this.network, TIME_BIN_SIZE);
		this.linkTravelCostEstimator = new TravelTimeDistanceCostCalculator(this.linkTravelTimeEstimator);

	}

	@Override
	protected void tearDown() throws Exception {
		this.destinationAct = null;
		this.linkTravelTimeEstimator = null;
		this.network = null;
		this.originAct = null;
		this.population = null;
		this.tDepDelayCalc = null;
		this.testee = null;
		this.testLeg = null;
		this.testPerson = null;
		this.testPlan = null;
		super.tearDown();
	}

	public void testGetLegTravelTimeEstimation() {

		Gbl.getConfig().charyparNagelScoring().setMarginalUtlOfDistance(0.0);

		this.testee = new FixedRouteLegTravelTimeEstimator(
				this.linkTravelTimeEstimator,
				this.linkTravelCostEstimator,
				this.tDepDelayCalc,
				this.network);

		double legTravelTimeEstimation = this.testee.getLegTravelTimeEstimation(
				new IdImpl("1"),
				0.0,
				this.originAct,
				this.destinationAct,
				this.testLeg);

		assertEquals(539.0, legTravelTimeEstimation, EPSILON);
	}

	public void testProcessDeparture() {

		this.testee = new FixedRouteLegTravelTimeEstimator(
				this.linkTravelTimeEstimator,
				this.linkTravelCostEstimator,
				this.tDepDelayCalc,
				this.network);
		Id linkId = this.originAct.getLinkId();

		Events events = new Events();
		events.addHandler(this.tDepDelayCalc);
		events.printEventHandlers();

		// this gives a delay of 36s (1/100th of an hour)
		AgentDepartureEvent depEvent = new AgentDepartureEvent(6.03 * 3600, TEST_PERSON_ID.toString(), this.originAct.getLinkId().toString());
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(6.04 * 3600, TEST_PERSON_ID.toString(), this.originAct.getLinkId().toString());

		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		double startTime = 6.00 * 3600;
		double delayEndTime = this.testee.processDeparture(this.network.getLink(new IdImpl("1")), startTime);
		assertEquals(delayEndTime, startTime + 36.0, EPSILON);

		// let's add another delay of 72s, should result in an average of 54s
		depEvent = new AgentDepartureEvent(6.02 * 3600, TEST_PERSON_ID.toString(), linkId.toString());
		leaveEvent = new LinkLeaveEvent(6.04 * 3600, TEST_PERSON_ID.toString(), linkId.toString());

		for (BasicEvent event : new BasicEvent[]{depEvent, leaveEvent}) {
			events.processEvent(event);
		}

		startTime = 6.00 * 3600;
		delayEndTime = this.testee.processDeparture(this.network.getLink(linkId), startTime);
		assertEquals(delayEndTime, startTime + (36.0 + 72.0) / 2, EPSILON);

		// the time interval for the previously tested events was for departure times from 6.00 to 6.25
		// for other time intervals, we don't have event information, so estimated delay should be 0s

		startTime = 5.9 * 3600;
		delayEndTime = this.testee.processDeparture(this.network.getLink(linkId), startTime);
		assertEquals(delayEndTime, startTime, EPSILON);

		startTime = 6.26 * 3600;
		delayEndTime = this.testee.processDeparture(this.network.getLink(linkId), 6.26 * 3600);
		assertEquals(delayEndTime, startTime, EPSILON);

	}

	public void testProcessRouteTravelTime() {

		this.testee = new FixedRouteLegTravelTimeEstimator(
				this.linkTravelTimeEstimator,
				this.linkTravelCostEstimator,
				this.tDepDelayCalc,
				this.network);

		Events events = new Events();
		events.addHandler(this.linkTravelTimeEstimator);
		events.printEventHandlers();

		CarRoute route = (CarRoute) this.testLeg.getRoute();
		log.info(route.toString());

		// generate some travel times
		BasicEvent event = null;

		List<Link> links = route.getLinks();
		System.out.println(links.size());

		String[][] eventTimes = new String[][]{
			new String[]{"06:05:00", "06:07:00", "06:09:00"},
			new String[]{"06:16:00", "06:21:00", "06:26:00"}
		};

		for (int eventTimesCnt = 0; eventTimesCnt < eventTimes.length; eventTimesCnt++) {
			for (int linkCnt = 0; linkCnt < links.size(); linkCnt++) {
				event = new LinkEnterEvent(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt]),
						this.testPerson.getId().toString(),
						links.get(linkCnt).getId().toString());
				events.processEvent(event);
				event = new LinkLeaveEvent(
						Time.parseTime(eventTimes[eventTimesCnt][linkCnt + 1]),
						this.testPerson.getId().toString(),
						links.get(linkCnt).getId().toString());
				events.processEvent(event);
			}
		}

		// test a start time where all link departures will be in the first time bin
		double startTime = Time.parseTime("06:10:00");
		double routeEndTime = this.testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:14:00"), routeEndTime, EPSILON);

		// test a start time where all link departures will be in the second time bin
		startTime = Time.parseTime("06:20:00");
		routeEndTime = this.testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:30:00"), routeEndTime, EPSILON);

		// test a start time in the first bin where one link departure is in the first bin, one in the second bin
		startTime = Time.parseTime("06:13:00");
		routeEndTime = this.testee.processRouteTravelTime(route, startTime);
		assertEquals(Time.parseTime("06:20:00"), routeEndTime, EPSILON);

		// test a start time in a free speed bin, having second departure in the first bin
		startTime = Time.parseTime("05:59:00");
		routeEndTime = this.testee.processRouteTravelTime(route, startTime);
		assertEquals(
				this.testee.processLink(links.get(1), startTime + this.network.getLink(links.get(0).getId()).getFreespeedTravelTime(Time.UNDEFINED_TIME)),
				routeEndTime, EPSILON);

		// test a start time in the second bin, having second departure in the free speed bin
		startTime = Time.parseTime("06:28:00");
		routeEndTime = this.testee.processRouteTravelTime(route, startTime);
		assertEquals(
				this.testee.processLink(links.get(0), startTime) + this.network.getLink(links.get(1).getId()).getFreespeedTravelTime(Time.UNDEFINED_TIME),
				routeEndTime, EPSILON);

	}

	public void testProcessLink() {

		this.testee = new FixedRouteLegTravelTimeEstimator(
				this.linkTravelTimeEstimator,
				this.linkTravelCostEstimator,
				this.tDepDelayCalc,
				this.network);
		Id linkId = ((CarRoute) this.testLeg.getRoute()).getLinks().get(0).getId();

		Events events = new Events();
		events.addHandler(this.linkTravelTimeEstimator);
		events.printEventHandlers();

		// we have one agent on this link, taking 1 minute and 48 seconds
		LinkEnterEvent enterEvent = new LinkEnterEvent(Time.parseTime("06:05:00"), TEST_PERSON_ID.toString(), linkId.toString());
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(Time.parseTime("06:06:48"), TEST_PERSON_ID.toString(), linkId.toString());

		for (BasicEvent event : new BasicEvent[]{enterEvent, leaveEvent}) {
			events.processEvent(event);
		}

		// for start times inside the time bin, the predicted travel time is always the same
		double startTime = Time.parseTime("06:10:00");
		double linkEndTime = this.testee.processLink(this.network.getLink(linkId), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:11:48"), EPSILON);

		startTime = Time.parseTime("06:01:00");
		linkEndTime = this.testee.processLink(this.network.getLink(linkId), startTime);
		assertEquals(linkEndTime, Time.parseTime("06:02:48"), EPSILON);

		// for start times outside the time bin, the free speed travel time is returned
		double freeSpeedTravelTime = this.network.getLink(linkId.toString()).getFreespeedTravelTime(Time.UNDEFINED_TIME);

		startTime = Time.parseTime("05:59:00");
		linkEndTime = this.testee.processLink(this.network.getLink(linkId), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime, EPSILON);

		startTime = Time.parseTime("08:12:00");
		linkEndTime = this.testee.processLink(this.network.getLink(linkId), startTime);
		assertEquals(startTime + freeSpeedTravelTime, linkEndTime, EPSILON);

	}

	public TravelTimeCalculator getLinkTravelTimeEstimator() {
		return this.linkTravelTimeEstimator;
	}

	public DepartureDelayAverageCalculator getTDepDelayCalc() {
		return this.tDepDelayCalc;
	}

	public Population getPopulation() {
		return this.population;
	}

}
