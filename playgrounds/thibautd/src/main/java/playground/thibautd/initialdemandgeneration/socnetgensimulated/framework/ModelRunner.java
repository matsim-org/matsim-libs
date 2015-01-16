/* *********************************************************************** *
 * project: org.matsim.*
 * ModelRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.Agent;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SocialPopulation;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkImpl;

/**
 * @author thibautd
 */
public class ModelRunner<T extends Agent> {
	private static final Logger log =
		Logger.getLogger(ModelRunner.class);

	private final SocialPopulation<T> population;
	private final TieUtility<T> utility;

	private final long randomSeed = 20150113;

	private final double primarySampleRate;
	private final double secondarySampleRate;
	private final int nThreads;

	public ModelRunner(
			final SocialPopulation<T> population , TieUtility<T> utility ,
			final double primarySampleRate ,
			final double secondarySampleRate ,
			final int nThreads ) {
		this.population = population;
		this.utility = utility;
		this.primarySampleRate = primarySampleRate;
		this.secondarySampleRate = secondarySampleRate;
		this.nThreads = nThreads;
	}

	public SocialNetwork runModel(
			final Thresholds thresholds ) {
		final SocialNetwork sn = runPrimary( thresholds );

		// could be iterated until no new friendship found,
		// but would need to make shure that tested sub-sample is
		// stable accross those "iterations"
		runSecondary( thresholds, sn );

		return sn;
	}

	private SocialNetwork runPrimary(
			final Thresholds thresholds ) {
		final Map<Id, Set<Id>> sn = new ConcurrentHashMap< >();
		log.info( "create primary ties using sampling rate "+primarySampleRate );

		for ( T agent : population.getAgents() ) sn.put( agent.getId() , new HashSet<Id>() );

		// TODO threads
		final Counter counter = new Counter( "consider primary pair # " );

		final List<T> agents = new ArrayList< >( population.getAgents() );
		final List<Thread> threads = new ArrayList< >();

		for ( int i=0; i < nThreads; i++ ) {
			final int threadNumber = i;
			final int startThreadAgents = i * agents.size() / nThreads;
			final int endThreadAgents = i == nThreads ? agents.size() : (i + 1) * agents.size() / nThreads;

			final Thread t = new Thread( new Runnable() {
				@Override
				public void run() {
					final Random random = new Random( randomSeed + threadNumber );
					for ( int agentIndex = startThreadAgents; agentIndex < endThreadAgents; agentIndex++ ) {
						final T ego = agents.get( agentIndex );

						final List<T> potentialAlters = new ArrayList< >( agents.subList( agentIndex + 1 , agents.size() ) );
						int nAltersToConsider = (int) Math.ceil( primarySampleRate * potentialAlters.size() );

						while ( nAltersToConsider-- > 0 && !potentialAlters.isEmpty() ) {
							counter.incCounter();
							final T alter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );

							if ( utility.getTieUtility( ego , alter ) > thresholds.getPrimaryThreshold() ) {
								sn.get( ego.getId() ).add( alter.getId() );
							}
						}
					}
				}
			} );
			threads.add( t );
			t.start();
		}

		try {
			for ( Thread t : threads ) t.join();
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException( e );
		}
		counter.printCounter();

		log.info( "fill in network with primary ties" );
		final SocialNetwork net = new SocialNetworkImpl( true );
		for ( T agent : population.getAgents() ) net.addEgo( agent.getId() );
		for ( Map.Entry<Id, Set<Id>> e : sn.entrySet() ) {
			for ( Id alter : e.getValue() ) {
				net.addBidirectionalTie( e.getKey() , alter );
			}
		}
		return net;
	}


	private void runSecondary(
			final Thresholds thresholds,
			final SocialNetwork sn ) {
		log.info( "create secondary ties" );
		final List<T> agents = new ArrayList< >( population.getAgents() );

		final Map<Id<Person>, Set<Id<Person>>> newTies = new ConcurrentHashMap< >();
		for ( T agent : population.getAgents() ) newTies.put( agent.getId() , new HashSet<Id<Person>>() );

		final Counter counter = new Counter( "consider secondary pair # " );
		final List<Thread> threads = new ArrayList< >();

		for ( int i=0; i < nThreads; i++ ) {
			final int startThreadAgents = i * agents.size() / nThreads;
			final int endThreadAgents = i == nThreads ? agents.size() : (i + 1) * agents.size() / nThreads;

			// always consider only "upper-right" half of contingency matrix
			// (because symetric)
			final Set<Id<Person>> allowedAlters = new HashSet< >();
			for ( int ai = startThreadAgents; ai < agents.size(); ai++ ) {
				allowedAlters.add( agents.get( ai ).getId() );
			}

			final Random random = new Random( randomSeed + 20140107 + i );
			final Thread t = new Thread( new Runnable() {
				@Override
				public void run() {
					for ( int agentIndex = startThreadAgents; agentIndex < endThreadAgents; agentIndex++ ) {
						final T ego = agents.get( agentIndex );
						allowedAlters.remove( ego.getId() );

						final List<T> potentialAlters =
							getUnknownFriendsOfFriends(
								ego,
								sn,
								allowedAlters );

						for ( int remainingChecks = (int) Math.ceil( secondarySampleRate * potentialAlters.size() );
								remainingChecks > 0 && !potentialAlters.isEmpty();
								remainingChecks--) {
							counter.incCounter();
							final T alter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );

							if ( utility.getTieUtility( ego , alter ) > thresholds.getSecondaryThreshold() ) {
								newTies.get( ego.getId() ).add( alter.getId() );
							}
						}
					}
				}
			} );
			threads.add( t );
			t.start();
		}

		try {
			for ( Thread t : threads ) t.join();
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException( e );
		}

		counter.printCounter();

		log.info( "fill in social network with secondary ties" );
		for ( Map.Entry<Id<Person>, Set<Id<Person>>> e : newTies.entrySet() ) {
			for ( Id<Person> alter : e.getValue() ) {
				sn.addBidirectionalTie( e.getKey() , alter );
			}
		}
	}

	private List<T> getUnknownFriendsOfFriends(
			final T ego,
			final SocialNetwork network,
			final Set<Id<Person>> allowedAlters ) {
		final Set<Id<Person>> alters = network.getAlters( ego.getId() );

		final Set<Id<Person>> unknownAltersOfAlters = new TreeSet< >();
		for ( Id alter : alters ) {
			final Set<Id<Person>> altersOfAlter = network.getAlters( alter );
			
			for ( Id alterOfAlter : altersOfAlter ) {
				// is the ego?
				if ( alterOfAlter.equals( ego ) ) continue;
				// already a friend?
				if ( alters.contains( alterOfAlter ) ) continue;
				// in good part of contigency matrix?
				if ( !allowedAlters.contains( alterOfAlter ) ) continue;

				unknownAltersOfAlters.add( alterOfAlter );
			}
		}

		final List<T> list = new ArrayList<T>();
		for ( Id<Person> id : unknownAltersOfAlters ) {
			list.add( population.getAgentsMap().get( id ) );
		}

		return list;
	}

}

