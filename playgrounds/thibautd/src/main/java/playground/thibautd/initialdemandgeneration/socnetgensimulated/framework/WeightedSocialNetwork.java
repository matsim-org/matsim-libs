/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedSocialNetwork.java
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

import gnu.trove.set.TIntSet;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * Stores ties and their utilities, if they are over a pre-defined threshold.
 * This allows to only consider the most relevant potential contacts from iteration
 * 2 of the calibration: contacts with a low utility are discarded forever,
 * and never looked back at.
 * @author thibautd
 */
public class WeightedSocialNetwork {
	private static final Logger log =
		Logger.getLogger(WeightedSocialNetwork.class);

	private final WeightedFriends[] alters;
	private final double lowestAllowedWeight;

	private final int maximalSize;

	public WeightedSocialNetwork(
			final int maximalSize,
			final double lowestWeight,
			final int populationSize ) {
		this.lowestAllowedWeight = lowestWeight;
		this.alters = new WeightedFriends[ populationSize ];
		this.maximalSize = maximalSize;

		for ( int i=0; i < this.alters.length; i++ ) {
			this.alters[ i ] = new WeightedFriends( 10 , maximalSize );
		}
	}

	public void clear() {
		for ( int i=0; i < this.alters.length; i++ ) {
			this.alters[ i ] = new WeightedFriends( 10 , maximalSize );
		}
	}

	public void addBidirectionalTie(
			final int ego,
			final int alter,
			final double weight ) {
		if ( weight < lowestAllowedWeight ) return;
		alters[ ego ].add( alter , weight );
		alters[ alter ].add( ego , weight );
	}

	/**
	 * Memory optimisation: shrinks storing arrays so that they do not contain
	 * unused slots.
	 */
	public void trim( final int ego ) {
		this.alters[ ego ].trim();
	}

	public void trimAll() {
		for ( int i = 0; i < alters.length; i++ ) {
			trim( i );
		}
	}

	public Set<Id<Person>> getAltersOverWeight(
			final int ego,
			final double weight,
			final IndexedPopulation population) {
		if ( weight < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+weight+" is lower than lowest stored weight "+lowestAllowedWeight );
		return alters[ ego ].getAltersOverWeight( weight , population );
	}

	public int[] getAltersOverWeight(
			final int ego,
			final double weight ) {
		if ( weight < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+weight+" is lower than lowest stored weight "+lowestAllowedWeight );
		return alters[ ego ].getAltersOverWeight( weight );
	}

	public void fillWithAltersOverWeight(
			final TIntSet set,
			final int ego,
			final double weight ) {
		if ( weight < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+weight+" is lower than lowest stored weight "+lowestAllowedWeight );
		alters[ ego ].fillWithAltersOverWeight( set , weight );
	}

	// for tests
	/*package*/ int getSize( final int ego ) {
		return alters[ ego ].size;
	}

	private static final class WeightedFriends {
		private int[] friends;
		// use float instead of double for saving memory.
		// TODO: check robustness of the results facing this...
		private float[] weights;

		private final int maximalSize;
		private int size = 0;

		public WeightedFriends( final int initialSize , final int maximalSize ) {
			this.maximalSize = maximalSize;
			this.friends = new int[ initialSize ];
			this.weights = new float[ initialSize ];
			Arrays.fill( weights , Float.POSITIVE_INFINITY  );
		}

		public synchronized void add( final int friend , final double weight ) {
			final float fweight = (float) weight; // TODO check overflow?
			final int insertionPoint = getInsertionPoint( fweight );
			insert( friend, fweight , insertionPoint );
			assert size <= friends.length;
			assert weights.length == friends.length;
		}

		public Set<Id<Person>> getAltersOverWeight(
				final double weight,
				final IndexedPopulation population ) {
			final Set<Id<Person>> alters = new LinkedHashSet< >();
			for ( int i = size - 1;
					i >= 0 && weights[ i ] >= weight;
					i-- ) {
				alters.add( population.getId( friends[ i ] ) );
			}
			return alters;
		}

		public void fillWithAltersOverWeight(
				final TIntSet set,
				final double weight ) {
			for ( int i = size - 1;
					i >= 0 && weights[ i ] >= weight;
					i-- ) {
				set.add( friends[ i ] );
			}
		}


		public int[] getAltersOverWeight(
				final double weight ) {
			final int insertionPoint = getInsertionPoint( (float) weight );
			return Arrays.copyOfRange( friends , insertionPoint , size );
		}

		private int getInsertionPoint( final float weight ) {
			return getInsertionPoint( weight , 0 );
		}

		private int getInsertionPoint( final float weight , final int from ) {
			// only search the range actually filled with values.
			// lower index can be specified, if known
			final int index = Arrays.binarySearch( weights , from , size , weight );
			return index >= 0 ? index : - 1 - index;
		}

		private synchronized void insert(
				final int friend,
				final float weight,
				int insertionPoint ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "insert "+friend+" at "+insertionPoint+" in array of size "+friends.length+" with data size "+size );
			}

			if ( size == maximalSize && insertionPoint == 0 ) {
				// full: do not add a low utility friend
				return;
			}

			if ( size == maximalSize ) {
				// delete lowest entry
				for ( int i=0; i < size - 1; i++ ) {
					friends[ i ] = friends[ i + 1 ];
					weights[ i ] = weights[ i + 1 ];
				}

				// shift  insertion point, as array shifted
				insertionPoint--;
			}
			else {
				if ( size == friends.length ) {
					final int newSize = Math.min( maximalSize , size * 2 );
					friends = Arrays.copyOf( friends , newSize );
					weights = Arrays.copyOf( weights , newSize );
				}
				size++;
			}

			for ( int i = size - 1; i > insertionPoint; i-- ) {
				friends[ i ] = friends[ i - 1 ];
				weights[ i ] = weights[ i - 1 ];
			}

			friends[ insertionPoint ] = friend;
			weights[ insertionPoint ] = weight;
		}

		public synchronized void trim() {
			final int newSize = Math.max( 1 , size );
			friends = Arrays.copyOf( friends , newSize );
			weights = Arrays.copyOf( weights , newSize );
		}
	}
}

