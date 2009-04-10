/* *********************************************************************** *
 * project: org.matsim.*
 * CalcTripDurationsTest.java
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

package playground.meisterk.org.matsim.analysis;

import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.Events;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

/**
 * Test class for {@link CalctripDurations}.
 * 
 * @author meisterk
 *
 */
public class CalcTripDurationsTest extends MatsimTestCase {

	public static final String BASE_FILE_NAME = "tripdurations.txt";
	
	private Person person = null;
	private NetworkLayer network = null;
	private Link link = null;
	private Node fromNode = null, toNode = null;
	
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(null);
		
		this.person = new PersonImpl(new IdImpl("123"));
		this.network = new NetworkLayer();
		this.fromNode = this.network.createNode(new IdImpl("123456"), new CoordImpl(100.0, 100.0));
		this.toNode = this.network.createNode(new IdImpl("789012"), new CoordImpl(200.0, 200.0));
		this.link = this.network.createLink(new IdImpl("456"), this.fromNode, this.toNode, Math.sqrt(20000.0), 13.333, 2000, 1);
		
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.person = null;
	}

	public void testNoEvents() {
		
		CalcTripDurations testee = new CalcTripDurations();
		
		Events events = new Events();
		events.addHandler(testee);
		
		// add events to handle here
		
		this.runTest(testee);
	}

	public void testSomeModes() {
		
		CalcTripDurations testee = new CalcTripDurations();
		
		Events events = new Events();
		events.addHandler(testee);
		
		Leg leg = new LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setArrivalTime(Time.parseTime("07:30:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), this.person, this.link, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), this.person, this.link, leg));

		leg = new LegImpl(BasicLeg.Mode.pt);
		leg.setDepartureTime(Time.parseTime("12:00:00"));
		leg.setArrivalTime(Time.parseTime("15:45:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), this.person, this.link, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), this.person, this.link, leg));
		
		leg = new LegImpl(BasicLeg.Mode.bike);
		leg.setDepartureTime(Time.parseTime("30:05:00"));
		leg.setArrivalTime(Time.parseTime("30:08:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), this.person, this.link, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), this.person, this.link, leg));
		
		leg = new LegImpl(BasicLeg.Mode.walk);
		leg.setDepartureTime(Time.parseTime("31:00:00"));
		leg.setArrivalTime(Time.parseTime("32:00:00"));
		testee.handleEvent(new AgentDepartureEvent(leg.getDepartureTime(), this.person, this.link, leg));
		testee.handleEvent(new AgentArrivalEvent(leg.getArrivalTime(), this.person, this.link, leg));
		
		this.runTest(testee);
	}

	protected void runTest(CalcTripDurations calcTripDurations) {
		
		calcTripDurations.writeStats(this.getOutputDirectory() + CalcTripDurationsTest.BASE_FILE_NAME);

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + CalcTripDurationsTest.BASE_FILE_NAME);
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + CalcTripDurationsTest.BASE_FILE_NAME);
		assertEquals("Output files differ.", expectedChecksum, actualChecksum);
	}

}
