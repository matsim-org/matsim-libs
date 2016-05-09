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

import com.google.inject.Inject;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

	private WeightedSocialNetwork preprocess = null;
	private DoublyWeightedSocialNetwork preprocessFriendsOfFriends = null;

	private double lowestStoredPrimary;
	private double lowestStoredSecondary;

	private double lowestKnownPrimaryThreshold = Double.POSITIVE_INFINITY;

	private final int randomSeed;

	private final IndexedPopulation population;
	private final TieUtility utility;

	private final double primarySampleRate;
	private final double secondarySampleRate;

	private final int maxSizePrimary;
	private final int maxSizeSecondary;

	private final int nThreads;

	@Inject
	public PreprocessedModelRunner(
			final SocialNetworkGenerationConfigGroup globalConfig,
			final PreprocessedModelRunnerConfigGroup config,
			final IndexedPopulation population ,
			final TieUtility utility,
			final TiesWeightDistribution distr) {
		this.lowestStoredPrimary = config.getLowestStoredPrimary();
		this.lowestStoredSecondary = config.getLowestStoredSecondary();

		this.primarySampleRate = config.getPrimarySampleRate();
		this.secondarySampleRate = config.getSecondarySampleRate();
		this.population = population;
		this.utility = utility;
		this.nThreads = config.getNThreads();
		this.maxSizePrimary = config.getMaxSizePrimary();
		this.maxSizeSecondary = config.getMaxSizeSecondary();
		this.randomSeed = config.getRandomSeed();

		// This part would be nicer externalized in a "preprocessed network provider" or smth
		if ( config.getInputPreprocessedNetwork() == null ) {
			this.updatePrimaryPreprocess( distr );
			this.updateSecondaryPreprocess( lowestStoredPrimary );
			// needed at least in tests (but one could imagine other usecases where this might happen as well)
			if ( globalConfig.getOutputDirectory() != null ) {
				new WeightedSocialNetworkWriter().write(preprocess, globalConfig.getOutputDirectory() + "/preprocess-network.xml.gz");
				write(distr, globalConfig.getOutputDirectory() + "/scoresHistogrammPrimary.dat");
			}
		}
		else {
			this.preprocess = new WeightedSocialNetworkReader().read( config.getInputPreprocessedNetwork() );
			this.updateSecondaryPreprocess( lowestStoredPrimary );
		}
	}

	private static void write(
			final TiesWeightDistribution distr,
			final String file ) {
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter(file) ) {
			writer.write( "binStart\tbinEnd\tcount" );

			final double[] binStarts = distr.getBinStarts();
			final int[] binCounts = distr.getBinCounts();
			for ( int i = 0; i < binStarts.length; i++ ) {
				writer.newLine();
				writer.write( binStarts[ i ]+"\t"+(binStarts[ i ] + distr.getBinWidth())+"\t"+binCounts[ i ] );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}


	private void updatePrimaryPreprocess( final TiesWeightDistribution distributionToFill ) {
		log.info( "create preprocess network using sampling rate "+primarySampleRate );
		Gbl.printMemoryUsage();

		this.preprocess = new WeightedSocialNetwork( maxSizePrimary , lowestStoredPrimary , population.size() );

		final Counter counter = new Counter( "consider (primary) pair # " );
		final ThreadGroup threads = new ThreadGroup();

		for ( int i=0; i < nThreads; i++ ) {
			final int threadNumber = i;
			final int startThreadAgents = i * population.size() / nThreads;
			final int endThreadAgents = i == nThreads ? population.size() : (i + 1) * population.size() / nThreads;

			threads.add(
					() -> {
						final TiesWeightDistribution threadDistribution =
							distributionToFill != null ?
								new TiesWeightDistribution( distributionToFill.getBinWidth() ) :
								null;

						final Random random = new Random( randomSeed + threadNumber );

						for ( int ego = startThreadAgents; ego < endThreadAgents; ego++ ) {
							for ( int alter=ego + 1; alter < population.size(); alter++ ) {
								if ( random.nextDouble() > primarySampleRate ) continue;
								counter.incCounter();

								final double score = utility.getTieUtility( ego , alter );

								preprocess.addBidirectionalTie(
										ego,
										alter,
										score );

								if ( log.isTraceEnabled() ) {
									log.trace( "Primary preprocessing: added tie "+ego+"<->"+alter+" with weight "+score );
								}

								if ( threadDistribution != null ) threadDistribution.addValue( score );
							}
						}

						if ( distributionToFill != null ) distributionToFill.addCounts( threadDistribution );
					} );
		}

		threads.run();
		// cannot trim "on the fly", even though one always looks "on the right":
		// other threads are also allowed to add elements
		preprocess.trimAll();
		counter.printCounter();

		log.info( "preprocessing done" );
		Gbl.printMemoryUsage();
	}

	@Override
	public SocialNetwork runModel( final Thresholds thresholds ) {
		if ( thresholds.getPrimaryThreshold() < lowestStoredPrimary ) {
			this.lowestStoredPrimary = thresholds.getPrimaryThreshold() - 1;
			updatePrimaryPreprocess( null );
		}

		if ( thresholds.getPrimaryThreshold() < this.lowestKnownPrimaryThreshold ||
				thresholds.getSecondaryThreshold() < lowestStoredSecondary ) {
			// store new friends of friends
			this.lowestStoredSecondary = Math.min( lowestStoredSecondary , thresholds.getSecondaryThreshold() - 1 );
			updateSecondaryPreprocess( thresholds.getPrimaryThreshold() );
			this.lowestKnownPrimaryThreshold = thresholds.getPrimaryThreshold();
		}

		final Map<Id<Person>, Set<Id<Person>>> sn = new ConcurrentHashMap< >();
		log.info( "create ties using preprocessed data" );
		Gbl.printMemoryUsage();

		final Counter counter = new Counter( "consider ego # " );
		final ThreadGroup threads = new ThreadGroup();

		for ( int i=0; i < nThreads; i++ ) {
			final int startThreadAgents = i * population.size() / nThreads;
			final int endThreadAgents = (i + 1) * population.size() / nThreads;

			threads.add(
					() -> {
						final TIntSet friendsOfFriends = new TIntHashSet();
						for ( int ego = startThreadAgents; ego < endThreadAgents; ego++ ) {
							counter.incCounter();
							sn.put(
								population.getId( ego ),
								preprocess.getAltersOverWeight(
									ego,
									thresholds.getPrimaryThreshold(),
									population ) );

							if ( log.isTraceEnabled() ) {
								log.trace( "ego "+population.getId( ego )+": add primary alters "+sn.get( population.getId( ego ) ) );
							}

							friendsOfFriends.clear();
							preprocessFriendsOfFriends.fillWithAltersOverWeights(
								friendsOfFriends,
								ego,
								thresholds.getPrimaryThreshold(),
								thresholds.getSecondaryThreshold() );

							if ( log.isTraceEnabled() ) {
								log.trace( "ego "+population.getId( ego )+": add secondary alters "+friendsOfFriends );
							}

							// sampling already done
							final Set<Id<Person>> newAlters = sn.get( population.getId( ego ) );
							for ( int fof : friendsOfFriends.toArray() ) {
								newAlters.add( population.getId( fof ) );
							}

						}
					} );
		}

		threads.run();
		counter.printCounter();

		Gbl.printMemoryUsage();
		// TODO: check if not possible in loop
		log.info( "fill in network with identified ties" );
		final SocialNetwork net = new SocialNetworkImpl( true );
		for ( int i = 0; i < population.size(); i++ ) net.addEgo( population.getId( i ) );

		for ( Map.Entry<Id<Person>, Set<Id<Person>>> e : sn.entrySet() ) {
			for ( Id<Person> alter : e.getValue() ) {
				if ( log.isTraceEnabled() ) log.trace( "add bidirectional tie "+e.getKey()+" -> "+alter );
				net.addBidirectionalTie( e.getKey(), alter );
			}
		}
		return net;
	}

	private void updateSecondaryPreprocess(
			final double primaryThreshold ) {
		log.info( "update secondary preprocess for use with primary threshold > "+primaryThreshold );
		Gbl.printMemoryUsage();

		preprocessFriendsOfFriends =
			new DoublyWeightedSocialNetwork(
					20,
					lowestStoredSecondary,
					population.size(),
					maxSizeSecondary );

		final Counter counter = new Counter( "add secondary pair # " );
		final ThreadGroup threads = new ThreadGroup();

		for ( int i=0; i < nThreads; i++ ) {
			final int startThreadAgents = i * population.size() / nThreads;
			final int endThreadAgents = i == nThreads ? population.size() : (i + 1) * population.size() / nThreads;

			final Random random = new Random( randomSeed + 20140107 + i );
			threads.add(
					() -> {
						// TODO: modify doublyweighted preprocess to avoid having to do that externally
						final TIntDoubleMap highestPrimary = new TIntDoubleHashMap();

						final TIntSet altersSet = new TIntHashSet();
						final TIntSet altersOfAltersSet = new TIntHashSet();
						for ( int egoi = startThreadAgents; egoi < endThreadAgents; egoi++ ) {
							final int ego = egoi;
							altersSet.clear();
							preprocess.fillWithAltersOverWeight( altersSet , ego , primaryThreshold );

							// for each friend of friend, search for the highest common
							// friend utility.
							highestPrimary.clear();
							altersSet.forEach( alter -> {
								final double alterWeight =
									utility.getTieUtility(
											ego,
											alter );
								altersOfAltersSet.clear();
								preprocess.fillWithAltersOverWeight( altersOfAltersSet , alter , primaryThreshold );

								altersOfAltersSet.forEach( alterOfAlter -> {
									if ( alterOfAlter <= ego ) return true; // only consider upper half of matrix
									if ( altersSet.contains( alterOfAlter ) ) return true;
									if ( random.nextDouble() > secondarySampleRate ) return true;
									counter.incCounter();

									// "utility" of an alter of alter is the min of the
									// two linked ties, as below this utility it is not an
									// alter of alter.
									final double aoaWeight =
										Math.min(
												alterWeight,
												utility.getTieUtility(
													alter,
													alterOfAlter ) );

									if ( !highestPrimary.containsKey( alterOfAlter ) ||
											highestPrimary.get( alterOfAlter ) < aoaWeight ) {
										highestPrimary.put( alterOfAlter , aoaWeight );
									}
									return true;
								} );

								return true;
							} );

							highestPrimary.forEachEntry( ( alterOfAlter, lowestUtilityOfAlter ) -> {
								final double w2 =
										utility.getTieUtility(
											ego,
											alterOfAlter );
								preprocessFriendsOfFriends.addMonodirectionalTie(
										ego,
										alterOfAlter,
										lowestUtilityOfAlter,
										w2 );

								if ( log.isTraceEnabled() ) {
									log.trace( "secondary preprocessing: added tie "+ego+"<->"+alterOfAlter+" with weights "+lowestUtilityOfAlter+" and "+w2 );
								}

								return true;
							} );

							// no tie added to this ego anymore (only monodirectional ties added)
							// trim storing array to avoid waste of space
							preprocessFriendsOfFriends.trim( ego );
						}
					} );
		}

		threads.run();

		counter.printCounter();
		Gbl.printMemoryUsage();
	}
}

