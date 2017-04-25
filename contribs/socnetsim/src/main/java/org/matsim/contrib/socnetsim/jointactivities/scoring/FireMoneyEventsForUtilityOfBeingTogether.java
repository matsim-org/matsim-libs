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
package org.matsim.contrib.socnetsim.jointactivities.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.scoring.BeingTogetherScoring;
import org.matsim.contrib.socnetsim.run.ScoringFunctionConfigGroup;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
public class FireMoneyEventsForUtilityOfBeingTogether implements
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler,
		AfterMobsimListener {

	private final double marginalUtilityOfMoney;

	private final BeingTogetherScoring.Filter actTypeFilter;
	private final BeingTogetherScoring.Filter modeFilter;

	private final SocialNetwork socialNetwork;
	private final ActivityFacilities facilities;
	private final Map<Id, BeingTogetherScoring> scorings = new HashMap<Id, BeingTogetherScoring>();

	private final OverlapScorerFactory scorerFactory;

	private final EventsManager events;

	@Inject
	public FireMoneyEventsForUtilityOfBeingTogether(
			final EventsManager events,
			final Scenario sc ) {
		this( events,
				(ScoringFunctionConfigGroup) sc.getConfig().getModule( ScoringFunctionConfigGroup.GROUP_NAME ),
				sc );
	}

	public FireMoneyEventsForUtilityOfBeingTogether(
			final EventsManager events,
			final ScoringFunctionConfigGroup module,
			final Scenario sc ) {
		this( events,
				module.getActTypeFilterForJointScoring(),
				module.getModeFilterForJointScoring(),
				getPersonOverlapScorerFactory( sc ),
				sc.getConfig().planCalcScore().getMarginalUtilityOfMoney(),
				sc.getActivityFacilities(),
				(SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME ) );

	}

	public static OverlapScorerFactory getPersonOverlapScorerFactory(
			final Scenario scenario ) {
		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
			scenario.getConfig().getModule( ScoringFunctionConfigGroup.GROUP_NAME );
		switch ( scoringFunctionConf.getTogetherScoringForm() ) {
			case linear:
				return new OverlapScorerFactory() {
						@Override
						public BeingTogetherScoring.PersonOverlapScorer createScorer(final Id id) {
							return new BeingTogetherScoring.LinearOverlapScorer(
									scoringFunctionConf.getMarginalUtilityOfBeingTogether_s() );
						}
					};
			case logarithmic:
				return new OverlapScorerFactory() {
						@Override
						public BeingTogetherScoring.PersonOverlapScorer createScorer(final Id id) {
							final Person person = (Person) scenario.getPopulation().getPersons().get( id );
							if ( person == null ) {
								// eg transit agent
								return new BeingTogetherScoring.LinearOverlapScorer( 0 );
							}
							final double typicalDuration =
								getTypicalDuration( 
										scenario,
										person,
										scoringFunctionConf.getActivityTypeForContactInDesires() );
							final double zeroDuration = typicalDuration * Math.exp( -10.0 / typicalDuration );
							return new BeingTogetherScoring.LogOverlapScorer(
									scoringFunctionConf.getMarginalUtilityOfBeingTogether_s(),
									typicalDuration,
									zeroDuration);
						}
					};
			default:
				throw new RuntimeException( ""+scoringFunctionConf.getTogetherScoringForm() );
		}
	}

	public static double getTypicalDuration(
			final Scenario scenario,
			final Person person,
			final String type ) {
		final Double typicalDuration =
					(Double) scenario.getPopulation().getPersonAttributes().getAttribute(
						person.getId().toString(),
						"typicalDuration_"+type );

		if ( typicalDuration != null ) return typicalDuration.doubleValue();

		final ActivityParams params = scenario.getConfig().planCalcScore().getActivityParams( type );
		
		if ( params == null ) {
			//throw new RuntimeException( "could not find typical duration for Person "+person.getId()+" for type "+type );
			// not that nice, but needed for agents that might not have a preference. BeingTogetherScoring knows how to handle that
			return Double.NEGATIVE_INFINITY;
		}
		
		return params.getTypicalDuration();
	}

	public FireMoneyEventsForUtilityOfBeingTogether(
			final EventsManager events,
			final BeingTogetherScoring.Filter actTypeFilter,
			final BeingTogetherScoring.Filter modeFilter,
			final OverlapScorerFactory scorerFactory,
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
		final Id<Person> ego = event.getPersonId();
		if ( !socialNetwork.getEgos().contains( ego ) ) return;
		final Iterable<Id<Person>> ids = cat( ego , socialNetwork.getAlters( ego ) );
		for ( Id<Person> id : ids ) {
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
										scorerFactory.createScorer(finalId),
										finalId,
										socialNetwork.getAlters( finalId ) );
							}
						});
			scoring.handleEvent( event );
		}
	}

	public Iterable<Id<Person>> cat(final Id<Person> ego, final Set<Id<Person>> alters) {
		final Collection<Id<Person>> ids = new HashSet<>( alters );
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

	public interface OverlapScorerFactory {
		BeingTogetherScoring.PersonOverlapScorer createScorer(Id id);
	}
}

