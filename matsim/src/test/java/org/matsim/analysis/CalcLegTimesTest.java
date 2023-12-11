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

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;


public class CalcLegTimesTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	public static final String BASE_FILE_NAME = "legdurations.txt";
	public final Id<Person> DEFAULT_PERSON_ID = Id.create(123, Person.class);
	public final Id<Link> DEFAULT_LINK_ID = Id.create(456, Link.class);

	private Population population = null;
	private Network network = null;

	@BeforeEach public void setUp() {
		utils.loadConfig((String)null);

		MutableScenario s = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.population = s.getPopulation();
		Person person = PopulationUtils.getFactory().createPerson(DEFAULT_PERSON_ID);
		this.population.addPerson(person);
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		PopulationUtils.createAndAddActivityFromCoord(plan, "act1", new Coord(100.0, 100.0));
		PopulationUtils.createAndAddLeg( plan, "undefined" );
		PopulationUtils.createAndAddActivityFromCoord(plan, "act2", new Coord(200.0, 200.0));
		PopulationUtils.createAndAddLeg( plan, "undefined" );
		PopulationUtils.createAndAddActivityFromCoord(plan, "act3", new Coord(200.0, 200.0));
		PopulationUtils.createAndAddLeg( plan, "undefined" );
		PopulationUtils.createAndAddActivityFromCoord(plan, "act4", new Coord(200.0, 200.0));
		PopulationUtils.createAndAddLeg( plan, "undefined" );
		PopulationUtils.createAndAddActivityFromCoord(plan, "act5", new Coord(200.0, 200.0));
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

	@AfterEach public void tearDown() {
		this.population = null;
		this.network = null;
	}

	@Test
	void testNoEvents() throws IOException {

		CalcLegTimes testee = new CalcLegTimes();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);

		// add events to handle here

		this.runTest(testee);
	}

	@Test
	void testAveraging() throws IOException {

		CalcLegTimes testee = new CalcLegTimes();

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);


		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setTravelTime( Time.parseTime("07:30:00") - leg.getDepartureTime().seconds());
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act1", null));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act2", null));

		leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:00:00"));
		leg.setTravelTime( Time.parseTime("07:10:00") - leg.getDepartureTime().seconds());
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act2", null));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act3", null));

		leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("31:12:00"));
		leg.setTravelTime( Time.parseTime("31:22:00") - leg.getDepartureTime().seconds());
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act3", null));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act4", null));

		leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("30:12:00"));
		leg.setTravelTime( Time.parseTime("30:12:01") - leg.getDepartureTime().seconds());
		testee.handleEvent(new ActivityEndEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act4", null));
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));
		testee.handleEvent(new ActivityStartEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, null, "act5", null));

		this.runTest(testee);
	}

	private void runTest( CalcLegTimes calcLegTimes ) throws IOException {

		calcLegTimes.writeStats(utils.getOutputDirectory() + CalcLegTimesTest.BASE_FILE_NAME);

		Assertions.assertEquals(readResult(utils.getInputDirectory() + CalcLegTimesTest.BASE_FILE_NAME),
				readResult(utils.getOutputDirectory() + CalcLegTimesTest.BASE_FILE_NAME));

	}

	private static String readResult(String filePath) throws IOException {
		BufferedReader br = IOUtils.getBufferedReader(filePath);
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
			sb.append(line);
			sb.append("\n");
			line = br.readLine();
		}

		return sb.toString();
	}

}
