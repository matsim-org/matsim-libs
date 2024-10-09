/* *********************************************************************** *
 * project: org.matsim.*
 * CourtesyEventsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.facilities.ActivityFacility;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;

/**
 * @author thibautd
 */
public class CourtesyEventsTest {
	private static final Logger log =
		LogManager.getLogger(CourtesyEventsTest.class);
	public final Id<Person> ID_1 = Id.create("tintin", Person.class);
	public final Id<Person> ID_2 = Id.create("milou", Person.class);
	public final Id<Link> LINK_ID = Id.createLinkId("Link");
	public final Id<ActivityFacility> FACILITY_ID = Id.create("Facility", ActivityFacility.class);
	public static final String TYPE = "type";

	@Test
	void testFullOverlap() {

		testEvents( 4,
				// 1:|------------------|
				// 2:      |-------|
				new ActivityStartEvent(
						0,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityStartEvent(
						2,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						3,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						4,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE) );
	}

	@Test
	void testPartialOverlap() {
		testEvents( 4,
				// 1:|------------------|
				// 2:      |-------------------|
				new ActivityStartEvent(
						0,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityStartEvent(
						2,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						3,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						4,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE) );
	}

	@Test
	void testNoOverlap() {
		testEvents( 0,
				// 1:|-----|
				// 2:            |------------|
				new ActivityStartEvent(
						0,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						2,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityStartEvent(
						3,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						4,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE) );
	}

	@Test
	void testStartTogether() {
		testEvents( 4,
				// 1:|-----|
				// 2:|------------------------|
				new ActivityStartEvent(
						0,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityStartEvent(
						0,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						2,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						4,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE) );
	}

	@Test
	void testEndTogether() {
		testEvents( 4,
				// 1:|------------------------|
				// 2:            |------------|
				new ActivityStartEvent(
						0,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityStartEvent(
						1,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						2,
						ID_1,
						LINK_ID,
						FACILITY_ID,
						TYPE),
				new ActivityEndEvent(
						2,
						ID_2,
						LINK_ID,
						FACILITY_ID,
						TYPE) );
	}

	public void testEvents( int expectedCourtesy, Event... events ) {
		final EventsManager eventManager = EventsUtils.createEventsManager();

		final SocialNetwork sn = new SocialNetworkImpl();
		sn.addEgo(ID_1);
		sn.addEgo(ID_2);
		sn.addBidirectionalTie(ID_1, ID_2);

		eventManager.addHandler(
				new CourtesyEventsGenerator(
					eventManager,
					sn ) );
		final List<CourtesyEvent> collected = new ArrayList< >();
		eventManager.addHandler(
				 new BasicEventHandler() {
						@Override
						public void reset( int iteration ) {}

						@Override
						public void handleEvent( Event event ) {
							if ( event instanceof CourtesyEvent ) {
								log.info( "got CourtesyEvent "+event );
								collected.add( (CourtesyEvent) event );
							}
						}
				});

		eventManager.initProcessing();

		for ( Event e : events ) eventManager.processEvent( e );

		eventManager.finishProcessing();

		Assertions.assertEquals(
				expectedCourtesy,
				collected.size(),
				"wrong number of events in "+collected );
	}
}

