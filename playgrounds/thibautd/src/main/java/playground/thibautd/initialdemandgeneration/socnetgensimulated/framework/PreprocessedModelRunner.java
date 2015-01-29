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
public class PreprocessedModelRunner implements ModelRunner {
	private static final Logger log =
		Logger.getLogger(PreprocessedModelRunner.class);

	private final WeightedSocialNetwork preprocess;

	private final DoublyWeightedSocialNetwork preprocessFriendsOfFriends;
	private double lowestPrimaryThreshold = Double.POSITIVE_INFINITY;

	private final int randomSeed = 20150116;

	private final IndexedPopulation population;
	private final TieUtility utility;

	private final double secondarySampleRate;

	private final int nThreads;

	public PreprocessedModelRunner(
			final double minUtilityPrimary,
			final double minUtilitySecondary,
			final IndexedPopulation population ,
			final TieUtility utility ,
			final double primarySampleRate ,
			final double secondarySampleRate ,
			final int nThreads ) {
		this.preprocess = new WeightedSocialNetwork( minUtilityPrimary , population.size() );
		this.preprocessFriendsOfFriends = new DoublyWeightedSocialNetwork( minUtilitySecondary , population.size() );

		this.secondarySampleRate = secondarySampleRate;
		this.population = population;
		this.utility = utility;
		this.nThreads = nThreads;

		log.info( "create preprocess network using sampling rate "+primarySampleRate );
		Gbl.printMemoryUsage();

		final Counter counter = new Counter( "consider (primary) pair # " );
		final ThreadGroup threads = new ThreadGroup();

		for ( int i=0; i < nThreads; i++ ) {
			final int threadNumber = i;
			final int startThreadAgents = i * population.size() / nThreads;
			final int endThreadAgents = i == nThreads ? population.size() : (i + 1) * population.size() / nThreads;

			threads.add(
				new Runnable() {
					@Override
					public void run() {
						final Random random = new Random( randomSeed + threadNumber );
						// initialize out loop to reduce stress on GC
						final List<Integer> potentialAlters = new ArrayList< >();

						for ( int ego = startThreadAgents; ego < endThreadAgents; ego++ ) {
							potentialAlters.clear();
							for ( int pa=ego + 1; pa < population.size(); pa++ ) {
								potentialAlters.add( pa );
							}

							int nAltersToConsider = (int) Math.ceil( primarySampleRate * potentialAlters.size() );

							while ( nAltersToConsider-- > 0 && !potentialAlters.isEmpty() ) {
								counter.incCounter();
								final int alter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );

								preprocess.addBidirectionalTie(
										ego,
										alter,
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
		if ( thresholds.getPrimaryThreshold() < this.lowestPrimaryThreshold ) {
			// store new friends of friends
			updateSecondaryPreprocess( thresholds.getPrimaryThreshold() );
			this.lowestPrimaryThreshold = thresholds.getPrimaryThreshold();
		}

		final Map<Id<Person>, Set<Id<Person>>> sn = new ConcurrentHashMap< >();
		log.info( "create ties using preprocessed data" );
		Gbl.printMemoryUsage();

		final Counter counter = new Counter( "consider primary pair # " );
		final Counter counter2 = new Counter( "consider secondary pair # " );
		final ThreadGroup threads = new ThreadGroup();

		for ( int i=0; i < nThreads; i++ ) {
			final int startThreadAgents = i * population.size() / nThreads;
			final int endThreadAgents = i == nThreads ? population.size() : (i + 1) * population.size() / nThreads;

			threads.add(
				new Runnable() {
					@Override
					public void run() {
						for ( int ego = startThreadAgents; ego < endThreadAgents; ego++ ) {
							sn.put(
								population.getId( ego ),
								preprocess.getAltersOverWeight(
									ego,
									thresholds.getPrimaryThreshold(),
									population ) );

							final Set<Integer> friendsOfFriends =
								preprocessFriendsOfFriends.getAltersOverWeights(
									ego,
									thresholds.getPrimaryThreshold(),
									thresholds.getSecondaryThreshold() );

							// sampling already done
							final Set<Id<Person>> newAlters = sn.get( population.getId( ego ) );
							for ( int fof : friendsOfFriends ) {
								counter2.incCounter();
								if ( utility.getTieUtility( ego , fof ) > thresholds.getSecondaryThreshold() ) {
									newAlters.add( population.getId( fof ) );
								}
							}

						}
					}
				} );
		}

		threads.run();
		counter.printCounter();
		counter2.printCounter();

		Gbl.printMemoryUsage();
		// TODO: check if not possible in loop
		log.info( "fill in network with identified ties" );
		final SocialNetwork net = new SocialNetworkImpl( true );
		for ( int i = 0; i < population.size(); i++ ) net.addEgo( population.getId( i ) );

		for ( Map.Entry<Id<Person>, Set<Id<Person>>> e : sn.entrySet() ) {
			for ( Id<Person> alter : e.getValue() ) {
				net.addBidirectionalTie( e.getKey(), alter );
			}
		}
		return net;
	}

	private void updateSecondaryPreprocess(
			final double primaryThreshold ) {
		log.info( "update secondary preprocess for use with primary threshold > "+primaryThreshold );
		Gbl.printMemoryUsage();

		preprocessFriendsOfFriends.clear();

		final Counter counter = new Counter( "add secondary pair # " );
		final ThreadGroup threads = new ThreadGroup();

		for ( int i=0; i < nThreads; i++ ) {
			final int startThreadAgents = i * population.size() / nThreads;
			final int endThreadAgents = i == nThreads ? population.size() : (i + 1) * population.size() / nThreads;

			final Random random = new Random( randomSeed + 20140107 + i );
			threads.add(
					new Runnable() {
						@Override
						public void run() {
							final List<Integer> potentialAlters = new ArrayList< >();
							// TODO: modify doublyweighted preprocess to avoid having to do that externally
							final Map< Integer , Double > highestPrimary = new ConcurrentHashMap< >();

							for ( int ego = startThreadAgents; ego < endThreadAgents; ego++ ) {
								final int[] alters = preprocess.getAltersOverWeight( ego , primaryThreshold );
								// TODO: find better way...
								final Set<Integer> altersSet = new HashSet<Integer>();

								// for each friend of friend, search for the highest common
								// friend utility.
								highestPrimary.clear();
								for ( int alter : alters ) {
									final double alterWeight =
										utility.getTieUtility(
												ego,
												alter );
									final int[] altersOfAlter = preprocess.getAltersOverWeight( alter , primaryThreshold );

									potentialAlters.clear();
									for ( int fof : altersOfAlter ) {
										if ( fof <= ego ) continue; // only consider upper half of matrix
										if ( altersSet.contains( fof ) ) continue;
										potentialAlters.add( fof );
									}

									for ( int remainingChecks = (int) Math.ceil( secondarySampleRate * potentialAlters.size() );
											remainingChecks > 0 && !potentialAlters.isEmpty();
											remainingChecks--) {
										counter.incCounter();
										final int alterOfAlter = potentialAlters.remove( random.nextInt( potentialAlters.size() ) );

										// "utility" of an alter of alter is the min of the
										// two linked ties, as below this utility it is not an
										// alter of alter.
										final double aoaWeight =
											Math.min(
													alterWeight,
													utility.getTieUtility(
														alter,
														alterOfAlter ) );

										if ( highestPrimary.get( alterOfAlter ) == null ||
												highestPrimary.get( alterOfAlter ) < aoaWeight ) {
											highestPrimary.put( alterOfAlter , aoaWeight );
										}
									}
								}

								for ( Map.Entry<Integer, Double> weight : highestPrimary.entrySet() ) {
									final int alterOfAlter = weight.getKey();
									final double lowestUtilityOfAlter = weight.getValue();
									preprocessFriendsOfFriends.addMonodirectionalTie(
											ego,
											alterOfAlter,
											lowestUtilityOfAlter,
											utility.getTieUtility(
												ego,
												alterOfAlter ) );
								}
							}
						}
					} );
		}

		threads.run();

		counter.printCounter();
		Gbl.printMemoryUsage();
	}
}

