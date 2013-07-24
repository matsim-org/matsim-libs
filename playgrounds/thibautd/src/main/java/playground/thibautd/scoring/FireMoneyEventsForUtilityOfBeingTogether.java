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
package playground.thibautd.scoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.utils.MapUtils;

/**
 * @author thibautd
 */
public class FireMoneyEventsForUtilityOfBeingTogether implements
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler,
		AfterMobsimListener {
	private final double marginalUtilityOfTime;
	private final double marginalUtilityOfMoney;
	private final SocialNetwork socialNetwork;
	private final Map<Id, BeingTogetherScoring> scorings = new HashMap<Id, BeingTogetherScoring>();

	private final EventsManager events;

	public FireMoneyEventsForUtilityOfBeingTogether(
			final EventsManager events,
			final double marginalUtilityOfTime,
			final double marginalUtilityOfMoney,
			final SocialNetwork socialNetwork) {
		this.events = events;
		this.marginalUtilityOfTime = marginalUtilityOfTime;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
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
	public void handleEvent(final AgentArrivalEvent event) {
		transmitEventToRelevantPersons( event );
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		transmitEventToRelevantPersons( event );
	}

	private <T extends Event & HasPersonId> void transmitEventToRelevantPersons( final T event ) {
		final Id ego = event.getPersonId();
		for ( Id id : cat( ego , socialNetwork.getAlters( ego ) ) ) {
			final Id finalId = id;
			final BeingTogetherScoring scoring =
				MapUtils.getArbitraryObject(
						id,
						scorings,
						new MapUtils.Factory<BeingTogetherScoring>() {
							@Override
							public BeingTogetherScoring create() {
								return new BeingTogetherScoring(
										marginalUtilityOfTime,
										finalId,
										socialNetwork.getAlters( finalId ) );
							}
						});
			scoring.handleEvent( event );
		}
	}

	private Iterable<Id> cat(final Id ego, final Set<Id> alters) {
		final Set<Id> ids = new HashSet<Id>( alters );
		ids.add( ego );
		return ids;
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		for ( Map.Entry<Id, BeingTogetherScoring> e : scorings.entrySet() ) {
			final Id id = e.getKey();
			final BeingTogetherScoring scoring = e.getValue();

			scoring.finish();
			events.processEvent(
					events.getFactory().createAgentMoneyEvent(
						Time.MIDNIGHT,
						id,
						scoring.getScore() / marginalUtilityOfMoney ) );
		}
	}

}

