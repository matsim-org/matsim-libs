/* *********************************************************************** *
 * project: org.matsim.*
 * PreprocessedModelRunner.java
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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.Agent;
import playground.thibautd.initialdemandgeneration.socnetgen.framework.SocialPopulation;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkImpl;

/**
 * This runner does a pre-processing for the "primary" ties:
 * it does the process, and stores all ties with utility greater than a given threshold.
 * The idea is the following: in order to sample enough close individuals,
 * one needs a reasonnably high sampling rate, which results in sampling an howful
 * lot of totally irrelevant alters (there are more individuals far away as close).
 * With this preprocessing step, those alters are indeed sampled, but only once:
 * the calibration process then only iterates over alters that actually have a chance
 * to be selected.
 * @author thibautd
 */
public class PreprocessedModelRunner<T extends Agent> implements ModelRunner {
	private static final Logger log =
		Logger.getLogger(PreprocessedModelRunner.class);

	private final WeightedSocialNetwork preprocess;

	private final DoublyWeightedSocialNetwork preprocessFriendsOfFriends;
	private double lowestPrimaryThreshold = Double.POSITIVE_INFINITY;

	private final int randomSeed = 20150116;

	private final SocialPopulation<T> population;
	private final TieUtility<T> utility;

	private final double secondarySampleRate;

	private final int nThreads;

	public PreprocessedModelRunner(
			final double minUtility,
			final SocialPopulation<T> population ,
			final TieUtility<T> utility ,
			final double primarySampleRate ,
			final double secondarySampleRate ,
			final int nThreads ) {
		this.preprocess = new WeightedSocialNetwork( minUtility );
		this.preprocessFriendsOfFriends = new DoublyWeightedSocialNetwork( minUtility );

		this.secondarySampleRate = secondarySampleRate;
		this.population = population;
		this.utility = utility;
		this.nThreads = nThreads;

		log.info( "create preprocess network using sampling rate "+primarySampleRate );
		Gbl.printMemoryUsage();

		preprocess.addEgosIdentifiable( population.getAgents() );

		final Counter counter = new Counter( "consider (primary) pair # " );
		final ThreadGroup threads = new ThreadGroup();

		final List<T> agents = new ArrayList< >( population.getAgents() );

		for ( int i=0; i < nThreads; i++ ) {
			final int threadNumber = i;
			final int startThreadAgents = i * agents.size() / nThreads;
			final int endThreadAgents = i == nThreads ? agents.size() : (i + 1) * agents.size() / nThreads;

			threads.add(
				new Runnable() {
					@Override
					public void run() {
						final Random random = new Random( randomSeed + threadNumber );
						// initialize out loop to reduce stress on GC
						final List<T> potentialAlters = new ArrayList< >();

						for ( int agentIndex = startThreadAgents; agentIndex < endThreadAgents; agentIndex++ ) {
							final T ego = agents.get( agentIndex );

							potentialAlters.clear();
							for ( int pa=agentIndex + 1; pa < agents.size(); pa++ ) {
								potentialAlters.add( agents.get( pa ) );
							}

							int nAltersToConsider = (int) Math.ceil( primarySampleRate * potentialAlters.size() );

							while ( nAltersToConsider-- > 0 && !potentialAlters.isEmpty() ) {
								counter.incCounter();
								final T alter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );

								preprocess.addBidirectionalTie(
										ego.getId(),
										alter.getId(),
										utility.getTieUtility( ego , alter ) );
							}
						}
					}
				} );
		}

		threads.run();
		counter.printCounter();

		log.info( "preprocessing done" );
		Gbl.printMemoryUsage();
	}

	@Override
	public SocialNetwork runModel( final Thresholds thresholds ) {
		final SocialNetwork sn = runPrimary( thresholds );
		runSecondary( thresholds , sn );
		return sn;
	}

	private SocialNetwork runPrimary(
			final Thresholds thresholds ) {
		final Map<Id<Person>, Set<Id<Person>>> sn = new ConcurrentHashMap< >();
		log.info( "create primary ties using preprocessed data" );
		Gbl.printMemoryUsage();

		for ( T agent : population.getAgents() ) sn.put( agent.getId() , new HashSet<Id<Person>>() );

		final Counter counter = new Counter( "consider primary pair # " );
		final ThreadGroup threads = new ThreadGroup();

		final List<T> agents = new ArrayList< >( population.getAgents() );

		for ( int i=0; i < nThreads; i++ ) {
			final int startThreadAgents = i * agents.size() / nThreads;
			final int endThreadAgents = i == nThreads ? agents.size() : (i + 1) * agents.size() / nThreads;

			threads.add(
				new Runnable() {
					@Override
					public void run() {
						for ( int agentIndex = startThreadAgents; agentIndex < endThreadAgents; agentIndex++ ) {
							final Id<Person> ego = agents.get( agentIndex ).getId();

							sn.put( ego , preprocess.getAltersOverWeight( ego , thresholds.getPrimaryThreshold() ) );
						}
					}
				} );
		}

		threads.run();
		counter.printCounter();

		Gbl.printMemoryUsage();
		log.info( "fill in network with primary ties" );
		final SocialNetwork net = new SocialNetworkImpl( true );
		for ( T agent : population.getAgents() ) net.addEgo( agent.getId() );

		for ( Map.Entry<Id<Person>, Set<Id<Person>>> e : sn.entrySet() ) {
			for ( Id<Person> alter : e.getValue() ) {
				net.addBidirectionalTie( e.getKey(), alter );
			}
		}

		if ( thresholds.getPrimaryThreshold() < this.lowestPrimaryThreshold ) {
			// store new friends of friends
			updateSecondaryPreprocess( net );
			this.lowestPrimaryThreshold = thresholds.getPrimaryThreshold();
		}

		Gbl.printMemoryUsage();
		return net;
	}

	private void updateSecondaryPreprocess(
			final SocialNetwork sn ) {
		log.info( "update secondary preprocess" );
		Gbl.printMemoryUsage();
		final List<T> agents = new ArrayList< >( population.getAgents() );

		preprocessFriendsOfFriends.clear();
		preprocessFriendsOfFriends.addEgosIds( sn.getEgos() );

		final Counter counter = new Counter( "add secondary pair # " );
		final ThreadGroup threads = new ThreadGroup();

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
			threads.add(
					new Runnable() {
						@Override
						public void run() {
							final List<Id<Person>> potentialAlters = new ArrayList< >();
							final Map< Id<Person> , Double > highestPrimary = new ConcurrentHashMap< >();

							for ( int agentIndex = startThreadAgents; agentIndex < endThreadAgents; agentIndex++ ) {
								final T egoAgent = agents.get( agentIndex );
								final Id<Person> ego = egoAgent.getId();

								allowedAlters.remove( ego );

								final Set<Id<Person>> alters = sn.getAlters( ego );

								// for each friend of friend, search for the highest common
								// friend utility.
								highestPrimary.clear();
								for ( Id<Person> alter : alters ) {
									final double alterWeight =
										utility.getTieUtility(
												egoAgent,
												population.getAgentsMap().get( alter ) );
									final Set<Id<Person>> altersOfAlter = sn.getAlters( alter );

									potentialAlters.clear();
									for ( Id<Person> fof : altersOfAlter ) {
										if ( !allowedAlters.contains( fof ) ) continue;
										if ( alters.contains( fof ) ) continue;
										potentialAlters.add( fof );
									}

									final T alterAgent = population.getAgentsMap().get( alter );
									for ( int remainingChecks = (int) Math.ceil( secondarySampleRate * potentialAlters.size() );
											remainingChecks > 0 && !potentialAlters.isEmpty();
											remainingChecks--) {
										counter.incCounter();
										final Id<Person> alterOfAlter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );

										// "utility" of an alter of alter is the min of the
										// two linked ties, as below this utility it is not an
										// alter of alter.
										final double aoaWeight =
											Math.min(
													alterWeight,
													utility.getTieUtility(
														alterAgent,
														population.getAgentsMap().get( alterOfAlter ) ) );

										if ( highestPrimary.get( alterOfAlter ) == null ||
												highestPrimary.get( alterOfAlter ) < aoaWeight ) {
											highestPrimary.put( alterOfAlter , aoaWeight );
										}
									}
								}

								for ( Map.Entry<Id<Person>, Double> weight : highestPrimary.entrySet() ) {
									final Id<Person> alterOfAlter = weight.getKey();
									final double lowestUtilityOfAlter = weight.getValue();
									preprocessFriendsOfFriends.addMonodirectionalTie(
											ego,
											alterOfAlter,
											lowestUtilityOfAlter,
											utility.getTieUtility(
												egoAgent,
												population.getAgentsMap().get( alterOfAlter ) ) );
								}
							}
						}
					} );
		}

		threads.run();

		counter.printCounter();
		Gbl.printMemoryUsage();
	}

	private void runSecondary(
			final Thresholds thresholds,
			final SocialNetwork sn ) {
		log.info( "create secondary ties" );
		Gbl.printMemoryUsage();
		final List<T> agents = new ArrayList< >( population.getAgents() );

		final Map<Id<Person>, Set<Id<Person>>> newTies = new ConcurrentHashMap< >();
		for ( T agent : population.getAgents() ) newTies.put( agent.getId() , new HashSet<Id<Person>>() );

		final Counter counter = new Counter( "consider secondary pair # " );
		final ThreadGroup threads = new ThreadGroup();

		for ( int i=0; i < nThreads; i++ ) {
			final int startThreadAgents = i * agents.size() / nThreads;
			final int endThreadAgents = i == nThreads ? agents.size() : (i + 1) * agents.size() / nThreads;

			// always consider only "upper-right" half of contingency matrix
			// (because symetric)
			final Set<Id<Person>> allowedAlters = new HashSet< >();
			for ( int ai = startThreadAgents; ai < agents.size(); ai++ ) {
				allowedAlters.add( agents.get( ai ).getId() );
			}

			threads.add(
					new Runnable() {
						@Override
						public void run() {
							for ( int agentIndex = startThreadAgents; agentIndex < endThreadAgents; agentIndex++ ) {
								final T ego = agents.get( agentIndex );
								allowedAlters.remove( ego.getId() );

								final Set<Id<Person>> friendsOfFriends =
									preprocessFriendsOfFriends.getAltersOverWeights(
										ego.getId(),
										thresholds.getPrimaryThreshold(),
										thresholds.getSecondaryThreshold() );

								// sampling already done
								for ( Id<Person> fof : friendsOfFriends ) {
									counter.incCounter();
									final T alter = population.getAgentsMap().get( fof );

									if ( utility.getTieUtility( ego , alter ) > thresholds.getSecondaryThreshold() ) {
										newTies.get( ego.getId() ).add( alter.getId() );
									}
								}
							}
						}
					} );
		}

		threads.run();

		counter.printCounter();
		Gbl.printMemoryUsage();

		log.info( "fill in social network with secondary ties" );
		for ( Map.Entry<Id<Person>, Set<Id<Person>>> e : newTies.entrySet() ) {
			for ( Id<Person> alter : e.getValue() ) {
				sn.addBidirectionalTie( e.getKey() , alter );
			}
		}
		Gbl.printMemoryUsage();
	}
}

