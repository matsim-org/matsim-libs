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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.Events;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class CalcLegTimesTest extends MatsimTestCase {

	public static final String BASE_FILE_NAME = "tripdurations.txt";
	public static final Id DEFAULT_PERSON_ID = new IdImpl(123);
	public static final Id DEFAULT_LINK_ID = new IdImpl(456);
	
	private Population population = null;
	private NetworkLayer network = null;
	
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);
		
		this.population = new PopulationImpl();
		Person person = new PersonImpl(DEFAULT_PERSON_ID);
		this.population.addPerson(person);
		Plan plan = person.createPlan(true);
		plan.createActivity("act1", new CoordImpl(100.0, 100.0));
		plan.createLeg(TransportMode.undefined);
		plan.createActivity("act2", new CoordImpl(200.0, 200.0));
		plan.createLeg(TransportMode.undefined);
		plan.createActivity("act3", new CoordImpl(200.0, 200.0));
		plan.createLeg(TransportMode.undefined);
		plan.createActivity("act4", new CoordImpl(200.0, 200.0));
		plan.createLeg(TransportMode.undefined);
		plan.createActivity("act5", new CoordImpl(200.0, 200.0));
		this.network = new NetworkLayer();
		Node fromNode = this.network.createNode(new IdImpl("123456"), new CoordImpl(100.0, 100.0));
		Node toNode = this.network.createNode(new IdImpl("789012"), new CoordImpl(200.0, 200.0));
		this.network.createLink(DEFAULT_LINK_ID, fromNode, toNode, Math.sqrt(20000.0), 13.333, 2000, 1);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.population = null;
		this.network = null;
	}

	public void testNoEvents() {
		
		CalcLegTimes testee = new CalcLegTimes(this.population);
		
		Events events = new Events();
		events.addHandler(testee);
		
		// add events to handle here
		
		this.runTest(testee);
	}

	public void testSomeModes() {
		
		CalcLegTimes testee = new CalcLegTimes(this.population);
		
		Events events = new Events();
		events.addHandler(testee);
		
		Person defaultPerson = this.population.getPersons().get(DEFAULT_PERSON_ID);
		Link defaultLink = this.network.getLinks().get(DEFAULT_LINK_ID);
		
		Leg leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setArrivalTime(Time.parseTime("07:30:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));

		leg = new LegImpl(TransportMode.pt);
		leg.setDepartureTime(Time.parseTime("12:00:00"));
		leg.setArrivalTime(Time.parseTime("15:45:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));
		
		leg = new LegImpl(TransportMode.bike);
		leg.setDepartureTime(Time.parseTime("30:05:00"));
		leg.setArrivalTime(Time.parseTime("30:08:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));
		
		leg = new LegImpl(TransportMode.walk);
		leg.setDepartureTime(Time.parseTime("31:00:00"));
		leg.setArrivalTime(Time.parseTime("32:00:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));
		
		this.runTest(testee);
	}

	public void testAveraging() {
		
		CalcLegTimes testee = new CalcLegTimes(this.population);
		
		Events events = new Events();
		events.addHandler(testee);

		Person defaultPerson = this.population.getPersons().get(DEFAULT_PERSON_ID);
		Link defaultLink = this.network.getLinks().get(DEFAULT_LINK_ID);

		Leg leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setArrivalTime(Time.parseTime("07:30:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));

		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:00:00"));
		leg.setArrivalTime(Time.parseTime("07:10:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));
		
		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("31:12:00"));
		leg.setArrivalTime(Time.parseTime("31:22:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));
		
		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("30:12:00"));
		leg.setArrivalTime(Time.parseTime("30:12:01"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), defaultPerson, defaultLink, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), defaultPerson, defaultLink, leg));
		
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
