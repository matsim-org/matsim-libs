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
package org.matsim.contrib.socnetsim.framework.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.facilities.ActivityFacility;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;

import com.google.inject.Inject;

/**
 * Creates "courtesy" events: agents say "hello" and "goodbye" to their
 * social contacts located in the same facility as them.
 *
 * This is needed for scoring, because scoring functions cannot listen to the events of social contacts.
 * @author thibautd
 */
public class CourtesyEventsGenerator implements ActivityStartEventHandler, ActivityEndEventHandler {

	private final EventsManager events;
	private final SocialNetwork socialNetwork;
	private final Map< Id<ActivityFacility> , Map<String, Set< Id<Person> > > > personsAtFacility = new HashMap< >();

	@Inject
	public CourtesyEventsGenerator(
			final EventsManager events,
			final Scenario sc) {
		this.events = events;
		this.socialNetwork = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );
	}

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
				event.getActType(),
				event.getPersonId(),
				event.getFacilityId(),
				event.getTime());
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		handleEvent(
				CourtesyEvent.Type.sayGoodbyeEvent,
				event.getActType(),
				event.getPersonId(),
				event.getFacilityId(),
				event.getTime() );
	}

	private void handleEvent(
			final CourtesyEvent.Type type,
			final String actType,
			final Id<Person> ego,
			final Id<ActivityFacility> facility,
			final double time) {
		// TODO: handle wraparound (done improperly because we track people from their second act...)
		final Set< Id<Person> > alters = socialNetwork.getAlters( ego );

		switch ( type ) {
			case sayGoodbyeEvent:
				// avoid problems with wrap-around: do not say goodbye before being tracked.
				// this caused problems with agents having leisure at home.
				// solution would be to track the agents from the start of the simulation
				if ( !getPersonsAtFacilityForType( facility , actType ).remove( ego ) ) return;
				break;
			case sayHelloEvent:
				getPersonsAtFacilityForType( facility , actType ).add( ego );
				break;
			default:
				throw new RuntimeException( type+"?" );
		}

		for ( Id<Person> present : getPersonsAtFacilityForType( facility , actType ) ) {
			if ( alters.contains( present ) ) {
				events.processEvent(
						new CourtesyEvent(
							time,
							actType,
							ego,
							present,
							type ) );
				events.processEvent(
						new CourtesyEvent(
							time,
							actType,
							present,
							ego,
							type ) );
			}
		}
	}

	private Set<Id<Person>> getPersonsAtFacilityForType( Id<ActivityFacility> facility, String actType ) {
		return MapUtils.getSet(
				actType,
				MapUtils.getMap(
						facility,
						personsAtFacility ) );
	}
}

