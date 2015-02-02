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

	private final int initialSize;

	public WeightedSocialNetwork(
			final int initialSize,
			final double lowestWeight,
			final int populationSize ) {
		this.lowestAllowedWeight = lowestWeight;
		this.alters = new WeightedFriends[ populationSize ];
		this.initialSize = initialSize;

		for ( int i=0; i < this.alters.length; i++ ) {
			this.alters[ i ] = new WeightedFriends( initialSize );
		}
	}

	public WeightedSocialNetwork(
			final double lowestWeight,
			final int populationSize ) {
		this( 20 , lowestWeight , populationSize );
	}

	public void clear() {
		for ( int i=0; i < this.alters.length; i++ ) {
			this.alters[ i ] = new WeightedFriends( initialSize );
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

	public void addAltersOverWeight(
			final TIntSet set,
			final int ego,
			final double weight ) {
		if ( weight < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+weight+" is lower than lowest stored weight "+lowestAllowedWeight );
		alters[ ego ].addAltersOverWeight( set , weight );
	}

	/* unused
	public Set<Id<Person>> getAltersInWeightInterval(
			final Id<Person> ego,
			final double low,
			final double high ) {
		if ( low < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+low+" is lower than lowest stored weight "+lowestAllowedWeight );
		if ( low > high ) throw new IllegalArgumentException( "lower bound "+low+" is higher than higher bound "+high );
		return altersMap.get( ego ).getAltersIn( low , high );
	}
	*/

	// for tests
	/*package*/ int getSize( final int ego ) {
		return alters[ ego ].size;
	}

	private static final class WeightedFriends {
		private int[] friends;
		// use float instead of double for saving memory.
		// TODO: check robustness of the results facing this...
		private float weights[];
		// as such, using short here might look as overdoing...
		// but we have one such structure per agent: this might make sense.
		private short size = 0;

		public WeightedFriends( final int initialSize ) {
			this.friends = new int[ initialSize ];
			this.weights = new float[ initialSize ];
			Arrays.fill( weights , Float.POSITIVE_INFINITY  );
		}

		public synchronized void add( final int friend , final double weight ) {
			final float fweight = (float) weight; // TODO check overflow?
			final int insertionPoint = getInsertionPoint( fweight );
			insert( friend, insertionPoint );
			insert( fweight, insertionPoint );
			size++;
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

		public void addAltersOverWeight(
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

		private synchronized void insert( int friend , int insertionPoint ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "insert "+friend+" at "+insertionPoint+" in array of size "+friends.length+" with data size "+size );
			}

			if ( size == friends.length ) {
				friends = Arrays.copyOf( friends , size * 2 );
			}

			for ( int i = size; i > insertionPoint; i-- ) {
				friends[ i ] = friends[ i - 1 ];
			}

			friends[ insertionPoint ] = friend;
		}

		private synchronized void insert( float weight , int insertionPoint ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "insert "+weight+" at "+insertionPoint+" in array of size "+weights.length+" with data size "+size );
			}

			if ( size == weights.length ) {
				weights = Arrays.copyOf( weights , size * 2 );
				for ( int i = size; i < weights.length; i++ ) weights[ i ] = Float.POSITIVE_INFINITY;
			}

			for ( int i = size; i > insertionPoint; i-- ) {
				weights[ i ] = weights[ i - 1 ];
			}

			weights[ insertionPoint ] = weight;
		}

		public synchronized void trim() {
			final int newSize = Math.max( 1 , size );
			friends = Arrays.copyOf( friends , newSize );
			weights = Arrays.copyOf( weights , newSize );
		}
	}
}

