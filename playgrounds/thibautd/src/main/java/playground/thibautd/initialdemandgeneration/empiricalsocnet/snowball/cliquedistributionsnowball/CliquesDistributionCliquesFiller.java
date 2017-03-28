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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.cliquedistributionsnowball;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliqueStub;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.Position;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballCliques;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballSamplingConfigGroup;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SocialPositions;
import playground.thibautd.utils.ArrayUtils;
import playground.thibautd.utils.spatialcollections.SpatialTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
public class CliquesDistributionCliquesFiller implements CliquesFiller {
	private static final Logger log = Logger.getLogger( CliquesDistributionCliquesFiller.class );

	private final Random random = MatsimRandom.getLocalInstance();
	private final CliqueSampler allCliques;

	private final Position position;

	private final Counter abortCounter = new Counter( "Incomplete clique # " );

	@Inject
	public CliquesDistributionCliquesFiller(
			final SnowballCliques snowballCliques,
			final SnowballSamplingConfigGroup configGroup,
			final Position position,
			final AutocloserModule.Closer closer,
			final SocialPositions socialPositions ) {
		this.position = position;

		final List<SocialPositions.CliquePositions> allCliquesList = new ArrayList<>();
		for ( SnowballCliques.Clique snowballClique : snowballCliques.getCliques().values() ) {
			final SocialPositions.CliquePositions clique = new SocialPositions.CliquePositions();

			allCliquesList.add( clique );

			for ( SnowballCliques.Member alter : snowballClique.getAlters() ) {
				clique.positions.add( socialPositions.calcPosition( snowballClique.getEgo() , alter ) );
			}
		}

		allCliques = new CliqueSampler( allCliquesList );
		closer.add( abortCounter::printCounter );
	}

	@Override
	public Set<Ego> sampleClique(
			final CliqueStub cliqueStub,
			final SpatialTree<double[], CliqueStub> freeStubs ) {
		final int size = cliqueStub.getCliqueSize();
		final SocialPositions.CliquePositions clique =
				allCliques.sampleClique(
						random,
						size );
		assert clique.size() == size;

		final double rotation = random.nextDouble() * 2 * Math.PI;
		final Set<CliqueStub> members = new HashSet<>();
		members.add( cliqueStub );
		for ( SocialPositions.CliquePosition cliqueMember : clique ) {
			final double[] point = position.calcPosition( cliqueStub, cliqueMember , rotation );
			final CliqueStub member = findEgo( freeStubs, clique, point, members );

			if ( member == null ) {
				abortCounter.incCounter();
				// there does not seem to remain enough agents for the given clique size.
				// for now just ignore them, and do something smarter only if it appears to bias a lot
				// given how we failed, those should be the only egos with this clique size to allocate
				break;
			}
			members.add( member );
		}

		for ( CliqueStub member : members ) {
			freeStubs.remove( member );
		}

		final Set<Ego> cliqueMembers =
				members.stream()
						.map( CliqueStub::getEgo )
						.collect( Collectors.toSet() );

		return cliqueMembers.size() > 1 ? cliqueMembers : null;
	}

	public static CliqueStub findEgo( final SpatialTree<double[],CliqueStub> egosWithFreeStubs,
			final SocialPositions.CliquePositions clique,
			final double[] point,
			final Collection<CliqueStub> currentClique ) {
		// TODO: could it be improved by putting size into coordinate system?
		// would increase tree complexity but decrease search space, and thus the number of unfulfilled predicates...
		return egosWithFreeStubs.getClosest(
				point,
				e1 -> !currentClique.contains( e1 ) );
	}

	public static class CliqueSampler {
		private final SocialPositions.CliquePositions[] cliques;

		private int first = -1;
		private int last = -1;

		public CliqueSampler( final Collection<SocialPositions.CliquePositions> l ) {
			this.cliques = l.toArray( new SocialPositions.CliquePositions[ l.size() ] );
			Arrays.sort( this.cliques , (c1, c2) -> Integer.compare( c1.size() , c2.size() ) );
		}

		public SocialPositions.CliquePositions sampleClique(
				final Random random,
				final int size ) {
			updateSize( size );
			// we always operate on all cliques, and by construction, there is no possibility that we search for a size
			// that does not exist.
			return cliques[ first + random.nextInt( last - first ) ];
		}

		private void updateSize( final int size ) {
			last = ArrayUtils.searchLowest(
					cliques,
					SocialPositions.CliquePositions::size,
					size,
					0 , cliques.length );
			first = ArrayUtils.searchLowest(
					cliques,
					SocialPositions.CliquePositions::size,
					size - 1,
					0 , last );
			assert first < last : size;
		}
	}
}
