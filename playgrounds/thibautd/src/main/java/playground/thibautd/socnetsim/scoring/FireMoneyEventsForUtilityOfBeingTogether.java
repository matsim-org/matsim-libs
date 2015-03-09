/* *********************************************************************** *
 * project: org.matsim.*
 * FireMoneyEventsForUtilityOfBeingTogether.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.scoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;

import playground.ivt.utils.MapUtils;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.scoring.BeingTogetherScoring.Filter;
import playground.thibautd.socnetsim.scoring.BeingTogetherScoring.PersonOverlapScorer;
import playground.thibautd.utils.GenericFactory;

/**
 * @author thibautd
 */
public class FireMoneyEventsForUtilityOfBeingTogether implements
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler,
		AfterMobsimListener {

	private final double marginalUtilityOfMoney;

	private final Filter actTypeFilter;
	private final Filter modeFilter;

	private final SocialNetwork socialNetwork;
	private final ActivityFacilities facilities;
	private final Map<Id, BeingTogetherScoring> scorings = new HashMap<Id, BeingTogetherScoring>();

	private final GenericFactory<PersonOverlapScorer, Id> scorerFactory;

	private final EventsManager events;

	public FireMoneyEventsForUtilityOfBeingTogether(
			final EventsManager events,
			final Filter actTypeFilter,
			final Filter modeFilter,
			final GenericFactory<PersonOverlapScorer, Id> scorerFactory,
			final double marginalUtilityOfMoney,
			final ActivityFacilities facilities,
			final SocialNetwork socialNetwork) {
		this.actTypeFilter = actTypeFilter;
		this.modeFilter = modeFilter;
		this.events = events;
		this.scorerFactory = scorerFactory;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.facilities = facilities;
		this.socialNetwork = socialNetwork;
	}

	@Override
	public void reset(final int iteration) {
		scorings.clear();
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		transmitEventToRelevantPersons( event );
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		transmitEventToRelevantPersons( event );
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		transmitEventToRelevantPersons( event );
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		transmitEventToRelevantPersons(  event );
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		transmitEventToRelevantPersons( event );
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		transmitEventToRelevantPersons( event );
	}

	private <T extends Event & HasPersonId> void transmitEventToRelevantPersons( final T event ) {
		final Id ego = event.getPersonId();
		if ( !socialNetwork.getEgos().contains( ego ) ) return;
		for ( Id<Person> id : cat( ego , socialNetwork.getAlters( ego ) ) ) {
			final Id finalId = id;
			final BeingTogetherScoring scoring =
				MapUtils.getArbitraryObject(
						id,
						scorings,
						new MapUtils.Factory<BeingTogetherScoring>() {
							@Override
							public BeingTogetherScoring create() {
								return new BeingTogetherScoring(
										facilities,
										actTypeFilter,
										modeFilter,
										scorerFactory.create( finalId ),
										finalId,
										socialNetwork.getAlters( finalId ) );
							}
						});
			scoring.handleEvent( event );
		}
	}

	private Iterable<Id<Person>> cat(final Id ego, final Set<Id<Person>> alters) {
		final Set<Id<Person>> ids = new HashSet<>( alters );
		ids.add( ego );
		return ids;
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		for ( Map.Entry<Id, BeingTogetherScoring> e : scorings.entrySet() ) {
			final Id id = e.getKey();
			final BeingTogetherScoring scoring = e.getValue();

			events.processEvent(
					new PersonMoneyEvent(Time.MIDNIGHT, id, scoring.getScore() / marginalUtilityOfMoney) );
		}
	}
}

