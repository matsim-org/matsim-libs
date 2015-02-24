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
import java.util.Iterator;
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

	public synchronized void addBidirectionalTie(
			final int ego,
			final int alter,
			final double weight ) {
		if ( weight < lowestAllowedWeight ) return;
		if ( ego == alter ) throw new IllegalArgumentException( "cannot create ties from one ego to himself!" );

		final float fweight = (float) weight; // TODO check overflow?
		final int insertionPointE = alters[ ego ].getInsertionPoint( fweight );
		final int insertionPointA = alters[ alter ].getInsertionPoint( fweight );

		// only add if both alters want it
		if ( !alters[ ego ].acceptInsertion( alter , fweight , insertionPointE ) ||
				!alters[ alter ].acceptInsertion( ego , fweight , insertionPointA ) ) return;

		addMonodirectionalTie( ego , alter , fweight , insertionPointE );
		addMonodirectionalTie( alter , ego , fweight , insertionPointA );
	}

	private void addMonodirectionalTie(
			final int ego,
			final int alter,
			final float fweight,
			final int insertionPoint ) {
		final int removed = alters[ ego ].insert( alter , fweight , insertionPoint );
		assert removed != alter; // addition not accepted in this case
		if ( removed >= 0 ) alters[ removed ].remove( ego );
	}

	/**
	 * Memory optimisation: shrinks storing arrays so that they do not contain
	 * unused slots.
	 */
	public void trim( final int ego ) {
		this.alters[ego].trim();
	}

	public double getLowestAllowedWeight() {
		return lowestAllowedWeight;
	}

	public int getMaximalSize() {
		return maximalSize;
	}

	public int getNEgos() {
		return alters.length;
	}

	public Iterable<WeightedAlter> getAlters( final int ego ) {
		return new AlterIterable( alters[ego] );
	}

	public void trimAll() {
		for ( int i = 0; i < alters.length; i++ ) {
			trim( i );
		}
	}

	/*for tests*/ boolean contains( int ego , int alter ) {
		return alters[ ego ].indexOfAlter( alter ) >= 0;
	}

	public Set<Id<Person>> getAltersOverWeight( final int ego , final double weight , final IndexedPopulation population ) {
		// if ( weight < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+weight+" is lower than lowest stored weight "+lowestAllowedWeight );
		return alters[ego].getAltersOverWeight( weight, population );
	}

	public int[] getAltersOverWeight( final int ego , final double weight ) {
		// if ( weight < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+weight+" is lower than lowest stored weight "+lowestAllowedWeight );
		return alters[ego].getAltersOverWeight( weight );
	}

	public void fillWithAltersOverWeight( final TIntSet set , final int ego , final double weight ) {
		// if ( weight < lowestAllowedWeight ) throw new IllegalArgumentException( "weight "+weight+" is lower than lowest stored weight "+lowestAllowedWeight );
		alters[ego].fillWithAltersOverWeight( set, weight );
	}

	// for tests
	/*package*/int getSize( final int ego ) {
		return alters[ego].size;
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
			this.friends = new int[initialSize];
			this.weights = new float[initialSize];
			Arrays.fill( weights, Float.POSITIVE_INFINITY );
		}

		public void remove( int alter ) {
			final int index = indexOfAlter( alter );
			if ( index < 0 ) throw new IllegalStateException( "no alter "+alter );
			
			size--;
			for ( int i = index; i < size; i++ ) {
				friends[ i ] = friends[ i + 1 ];
				weights[ i ] = weights[ i + 1 ];
			}
		}

		private int indexOfAlter( final int alter ) {
			for ( int i=0; i < size; i++ ) {
				if ( friends[ i ] == alter ) return i;
			}
			return -1;
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

		private synchronized boolean acceptInsertion(
				final int friend,
				final float weight,
				int insertionPoint ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "check if inserting "+friend+" at "+insertionPoint+" in array of size "+friends.length+" with data size "+size );
			}

			if ( size == maximalSize && insertionPoint == 0 ) {
				// full: do not add a low utility friend
				if ( log.isTraceEnabled() ) log.trace( "no space left and value too bad. nothing done" );
				return false;
			}

			// is the friend already in the array?
			for ( int i = insertionPoint - 1; i >= 0 && weights[ i ] == weight; i-- ) {
				if ( friends[ i ] == friend ) {
					if ( log.isTraceEnabled() ) log.trace( "friend already present. nothing done" );
					return false;
				}
			}
			for ( int i = insertionPoint; i >= 0 && i < size && weights[ i ] <= weight; i++ ) {
				if ( friends[ i ] == friend ) {
					if ( log.isTraceEnabled() ) log.trace( "friend already present. nothing done" );
					return false;
				}
			}

			if ( log.isTraceEnabled() ) {
				log.trace( "accept addition. State before addition:" );
				traceState();
			}

			return true;
		}


		private synchronized int insert(
				final int friend,
				final float weight,
				int insertionPoint ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "insert "+friend+" at "+insertionPoint+" in array of size "+friends.length+" with data size "+size );
				log.trace( "State before addition:" );
				traceState();
			}

			int removed = -1;
			if ( size == maximalSize ) {
				// delete lowest entry
				removed = friends[ 0 ];
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

			if ( log.isTraceEnabled() ) {
				log.trace( "entry added. State after addition:" );
				traceState();
			}

			return removed;
		}

		private void traceState() {
			if ( log.isTraceEnabled() ) {
				log.trace( "friends: "+Arrays.toString( Arrays.copyOf( friends , size ) ) );
				log.trace( "weights: "+Arrays.toString( Arrays.copyOf( weights , size ) ) );
			}
		}

		public synchronized void trim() {
			final int newSize = Math.max( 1 , size );
			friends = Arrays.copyOf( friends , newSize );
			weights = Arrays.copyOf( weights , newSize );
		}

		@Override
		public boolean equals( final Object other ) {
			if ( !(other instanceof WeightedFriends) ) return false;

			return size == ((WeightedFriends) other).size &&
				Arrays.equals( Arrays.copyOf( friends , size ) , Arrays.copyOf( ((WeightedFriends) other).friends , size ) ) &&
				Arrays.equals( Arrays.copyOf( weights , size ) , Arrays.copyOf( ((WeightedFriends) other).weights , size ) );
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode( Arrays.copyOf( friends , size ) ) +
				Arrays.hashCode( Arrays.copyOf( weights , size ) );
		}

		@Override
		public String toString() {
			return "[WeightedFriends: size="+size+"; friends="+
				Arrays.toString( Arrays.copyOf( friends , size ) )+"; weights="+
				Arrays.toString( Arrays.copyOf( weights , size ) )+"]";
		}
	}

	private static final class AlterIterable implements Iterable<WeightedAlter> {
		private final WeightedFriends alters;

		private AlterIterable( final WeightedFriends alters ) {
			this.alters = alters;
		}

		@Override
		public Iterator<WeightedAlter> iterator() {
			return new Iterator<WeightedAlter>() {
				int index=0;

				@Override
				public boolean hasNext() {
					return index < alters.size;
				}

				@Override
				public WeightedAlter next() {
					return new WeightedAlter(
							alters.friends[ index ],
							alters.weights[ index++ ] );
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public boolean equals( final Object other ) {
			if ( !(other instanceof AlterIterable) ) return false;

			return alters.equals( ((AlterIterable) other).alters );
		}

		@Override
		public int hashCode() {
			return alters.hashCode();
		}

		@Override
		public String toString() {
			return "Iter->"+alters;
		}
	}

	public static class WeightedAlter {
		public final int alter;
		public final double weight;

		private WeightedAlter(final int alter, final double weight) {
			this.alter = alter;
			this.weight = weight;
		}
	}
}

