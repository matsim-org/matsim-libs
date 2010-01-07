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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class CalcLegTimesTest extends MatsimTestCase {

	public static final String BASE_FILE_NAME = "tripdurations.txt";
	public static final Id DEFAULT_PERSON_ID = new IdImpl(123);
	public static final Id DEFAULT_LINK_ID = new IdImpl(456);
	
	private PopulationImpl population = null;
	private NetworkLayer network = null;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);
		
		this.population = new PopulationImpl();
		PersonImpl person = new PersonImpl(DEFAULT_PERSON_ID);
		this.population.addPerson(person);
		PlanImpl plan = person.createAndAddPlan(true);
		plan.createAndAddActivity("act1", new CoordImpl(100.0, 100.0));
		plan.createAndAddLeg(TransportMode.undefined);
		plan.createAndAddActivity("act2", new CoordImpl(200.0, 200.0));
		plan.createAndAddLeg(TransportMode.undefined);
		plan.createAndAddActivity("act3", new CoordImpl(200.0, 200.0));
		plan.createAndAddLeg(TransportMode.undefined);
		plan.createAndAddActivity("act4", new CoordImpl(200.0, 200.0));
		plan.createAndAddLeg(TransportMode.undefined);
		plan.createAndAddActivity("act5", new CoordImpl(200.0, 200.0));
		this.network = new NetworkLayer();
		Node fromNode = this.network.createAndAddNode(new IdImpl("123456"), new CoordImpl(100.0, 100.0));
		Node toNode = this.network.createAndAddNode(new IdImpl("789012"), new CoordImpl(200.0, 200.0));
		this.network.createAndAddLink(DEFAULT_LINK_ID, fromNode, toNode, Math.sqrt(20000.0), 13.333, 2000, 1);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.population = null;
		this.network = null;
	}

	public void testNoEvents() {
		
		CalcLegTimes testee = new CalcLegTimes(this.population);
		
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(testee);
		
		// add events to handle here
		
		this.runTest(testee);
	}

	public void testAveraging() {
		
		CalcLegTimes testee = new CalcLegTimes(this.population);
		
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(testee);

		Person defaultPerson = this.population.getPersons().get(DEFAULT_PERSON_ID);

		LegImpl leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setArrivalTime(Time.parseTime("07:30:00"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));

		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:00:00"));
		leg.setArrivalTime(Time.parseTime("07:10:00"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));
		
		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("31:12:00"));
		leg.setArrivalTime(Time.parseTime("31:22:00"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));
		
		leg = new LegImpl(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("30:12:00"));
		leg.setArrivalTime(Time.parseTime("30:12:01"));
		testee.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));
		testee.handleEvent(new AgentArrivalEventImpl(leg.getArrivalTime(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg));
		
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
