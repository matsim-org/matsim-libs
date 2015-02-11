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

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;

/**
 * @author thibautd
 */
public class DoublyWeightedSocialNetwork {
	private final DoublyWeightedFriends[] alters;
	private final double lowestAllowedFirstWeight;
	private final double lowestAllowedSecondWeight;
	private int initialSize;

	public DoublyWeightedSocialNetwork(
			final int initialSize,
			final double lowestWeight,
			final int populationSize ) {
		this.initialSize  = initialSize;
		this.lowestAllowedFirstWeight = lowestWeight;
		this.lowestAllowedSecondWeight = lowestWeight;
		this.alters = new DoublyWeightedFriends[ populationSize ];
		for ( int i = 0; i < populationSize; i++ ) {
			this.alters[ i ] = new DoublyWeightedFriends( initialSize );
		}
	}

	public DoublyWeightedSocialNetwork(
			final double lowestWeight,
			final int populationSize ) {
		this( 20 , lowestWeight , populationSize );
	}

	public void clear() {
		for ( int i = 0; i < alters.length; i++ ) {
			this.alters[ i ] = new DoublyWeightedFriends( initialSize );
		}
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

	public void addBidirectionalTie(
			final int ego,
			final int alter,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) return;
		if ( weight2 < lowestAllowedSecondWeight ) return;
		alters[ ego ].add( alter , weight1 , weight2 );
		alters[ alter ].add( ego , weight1 , weight2 );
	}

	public void addMonodirectionalTie(
			final int ego,
			final int alter,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) return;
		if ( weight2 < lowestAllowedSecondWeight ) return;
		alters[ ego ].add( alter , weight1 , weight2 );
	}


	public TIntSet getAltersOverWeights(
			final int ego,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) throw new IllegalArgumentException( "first weight "+weight1+" is lower than lowest stored weight "+lowestAllowedFirstWeight );
		if ( weight2 < lowestAllowedSecondWeight ) throw new IllegalArgumentException( "second weight "+weight2+" is lower than lowest stored weight "+lowestAllowedSecondWeight );
		return alters[ ego ].getAltersOverWeights( weight1 , weight2 );
	}

	public void fillWithAltersOverWeights(
			final TIntSet set,
			final int ego,
			final double weight1,
			final double weight2 ) {
		if ( weight1 < lowestAllowedFirstWeight ) throw new IllegalArgumentException( "first weight "+weight1+" is lower than lowest stored weight "+lowestAllowedFirstWeight );
		if ( weight2 < lowestAllowedSecondWeight ) throw new IllegalArgumentException( "second weight "+weight2+" is lower than lowest stored weight "+lowestAllowedSecondWeight );
		alters[ ego ].fillWithAltersOverWeights( set , weight1 , weight2 );
	}


	// for tests
	/*package*/ int getSize( final int ego ) {
		return alters[ ego ].size;
	}

	// point quad-tree
	// No check is done that is is balanced!
	// should be ok, as agents are got in random order
	private static final class DoublyWeightedFriends {
		private int[] friends;

		// use float and short for memory saving
		private float[] weights1;
		private float[] weights2;

		private short[] childSE;
		private short[] childSW;
		private short[] childNE;
		private short[] childNW;

		// cannot exceed the maximum value of the "children" arrays:
		// set to short, even if it might have only limited impact on
		// memory
		// "shift" the size to store 2 times more values
		private static final short MIN_SHIFTED_INDEX = Short.MIN_VALUE + 1;
		private short shiftedSize = MIN_SHIFTED_INDEX;

		public DoublyWeightedFriends(final int initialSize) {
			this.friends = new int[ initialSize ];

			this.weights1 = new float[ initialSize ];
			this.weights2 = new float[ initialSize ];

			Arrays.fill( weights1 , Float.POSITIVE_INFINITY );
			Arrays.fill( weights2 , Float.POSITIVE_INFINITY );

			this.childSE = new short[ initialSize ];
			this.childSW = new short[ initialSize ];
			this.childNE = new short[ initialSize ];
			this.childNW = new short[ initialSize ];

			Arrays.fill( childSE , shift( -1 ) );
			Arrays.fill( childSW , shift( -1 ) );
			Arrays.fill( childNE , shift( -1 ) );
			Arrays.fill( childNW , shift( -1 ) );
		}

		private int unshift( final short shiftedIndex ) {
			return shiftedIndex - MIN_SHIFTED_INDEX;
		}

		private short shift( final int index ) {
			final int shifted = index + MIN_SHIFTED_INDEX;
			if ( shifted < Short.MIN_VALUE ) throw new IllegalArgumentException( "underflow" );
			if ( shifted > Short.MAX_VALUE ) throw new IllegalArgumentException( "overflow" );
			return (short) shifted;
		}

		public synchronized void add(
				final int friend,
				final double firstWeight,
				final double secondWeight ) {
			add( friend , (float) firstWeight , (float) secondWeight );
		}

		public synchronized void add(
				final int friend,
				final float firstWeight,
				final float secondWeight ) {
			if ( unshift( shiftedSize ) == 0 ) {
				// first element is the head: special case...
				friends[ 0 ] = friend;

				weights1[ 0 ] = firstWeight;
				weights2[ 0 ] = secondWeight;
			}
			else {
				final int parent = searchParentLeaf( 0, firstWeight, secondWeight );

				final int index = unshift( shiftedSize );
				if ( index == friends.length ) expand();

				final short[] quadrant = getQuadrant( parent , firstWeight, secondWeight );
				friends[ index ] = friend;

				weights1[ index ] = firstWeight;
				weights2[ index ] = secondWeight;

				quadrant[ parent ] = shiftedSize;
			}
			shiftedSize++;
		}

		private synchronized void expand() {
			final int newLength = 2 * friends.length;
			friends = Arrays.copyOf( friends , newLength );

			weights1 = Arrays.copyOf( weights1 , newLength );
			weights2 = Arrays.copyOf( weights2 , newLength );

			Arrays.fill( weights1 , unshift( shiftedSize ) , newLength , Float.POSITIVE_INFINITY );
			Arrays.fill( weights2 , unshift( shiftedSize ) , newLength , Float.POSITIVE_INFINITY );

			childSE = Arrays.copyOf( childSE , newLength );
			childSW = Arrays.copyOf( childSW , newLength );
			childNE = Arrays.copyOf( childNE , newLength );
			childNW = Arrays.copyOf( childNW , newLength );

			Arrays.fill( childSE , unshift( shiftedSize ) , newLength , shift( -1 ) );
			Arrays.fill( childSW , unshift( shiftedSize ) , newLength , shift( -1 ) );
			Arrays.fill( childNE , unshift( shiftedSize ) , newLength , shift( -1 ) );
			Arrays.fill( childNW , unshift( shiftedSize ) , newLength , shift( -1 ) );
		}

		private int searchParentLeaf(
				final int head,
				final float firstWeight,
				final float secondWeight ) {
			short[] quadrant = getQuadrant( head, firstWeight, secondWeight );

			return quadrant[ head ] == shift( -1 ) ? head :
				searchParentLeaf( unshift( quadrant[ head ] ), firstWeight, secondWeight );
		}

		private short[] getQuadrant(
				final int head,
				final float firstWeight,
				final float secondWeight ) {
			if ( firstWeight > weights1[ head ] ) {
				return secondWeight > weights2[ head ] ? childNE : childSE;
			}
			return secondWeight > weights2[ head ] ? childNW : childSW;
		}

		public TIntSet getAltersOverWeights(
				final double firstWeight,
				final double secondWeight) {
			final TIntSet alters = new TIntHashSet();

			fillWithGreaterPoints( 0, alters, firstWeight, secondWeight );

			return alters;
		}

		public void fillWithAltersOverWeights(
				final TIntSet alters,
				final double firstWeight,
				final double secondWeight) {
			fillWithGreaterPoints( 0, alters, firstWeight, secondWeight );
		}

		private void fillWithGreaterPoints(
				final int head,
				final TIntSet alters,
				final double firstWeight,
				final double secondWeight ) {
			if ( head == -1 ) return; // we fell of the tree!

			if ( weights1[ head ] > firstWeight && weights2[ head ] > secondWeight ) {
				alters.add( friends[ head ] );
				fillWithGreaterPoints( unshift( childSW[ head ] ) , alters , firstWeight , secondWeight );
			}
			if ( weights1[ head ] > firstWeight ) {
				fillWithGreaterPoints( unshift( childNW[ head ] ) , alters , firstWeight , secondWeight );
			}
			if ( weights2[ head ] > secondWeight ) {
				fillWithGreaterPoints( unshift( childSE[ head ] ) , alters , firstWeight , secondWeight );
			}
			// always look to the NW
			fillWithGreaterPoints( unshift( childNE[ head ] ) , alters , firstWeight , secondWeight );
		}

		public synchronized void trim() {
			final int newSize = Math.max( 1 , unshift( shiftedSize ) );
			friends = Arrays.copyOf( friends , newSize );

			weights1 = Arrays.copyOf( weights1 , newSize );
			weights2 = Arrays.copyOf( weights2 , newSize );

			childSE = Arrays.copyOf( childSE , newSize );
			childSW = Arrays.copyOf( childSW , newSize );
			childNE = Arrays.copyOf( childNE , newSize );
			childNW = Arrays.copyOf( childNW , newSize );
		}
	}
}

