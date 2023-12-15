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

package org.matsim.contrib.analysis.kai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.analysis.kai.KNAnalysisEventsHandler.StatType;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

public class KNAnalysisEventsHandlerTest {


    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	// yy this test is probably not doing anything with respect to some of the newer statistics, such as money. kai, mar'14

	public static final String BASE_FILE_NAME = "stats_";
	public final Id<Person> DEFAULT_PERSON_ID = Id.create(123, Person.class);
	public final Id<Link> DEFAULT_LINK_ID = Id.create(456, Link.class);

	private Scenario scenario = null ;
	private Population population = null ;
	private Network network = null ;

    @BeforeEach
	public void setUp() throws Exception {

		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		this.population = scenario.getPopulation();
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
		plan.setScore(12.);

		this.network = scenario.getNetwork();
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

	@AfterEach
	public void tearDown() throws Exception {
		this.population = null;
		this.network = null;
	}

	@Test
	@Disabled
	void testNoEvents() {

		KNAnalysisEventsHandler testee = new KNAnalysisEventsHandler(this.scenario);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);

		// add events to handle here

		this.runTest(testee);
	}

	@Test
	@Disabled
	void testAveraging() {
		// yy this test is probably not doing anything with respect to some of the newer statistics, such as money. kai, mar'14

		KNAnalysisEventsHandler testee = new KNAnalysisEventsHandler(this.scenario);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(testee);

		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:10:00"));
		leg.setTravelTime( Time.parseTime("07:30:00") - leg.getDepartureTime().seconds());
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("07:00:00"));
		leg.setTravelTime( Time.parseTime("07:10:00") - leg.getDepartureTime().seconds());
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("31:12:00"));
		leg.setTravelTime( Time.parseTime("31:22:00") - leg.getDepartureTime().seconds());
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setDepartureTime(Time.parseTime("30:12:00"));
		leg.setTravelTime( Time.parseTime("30:12:01") - leg.getDepartureTime().seconds());
		testee.handleEvent(new PersonDepartureEvent(leg.getDepartureTime().seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode(), leg.getMode()));
		testee.handleEvent(new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
				.seconds(), DEFAULT_PERSON_ID, DEFAULT_LINK_ID, leg.getMode()));

		this.runTest(testee);
	}

	protected void runTest(KNAnalysisEventsHandler calcLegTimes) {

		calcLegTimes.writeStats(utils.getOutputDirectory() + KNAnalysisEventsHandlerTest.BASE_FILE_NAME);

		// actual test: compare checksums of the files
		for ( StatType type : StatType.values() ) {
			final String str = KNAnalysisEventsHandlerTest.BASE_FILE_NAME + type.toString() + ".txt" ;
			LogManager.getLogger(this.getClass()).info( "comparing " + str );
			final long expectedChecksum = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + str);
			final long actualChecksum = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + str);
			Assertions.assertEquals(expectedChecksum, actualChecksum, "Output files differ.");
		}
	}

}
