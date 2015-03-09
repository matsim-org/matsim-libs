/* *********************************************************************** *
 * project: org.matsim.*
 * CourtesyEventsGenerator.java
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
package playground.thibautd.socnetsim.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.facilities.ActivityFacility;

import playground.ivt.utils.MapUtils;
import playground.thibautd.socnetsim.population.SocialNetwork;

/**
 * Creates "courtesy" events: agents say "hello" and "goodbye" to their
 * social contacts located in the same facility as them.
 * @author thibautd
 */
public class CourtesyEventsGenerator implements ActivityStartEventHandler, ActivityEndEventHandler {

	private final EventsManager events;
	private final SocialNetwork socialNetwork;
	private final Map< Id<ActivityFacility> , Set< Id<Person> > > personsAtFacility = new HashMap< >();

	public CourtesyEventsGenerator(
			final EventsManager events,
			final SocialNetwork socialNetwork) {
		this.events = events;
		this.socialNetwork = socialNetwork;
	}

	@Override
	public void reset(int iteration) {
		personsAtFacility.clear();
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		handleEvent(
				CourtesyEvent.Type.sayHelloEvent,
				event.getPersonId(),
				event.getFacilityId(),
				event.getTime() );
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		handleEvent(
				CourtesyEvent.Type.sayGoodbyeEvent,
				event.getPersonId(),
				event.getFacilityId(),
				event.getTime() );
	}

	private void handleEvent(
			final CourtesyEvent.Type type,
			final Id<Person> ego,
			final Id<ActivityFacility> facility,
			final double time) {
		// TODO: handle wraparound (done improperly because we track people from their second act...)
		final Set< Id<Person> > alters = socialNetwork.getAlters( ego );
		for ( Id<Person> present : MapUtils.getSet( facility , personsAtFacility ) ) {
			if ( alters.contains( present ) ) {
				events.processEvent(
						new CourtesyEvent(
							time,
							ego,
							present,
							type ) );
				events.processEvent(
						new CourtesyEvent(
							time,
							present,
							ego,
							type ) );
			}
		}

		switch ( type ) {
			case sayGoodbyeEvent:
				MapUtils.getSet( facility , personsAtFacility ).remove( ego );
				break;
			case sayHelloEvent:
				MapUtils.getSet( facility , personsAtFacility ).add( ego );
				break;
			default:
				throw new RuntimeException( type+"?" );
		}
	}
}

