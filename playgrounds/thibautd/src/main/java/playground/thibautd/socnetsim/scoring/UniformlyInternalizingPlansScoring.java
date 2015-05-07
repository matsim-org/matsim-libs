/* *********************************************************************** *
 * project: org.matsim.*
 * UniformlyInternalizingPlansScoring.java
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.ivt.utils.MapUtils;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.population.SocialNetwork;

import com.google.inject.Inject;

/**
 * @author thibautd
 */
public class UniformlyInternalizingPlansScoring implements PlansScoring, ScoringListener, IterationStartsListener, IterationEndsListener {
	private static final Logger log =
		Logger.getLogger(UniformlyInternalizingPlansScoring.class);


	private EventsToScore eventsToScore;

	private final String socialNetworkName;

	private final Scenario sc;
	private final EventsManager events;
	private final ScoringFunctionFactory scoringFunctionFactory;

	private final InternalizationRatioCalculator ratioCalculator;

	@Inject
	public UniformlyInternalizingPlansScoring(
			final Scenario sc,
			final EventsManager events,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this( new ConfigBasedInternalizationRatio(
				sc.getConfig() ),
				sc,
				events,
				scoringFunctionFactory );
	}

	public UniformlyInternalizingPlansScoring(
			final InternalizationRatioCalculator ratio,
			final Scenario sc,
			final EventsManager events,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.socialNetworkName = SocialNetwork.ELEMENT_NAME;
		this.ratioCalculator = ratio;
		this.sc = sc ;
		this.events = events ;
		this.scoringFunctionFactory = scoringFunctionFactory ;
	}

	public UniformlyInternalizingPlansScoring(
			final String socialNetworkName,
			final Scenario sc,
			final EventsManager events,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.socialNetworkName = socialNetworkName;
		this.ratioCalculator = 
			new ConfigBasedInternalizationRatio(
					sc.getConfig() );
		this.sc = sc;
		this.events = events;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	public UniformlyInternalizingPlansScoring(
			final String socialNetworkName,
			final InternalizationRatioCalculator ratio,
			final Scenario sc,
			final EventsManager events,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.socialNetworkName = socialNetworkName;
		this.ratioCalculator = ratio;
		this.sc = sc;
		this.events = events;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.eventsToScore = new EventsToScore( this.sc, this.scoringFunctionFactory, this.sc.getConfig().planCalcScore().getLearningRate() );
		this.events.addHandler(this.eventsToScore);
	}

	@Override
	public void notifyScoring(final ScoringEvent event) {
		this.eventsToScore.finish();
		internalizeAltersScores();
	}

	private void internalizeAltersScores() {
		if ( log.isTraceEnabled() ) log.trace( "internalizing alter's scores" );
		final SocialNetwork socialNet = (SocialNetwork)
			sc.getScenarioElement( socialNetworkName );

		// first need to let the scores unmodified, to "internalize" "raw" scores
		final Map<Id, Double> internalizedScores = new HashMap<Id, Double>();

		for ( Person ego : sc.getPopulation().getPersons().values() ) {
			final Collection<? extends Person> alters =
				MapUtils.get(
						socialNet.getAlters( ego.getId() ),
						sc.getPopulation().getPersons() );

			double internalizedScore = getScore( ego );

			if ( log.isTraceEnabled() ) {
				log.trace( "internalizing alter's scores for ego "+ego );
				log.trace( alters.size()+" alters" );
				log.trace( "initial score "+internalizedScore );
			}

			for ( Person alter : alters ) {
				if ( alter == null ) throw new NullPointerException( "not all alters part of the population?" );
				final double ratio = ratioCalculator.getInternalizationRatio( ego.getId() , alter.getId() );
				internalizedScore += ratio * getScore( alter );
			}

			assert !internalizedScores.containsKey( ego.getId() );

			internalizedScores.put(
					ego.getId(), 
					internalizedScore );

			if ( log.isTraceEnabled() ) {
				log.trace( "FINISHED internalizing alter's scores for ego "+ego );
				log.trace( "new score "+internalizedScore );
			}
		}

		for ( Person p : sc.getPopulation().getPersons().values() ) {
			assert internalizedScores.containsKey( p.getId() );
			setScore( p , internalizedScores.remove( p.getId() ) );
		}

		assert internalizedScores.isEmpty();
		if ( log.isTraceEnabled() ) log.trace( "internalizing alter's scores: DONE" );
	}

	private static void setScore(
			final Person p,
			final Double score) {
		if ( score == null ) throw new NullPointerException( "got null score for person "+p );
		if ( score.isNaN() ) throw new RuntimeException( "got NaN score for person "+p );

		p.getSelectedPlan().setScore( score );
	}

	private static double getScore(final Person p) {
		final Double score = p.getSelectedPlan().getScore();

		if ( score == null ) throw new NullPointerException( "got null score for person "+p );
		if ( score.isNaN() ) throw new RuntimeException( "got NaN score for person "+p );

		return score.doubleValue();
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		this.events.removeHandler(this.eventsToScore);
	}

	public static interface InternalizationRatioCalculator {
		public double getInternalizationRatio( Id ego , Id alter );
	}

	public static class ConfigBasedInternalizationRatio implements  InternalizationRatioCalculator {
		private final double ratio;

		public ConfigBasedInternalizationRatio(final Config config) {
			this.ratio = calc( config );
		}

		private static double calc( final Config config ) {
			final GroupReplanningConfigGroup group = (GroupReplanningConfigGroup)
				config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

			if ( group == null ) {
				log.warn( "no "+GroupReplanningConfigGroup.GROUP_NAME+" module found in config" );
				log.warn( "using null internalization ratio as a consequence" );
				return 0;
			}

			assert group != null;
			final double ratio = group.getInternalizationRatio();

			final double epsilon = 1E-9;

			if ( ratio < -epsilon ) {
				log.warn( "internalization ratio "+ratio+" is negative!" );
				log.warn( "agents will try to DECREASE the score of their alters!" );
			}

			if ( Math.abs( ratio ) < epsilon ) {
				log.info( "null internalisation ratio: egoistic agents" );
			}

			if ( Math.abs( ratio - 1 ) < epsilon ) {
				log.info( "unit internalisation ratio: agents try to maximize group average utility" );
			}

			if ( ratio > 1 + epsilon ) {
				log.warn( "internalization ratio "+ratio+" is greater than one!" );
				log.warn( "agents will value the utility of alters higher than their own!" );
			}

			log.info( "setting internalization ratio to "+ratio );
			return ratio;
		}

		@Override
		public double getInternalizationRatio(final Id ego, final Id alter) {
			return ratio;
		}
	}
}

