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
package playground.thibautd.analysis.socialchoicesetconstraints;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.utils.CsvParser;
import playground.thibautd.utils.spatialcollections.VPTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
class Utils {
	public static VPTree<Coord,ActivityFacility> createTree( final ActivityFacilities activityFacilities ) {
		final Collection<ActivityFacility> facilities =
				activityFacilities.getFacilities().values().stream()
						// TODO: make predicate configurable
						.filter( a -> a.getActivityOptions().keySet().contains( "leisure" ) )
						.collect( Collectors.toList() );

		final VPTree<Coord,ActivityFacility> tree = new VPTree<>( CoordUtils::calcEuclideanDistance , ActivityFacility::getCoord );
		tree.add( facilities );

		return tree;
	}

	public static AllCliques readMaximalCliques( final String cliquesCsv ) {
		final AllCliques cliques = new AllCliques();

		try ( final CsvParser reader = new CsvParser( '\t' , '"' , cliquesCsv ) ) {
			String currentCliqueId = null;
			Set<Id<Person>> currentClique = null;
			while ( reader.nextLine() ) {
				final String cliqueId = reader.getField( "cliqueId" );
				final Id<Person> egoId = reader.getIdField( "egoId", Person.class );

				if ( !cliqueId.equals( currentCliqueId ) ) {
					currentCliqueId = cliqueId;
					currentClique = new HashSet<>();
					cliques.addClique( currentClique );
				}

				currentClique.add( egoId );
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}

		return cliques;
	}

	public static class AllCliques {
		private final Collection<Set<Id<Person>>> maximalCliques = new ArrayList<>();

		public Collection<Set<Id<Person>>> getMaximalCliques() {
			return maximalCliques;
		}

		public int getMaxSize() {
			return maximalCliques.stream()
					.mapToInt( Set::size )
					.max()
					.orElseGet( () -> 0 );
		}

		public Collection<Set<Id<Person>>> getCliquesOfSize( final int size ) {
			final Collection<Set<Id<Person>>> cliques = new HashSet<>();

			for ( Set<Id<Person>> clique : maximalCliques ) {
				cliques.addAll( allCombinations( size , new ArrayList<>( clique ) ) );
			}

			return cliques;
		}

		private Collection<Set<Id<Person>>> allCombinations(
				final int size,
				final List<Id<Person>> clique ) {
			if ( clique.size() < size ) return Collections.emptyList();
			if ( size == 0 ) return Collections.singleton( new HashSet<>() );
			if ( clique.size() == size ) return Collections.singleton( new HashSet<>( clique ) );

			final Collection<Set<Id<Person>>> combs = new ArrayList<>();

			assert clique.size() > 1 : size +" in "+ clique.size();
			for ( int i=0; i <= clique.size() - size; i++ ) {
				final Id<Person> p = clique.get( i );
				final Collection<Set<Id<Person>>> subsets = allCombinations( size - 1 , clique.subList( i+1 , clique.size() ) );

				subsets.stream().peek( s -> s.add( p ) ).forEach( combs::add );
				combs.addAll( subsets );
			}

			return combs;
		}

		public void addClique( Set<Id<Person>> c ) {
			maximalCliques.add( c );
		}
	}
}
