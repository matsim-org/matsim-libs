/* *********************************************************************** *
 * project: org.matsim.*
 * ModelRunner.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

/**
 * @author thibautd
 */
public class ModelRunner<T extends Agent> {
	private static final Logger log =
		Logger.getLogger(ModelRunner.class);

	private int randomSeed = 20130226;
	private int stepSize = 1;
	private UtilityFunction<T> utilityFunction = null;
	private ThresholdFunction thresholds = null;

	// /////////////////////////////////////////////////////////////////////////
	// configuration ( setters )
	// /////////////////////////////////////////////////////////////////////////
	public void setRandomSeed(final int randomSeed) {
		this.randomSeed = randomSeed;
	}

	public void setSamplingRate(final int stepSize) {
		if ( stepSize <= 0 ) throw new IllegalArgumentException( stepSize+" is negative" );
		this.stepSize = stepSize;
	}

	public void setUtilityFunction(final UtilityFunction<T> utilityFunction) {
		this.utilityFunction = utilityFunction;
	}

	public void setThresholds(final ThresholdFunction thresholds) {
		this.thresholds = thresholds;
	}

	// /////////////////////////////////////////////////////////////////////////
	// run
	// /////////////////////////////////////////////////////////////////////////
	public SocialNetwork run(final SocialPopulation<T> population) {
		if ( utilityFunction == null || thresholds == null ) {
			throw new IllegalStateException( "utility="+utilityFunction+"; thresholds="+thresholds );
		}
		final Random random = new Random( randomSeed );
		final SocialNetwork network = new SocialNetwork();

		log.info( "create primary ties" );
		fillInPrimaryTies( random , network , population );
		log.info( "create secondary ties" );
		fillInSecondaryTies( random , network , population );

		return network;
	}

	private void fillInPrimaryTies(
			final Random random,
			final SocialNetwork network,
			final SocialPopulation<T> population) {
		final List<T> remainingAgents = new ArrayList<T>( population.getAgents() );

		final Counter counter = new Counter( "consider primary pair # " );
		while ( !remainingAgents.isEmpty() ) {
			Collections.shuffle( remainingAgents , random );
			final T ego = remainingAgents.remove( 0 );
			final int lastAlterToConsider = (int) (remainingAgents.size() / ((double) stepSize));

			if ( lastAlterToConsider == 0 ) continue;

			final List<T> potentialAlters =
				remainingAgents.subList(
						0,
						lastAlterToConsider);

			for ( T alter : potentialAlters ) {
				counter.incCounter();
				final double prob = calcAcceptanceProbability(
						utilityFunction.calcTieUtility( ego , alter ),
						thresholds.getPrimaryTieThreshold() );

				if ( random.nextDouble() < prob ) {
					network.addTie( new Tie( ego.getId() , alter.getId() ) );
				}
			}
		}
		counter.printCounter();
	}

	private void fillInSecondaryTies(
			final Random random,
			final SocialNetwork network,
			final SocialPopulation<T> population) {
		final Map<Id, T> remainingAgents = new HashMap<Id, T>( population.getAgentsMap() );

		if ( stepSize != 1 ) {
			log.warn( "step size "+stepSize+" is not considered for secondary ties" );
		}

		final Counter counter = new Counter( "consider secondary pair # " );
		while ( !remainingAgents.isEmpty() ) {
			final T ego = removeRandomMapping( random , remainingAgents );

			final List<T> potentialAlters =
				getUnknownFriendsOfFriends( ego , network , remainingAgents );

			for ( T alter : potentialAlters ) {
				counter.incCounter();
				final double prob = calcAcceptanceProbability(
						utilityFunction.calcTieUtility( ego , alter ),
						thresholds.getSecondaryTieThreshold() );

				if ( random.nextDouble() < prob ) {
					network.addTie( new Tie( ego.getId() , alter.getId() ) );
				}
			}
		}
		counter.printCounter();
	}

	// package visible for tests
	final static <T extends Agent> T removeRandomMapping(
			final Random random,
			final Map<Id, T> remainingAgents) {
		final List<Id> list = new ArrayList<Id>( remainingAgents.keySet() );
		final int index = random.nextInt( remainingAgents.size() );
		return remainingAgents.remove( list.get( index ) );
	}

	// package visible for tests
	final static <T extends Agent> List<T> getUnknownFriendsOfFriends(
			final T ego,
			final SocialNetwork network,
			final Map<Id, T> remainingAgents) {
		final List<T> unknownFriendsOfFriends = new ArrayList<T>();
		final Set<Id> alters = network.getAlters( ego.getId() );

		for ( Id alter : alters ) {
			final Set<Id> altersOfAlter = network.getAlters( alter );
			
			for ( Id alterOfAlter : altersOfAlter ) {
				// is the ego?
				if ( alterOfAlter.equals( ego.getId() ) ) continue;
				// already a friend?
				if ( alters.contains( alterOfAlter ) ) continue;
				final T friendOfFriend = remainingAgents.get( alterOfAlter );

				// already allocated?
				if ( friendOfFriend == null ) continue;
				unknownFriendsOfFriends.add( friendOfFriend );
			}
		}

		return unknownFriendsOfFriends;
	}

	private static double calcAcceptanceProbability(
			final double utility,
			final double threshold) {
		final double expUtility = Math.exp( utility );
		final double expThreshold = Math.exp( threshold );

		if ( (expThreshold + expUtility) == Double.MAX_VALUE ) throw new RuntimeException( "overflow" );

		return expUtility / (expThreshold + expUtility);
	}

}

