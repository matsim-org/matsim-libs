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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.degreebased;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.MapUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.Position;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballCliques;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballSamplingConfigGroup;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SocialPositions;
import playground.thibautd.utils.ArrayUtils;
import playground.thibautd.utils.KDTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author thibautd
 */
@Singleton
public class SimpleCliquesFiller implements CliquesFiller {
	private static final Logger log = Logger.getLogger( SimpleCliquesFiller.class );

	private final Random random = MatsimRandom.getLocalInstance();
	private final Map<SocialPositions.EgoClass, CliqueSampler> cliques = new LinkedHashMap<>(  );
	private final CliqueSampler allCliques;

	private final SnowballSamplingConfigGroup configGroup;

	private final Set<SocialPositions.EgoClass> knownEmptyClasses = new HashSet<>();

	private final Position position;

	@Inject
	public SimpleCliquesFiller(
			final SnowballCliques snowballCliques,
			final SnowballSamplingConfigGroup configGroup,
			final Position position ) {
		this.configGroup = configGroup;
		this.position = position;

		final Map<SocialPositions.EgoClass, List<SocialPositions.CliquePositions>> cliquesMap = new LinkedHashMap<>();
		final List<SocialPositions.CliquePositions> allCliquesList = new ArrayList<>();
		for ( SnowballCliques.Clique snowballClique : snowballCliques.getCliques().values() ) {
			final SocialPositions.CliquePositions clique = new SocialPositions.CliquePositions();

			MapUtils.getList( SocialPositions.createEgoClass( configGroup , snowballClique.getEgo() ) , cliquesMap ).add( clique );
			allCliquesList.add( clique );

			for ( SnowballCliques.Member alter : snowballClique.getAlters() ) {
				clique.positions.add( SocialPositions.calcPosition( snowballClique.getEgo() , alter ) );
			}
		}

		for ( Map.Entry<SocialPositions.EgoClass,List<SocialPositions.CliquePositions>> e : cliquesMap.entrySet() ) {
			cliques.put( e.getKey() , new CliqueSampler( e.getValue() ) );
		}
		allCliques = new CliqueSampler( allCliquesList );
	}

	@Override
	public Set<Ego> sampleClique(
			final Ego ego,
			final KDTree<Ego> egosWithFreeStubs ) {
		final SocialPositions.EgoClass egoClass = SocialPositions.createEgoClass( configGroup , ego );
		CliqueSampler cliqueSampler = cliques.get( egoClass );

		if ( cliqueSampler == null ) {
			if ( knownEmptyClasses.add( egoClass ) ) {
				log.warn( "No cliques for egos of class "+egoClass );
				log.warn( "Sampling unconditionned instead" );
			}
			cliqueSampler = allCliques;
		}

		final SocialPositions.CliquePositions clique = cliqueSampler.sampleClique( random , egosWithFreeStubs.size() );
		if ( clique == null ) {
			egosWithFreeStubs.remove( ego );
			return null;
		}

		final Set<Ego> members = new HashSet<>();
		members.add( ego );
		for ( SocialPositions.CliquePosition cliqueMember : clique ) {
			// TODO: rotate? -> only once per clique
			final double[] point = position.calcPosition( ego , cliqueMember );
			final Ego member = findEgo( egosWithFreeStubs, clique, point , members );

			if ( member == null ) {
				throw new RuntimeException( "no alter found at "+ Arrays.toString( point )+" for clique size "+clique.size() );
			}

			SocialPositions.group( member , members );
		}

		return members;
	}

	@Override
	public boolean stopConsidering( final Ego ego ) {
		return ego.getDegree() <= ego.getAlters().size();
	}

	public static Ego findEgo( final KDTree<Ego> egosWithFreeStubs,
			final SocialPositions.CliquePositions clique,
			final double[] point,
			final Set<Ego> currentClique ) {
		// Allow more ties than stubs in case no agent can be found.
		// should remain OK, because:
		// - clique size cannot exceed number of agents with free stubs
		// - this can happen at most once per agent
		// - we keep the increase minimal, by increasingly increasing tolerance.
		for ( int i = 1; true; i++ ) {
			final int freeStubs = clique.size() - i;
			final Ego member =
					egosWithFreeStubs.getClosestEuclidean(
							point,
							( e ) -> e.getFreeStubs() >= freeStubs && !currentClique.contains( e ) );
			if ( member != null ) return member;
		}
	}

	public static class CliqueSampler {
		private final SocialPositions.CliquePositions[] cliques;

		private int currentMaxSize = -1;
		private int currentMaxIndex = -1;

		public CliqueSampler( final Collection<SocialPositions.CliquePositions> l ) {
			this.cliques = l.toArray( new SocialPositions.CliquePositions[ l.size() ] );
			Arrays.sort( this.cliques , (c1, c2) -> Integer.compare( c1.size() , c2.size() ) );
		}

		public SocialPositions.CliquePositions sampleClique(
				final Random random,
				final int maxSize ) {
			updateMaxSize( maxSize );
			if ( currentMaxIndex == 0 ) return null;
			return cliques[ random.nextInt( currentMaxIndex ) ];
		}

		private void updateMaxSize( final int maxSize ) {
			if ( maxSize == currentMaxSize ) return;

			// if greater than the known max size, restart from scratch.
			// otherwise, start from known max
			if ( maxSize > currentMaxSize ) {
				currentMaxSize = cliques[ cliques.length - 1 ].size();
				currentMaxIndex = cliques.length;
			}

			currentMaxIndex = ArrayUtils.searchLowest(
					cliques,
					SocialPositions.CliquePositions::size,
					maxSize,
					0 , currentMaxIndex );
			currentMaxSize = maxSize;
		}
	}
}
