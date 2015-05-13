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
package playground.thibautd.socnetsim.framework.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
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

import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.population.SocialNetworkImpl;

/**
 * @author thibautd
 */
public class CourtesyEventsTest {
	private static final Logger log =
		Logger.getLogger(CourtesyEventsTest.class);

	// TODo test all overlaps possible (none, A arrives B departs, A arrives and departs)
	@Test
	public void testEvents() {
		final EventsManager events = EventsUtils.createEventsManager();

		final Id<Person> id1 = Id.create( "tintin" , Person.class );
		final Id<Person> id2 = Id.create( "milou" , Person.class );

		final Id<Link> link = Id.createLinkId( "Link" );
		final Id<ActivityFacility> facility = Id.create( "Facility" , ActivityFacility.class );

		final SocialNetwork sn = new SocialNetworkImpl();
		sn.addEgo( id1 );
		sn.addEgo( id2 );
		sn.addBidirectionalTie( id1 , id2 );

		events.addHandler(
				new CourtesyEventsGenerator(
					events,
					sn ) );
		final List<CourtesyEvent> collected = new ArrayList< >();
		events.addHandler(
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

		// 1:|------------------|
		// 2:      |-------|
		events.processEvent(
				new ActivityStartEvent(
					0,
					id1,
					link,
					facility,
					"type" ) );
		events.processEvent(
				new ActivityStartEvent(
					2,
					id2,
					link,
					facility,
					"type" ) );
		events.processEvent(
				new ActivityEndEvent(
					3,
					id2,
					link,
					facility,
					"type" ) );
		events.processEvent(
				new ActivityEndEvent(
					4,
					id1,
					link,
					facility,
					"type" ) );

		Assert.assertEquals(
				"wrong number of events in "+collected,
				4,
				collected.size() );
		// TODO: more thorough
	}

}

