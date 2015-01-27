/* *********************************************************************** *
 * project: org.matsim.*
 * DoublyWeightedSocialNetwork.java
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;

/**
 * @author thibautd
 */
public class DoublyWeightedSocialNetwork {
	private final Map<Id<Person>, DoublyWeightedFriends> altersMap = new ConcurrentHashMap< >();
	private final double lowestAllowedFirstWeight;
	private final double lowestAllowedSecondWeight;

	public DoublyWeightedSocialNetwork( final double lowestWeight ) {
		this.lowestAllowedFirstWeight = lowestWeight;
		this.lowestAllowedSecondWeight = lowestWeight;
	}

	public void addEgo( final Id<Person> ego ) {
		altersMap.put( ego , new DoublyWeightedFriends() );
	}

	public void addEgosIds( final Collection<Id<Person>> egos ) {
		for ( Id<Person> ego : egos ) {
			altersMap.put( ego , new DoublyWeightedFriends() );
		}
	}

	public void addEgosIdentifiable( final Collection<? extends Identifiable<Person>> egos ) {
		for ( Identifiable<Person> ego : egos ) {
			altersMap.put( ego.getId() , new DoublyWeightedFriends() );
		}
	}

	public void clear() {
		altersMap.clear();
	}

	public void addBidirectionalTie(
			final Id<Person> ego,
			final Id<Person> alter,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) return;
		if ( weight2 < lowestAllowedSecondWeight ) return;
		altersMap.get( ego ).add( alter , weight1 , weight2 );
		altersMap.get( alter ).add( ego , weight1 , weight2 );
	}

	public void addMonodirectionalTie(
			final Id<Person> ego,
			final Id<Person> alter,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) return;
		if ( weight2 < lowestAllowedSecondWeight ) return;
		altersMap.get( ego ).add( alter , weight1 , weight2 );
	}


	public Set<Id<Person>> getAltersOverWeights(
			final Id<Person> ego,
			final double weight1,
			final double weight2) {
		if ( weight1 < lowestAllowedFirstWeight ) throw new IllegalArgumentException( "first weight "+weight1+" is lower than lowest stored weight "+lowestAllowedFirstWeight );
		if ( weight2 < lowestAllowedSecondWeight ) throw new IllegalArgumentException( "second weight "+weight2+" is lower than lowest stored weight "+lowestAllowedSecondWeight );
		return altersMap.get( ego ).getAltersOverWeights( weight1 , weight2 );
	}

	// point quad-tree
	// No check is done that is is balanced!
	private static final class DoublyWeightedFriends {
		private Id[] friends = new Id[ 20 ];
		private double weights1[] = new double[ 20 ];
		private double weights2[] = new double[ 20 ];

		private int childSE[] = new int[ 20 ];
		private int childSW[] = new int[ 20 ];
		private int childNE[] = new int[ 20 ];
		private int childNW[] = new int[ 20 ];

		private int size = 0;

		public synchronized void add(
				final Id<Person> friend,
				final double firstWeight,
				final double secondWeight ) {
			final int parent = searchParentLeaf( 0, firstWeight, secondWeight );

			if ( size == friends.length ) expand();

			final int[] quadrant = getQuadrant( parent , firstWeight, secondWeight );
			friends[ size ] = friend;

			weights1[ size ] = firstWeight;
			weights2[ size ] = secondWeight;

			quadrant[ parent ] = size;
			size++;
		}

		private void expand() {
			final int newLength = 2 * friends.length;
			friends = Arrays.copyOf( friends , newLength );

			weights1 = Arrays.copyOf( weights1 , newLength );
			weights2 = Arrays.copyOf( weights2 , newLength );

			childSE = Arrays.copyOf( childSE , newLength );
			childSW = Arrays.copyOf( childSW , newLength );
			childNE = Arrays.copyOf( childNE , newLength );
			childNW = Arrays.copyOf( childNW , newLength );
		}

		private int searchParentLeaf(
				final int head,
				final double firstWeight,
				final double secondWeight ) {
			int[] quadrant = getQuadrant( head, firstWeight, secondWeight );

			return quadrant[ head ] == 0 ? head : searchParentLeaf( quadrant[head], firstWeight, secondWeight );
		}

		private int[] getQuadrant(
				final int head,
				final double firstWeight,
				final double secondWeight ) {
			if ( firstWeight > weights1[ head ] ) {
				return secondWeight > weights2[ head ] ? childNE : childSE;
			}
			return secondWeight > weights2[ head ] ? childNW : childSW;
		}

		public Set<Id<Person>> getAltersOverWeights(
				final double firstWeight,
				final double secondWeight ) {
			final Set<Id<Person>> alters = new HashSet<Id<Person>>();

			addGreaterPoints( 0, alters, firstWeight, secondWeight );

			return alters;
		}

		private void addGreaterPoints(
				final int head,
				final Set<Id<Person>> alters,
				final double firstWeight,
				final double secondWeight ) {
			if ( weights1[ head ] > firstWeight && weights2[ head ] > secondWeight ) {
				alters.add( friends[ head ] );
				addGreaterPoints( childSW[ head ] , alters , firstWeight , secondWeight );
			}
			if ( weights1[ head ] > firstWeight ) {
				addGreaterPoints( childNW[ head ] , alters , firstWeight , secondWeight );
			}
			if ( weights2[ head ] > secondWeight ) {
				addGreaterPoints( childSE[ head ] , alters , firstWeight , secondWeight );
			}
			// always look to the NW
			addGreaterPoints( childNE[ head ] , alters , firstWeight , secondWeight );
		}
	}
}

