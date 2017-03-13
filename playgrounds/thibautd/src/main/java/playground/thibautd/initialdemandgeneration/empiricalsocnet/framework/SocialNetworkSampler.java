/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.utils.spatialcollections.SpatialCollectionUtils;
import playground.thibautd.utils.spatialcollections.SpatialTree;
import playground.thibautd.utils.spatialcollections.VPTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
@Singleton
public class SocialNetworkSampler {
	private static final Logger log = Logger.getLogger( SocialNetworkSampler.class );

	private final Population population;
	private final EgoCharacteristicsDistribution egoDistribution;
	private final CliquesFiller cliquesFiller;
	private final EgoLocator egoLocator;
	private final SpatialCollectionUtils.Metric<double[]> metric;

	private Consumer<Set<Ego>> cliquesListener = (e) -> {};

	@Inject
	public SocialNetworkSampler(
			final Population population,
			final EgoCharacteristicsDistribution degreeDistribution,
			final CliquesFiller cliquesFiller,
			final EgoLocator egoLocator,
			final SpatialCollectionUtils.Metric<double[]> metric ) {
		this.population = population;
		this.egoDistribution = degreeDistribution;
		this.cliquesFiller = cliquesFiller;
		this.egoLocator = egoLocator;
		this.metric = metric;
	}

	public void addCliqueListener( final Consumer<Set<Ego>> l ) {
		cliquesListener = cliquesListener.andThen( l );
	}

	public SocialNetwork sampleSocialNetwork() {
		log.info( "Prepare data for social network sampling" );
		final Collection<Tuple<Ego, Collection<CliqueStub>>> egos = new ArrayList<>();
		for ( Person p : population.getPersons().values() ) {
			final Tuple<Ego,Collection<CliqueStub>> ego = egoDistribution.sampleEgo( p );
			egos.add( ego );
		}
		final SpatialTree<double[],CliqueStub> freeStubs = createSpatialTree();
		freeStubs.add(
				egos.stream()
						.map( Tuple::getSecond )
						.flatMap( Collection::stream )
						.collect( Collectors.toList() ) );

		log.info( "Start sampling with "+egos.size()+" egos" );
		log.info( "Start sampling with "+freeStubs.size()+" free stubs" );
		final Counter counter = new Counter( "Sample clique # " );
		while ( freeStubs.size() > 1 ) {
			counter.incCounter();

			final CliqueStub stub = freeStubs.getAny();

			final Set<Ego> clique = cliquesFiller.sampleClique( stub , freeStubs );
			if ( clique == null ) continue;

			link( clique );
			cliquesListener.accept( clique );
		}
		counter.printCounter();

		// to assess what kind of damage the resolution of "conflicts" did
		final int sumPlannedDegrees =
				egos.stream()
						.map( Tuple::getFirst )
						.mapToInt( Ego::getDegree )
						.sum();
		final int sumActualDegrees =
				egos.stream()
						.map( Tuple::getFirst )
						.map( Ego::getAlters )
						.mapToInt( Collection::size )
						.sum();

		log.info( "Average planned degree was "+((double) sumPlannedDegrees / egos.size()) );
		log.info( "Average actual degree is "+((double) sumActualDegrees / egos.size()) );
		log.info( "Number of excedentary ties: "+(sumActualDegrees - sumPlannedDegrees) );

		return new SampledSocialNetwork(
				egos.stream()
						.map( Tuple::getFirst )
						.collect(
							Collectors.toMap(
									Ego::getId,
									e -> e ) ) );
	}

	private static void link( final Set<Ego> members ) {
		for ( Ego ego : members ) {
			for ( Ego alter : members ) {
				if ( alter == ego ) break;
				alter.getAlters().add( ego );
				ego.getAlters().add( alter );
			}
		}
	}

	private SpatialTree<double[],CliqueStub> createSpatialTree() {
		return new VPTree<>(
				metric,
				egoLocator );
	}
}
