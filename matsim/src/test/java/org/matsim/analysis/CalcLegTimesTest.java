/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesTest.java
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

package org.matsim.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class CalcLegTimesTest extends MatsimTestCase {

	public static final String BASE_FILE_NAME = "tripdurations.txt";
	public static final Id<Person> DEFAULT_PERSON_ID = Id.create(123, Person.class);
	public static final Id<Link> DEFAULT_LINK_ID = Id.create(456, Link.class);

	private Population population = null;
	private Network network = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);

		ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.population = s.getPopulation();
		PersonImpl person = new PersonImpl(DEFAULT_PERSON_ID);
		this.population.addPerson(person);
		PlanImpl plan = person.createAndAddPlan(true);
		plan.createAndAddActivity("act1", new Coord(100.0, 100.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act2", new Coord(200.0, 200.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act3", new Coord(200.0, 200.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act4", new Coord(200.0, 200.0));
		plan.createAndAddLeg("undefined");
		plan.createAndAddActivity("act5", new Coord(200.0, 200.0));
		this.network = s.getNetwork();
		Node fromNode = this.network.getFactory().createNode(Id.create("123456", Node.class), new Coord(100.0, 100.0));
		this.network.addNode(fromNode);
		Node toNode = this.network.getFactory().createNode(Id.create("789012", Node.class), new Coord(200.0, 200.0));
		this.network.addNode(toNode);
		Link link = this.network.getFactory().createLink(DEFAULT_LINK_ID, fromNode, toNode);
		link.setLength(Math.sqrt(20000.0));
		link.setFreespeed(13.333);
		link.setCapacity(2000);
		link.setNumberOfLanes(1);
		this.network.addLink(link);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.population = null;
		this.network = null;
	}

	public void testNoEvents() {

		CalcLegTimes testee = new CalcLegTimes();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);

		// add events to handle here

		this.runTest(testee);
	}

	public void testAveraging() {

		CalcLegTimes testee = new CalcLegTimes();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);


		LegImpl leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setArrivalTime(Time.parseTime("07:30:00"));
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act1"));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act2"));
		
		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:00:00"));
		leg.setArrivalTime(Time.parseTime("07:10:00"));
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act2"));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act3"));

		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("31:12:00"));
		leg.setArrivalTime(Time.parseTime("31:22:00"));
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act3"));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act4"));
		
		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("30:12:00"));
		leg.setArrivalTime(Time.parseTime("30:12:01"));
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act4"));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act5"));

		this.runTest(testee);
	}

	protected void runTest(CalcLegTimes calcLegTimes) {

		calcLegTimes.writeStats(this.getOutputDirectory() + CalcLegTimesTest.BASE_FILE_NAME);

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + CalcLegTimesTest.BASE_FILE_NAME);
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + CalcLegTimesTest.BASE_FILE_NAME);
		assertEquals("Output files differ.", expectedChecksum, actualChecksum);
	}

}
