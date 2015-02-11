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
import gnu.trove.stack.TShortStack;
import gnu.trove.stack.array.TShortArrayStack;

import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
public class DoublyWeightedSocialNetwork {
	private static final Logger log =
		Logger.getLogger(DoublyWeightedSocialNetwork.class);

	private final DoublyWeightedFriends[] alters;
	private final double lowestAllowedFirstWeight;
	private final double lowestAllowedSecondWeight;
	private final int initialSize;
	private final short maxSize;

	public DoublyWeightedSocialNetwork(
			final int initialSize,
			final double lowestWeight,
			final int populationSize,
			final int maxSize ) {
		this.initialSize  = initialSize;
		this.maxSize = (short) maxSize;
		this.lowestAllowedFirstWeight = lowestWeight;
		this.lowestAllowedSecondWeight = lowestWeight;
		this.alters = new DoublyWeightedFriends[ populationSize ];
		for ( int i = 0; i < populationSize; i++ ) {
			this.alters[ i ] = new DoublyWeightedFriends( initialSize , this.maxSize );
		}
	}

	public DoublyWeightedSocialNetwork(
			final double lowestWeight,
			final int populationSize ) {
		this( 20 , lowestWeight , populationSize , Short.MAX_VALUE );
	}

	public void clear() {
		for ( int i = 0; i < alters.length; i++ ) {
			this.alters[ i ] = new DoublyWeightedFriends( initialSize , maxSize );
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
		private short size = 0;

		private final short maxSize;

		public DoublyWeightedFriends(
				final int initialSize,
				final short maxSize) {
			this.maxSize = maxSize;
			this.friends = new int[ initialSize ];

			this.weights1 = new float[ initialSize ];
			this.weights2 = new float[ initialSize ];

			Arrays.fill( weights1 , Float.POSITIVE_INFINITY );
			Arrays.fill( weights2 , Float.POSITIVE_INFINITY );

			this.childSE = new short[ initialSize ];
			this.childSW = new short[ initialSize ];
			this.childNE = new short[ initialSize ];
			this.childNW = new short[ initialSize ];

			Arrays.fill( childSE , (short) -1 );
			Arrays.fill( childSW , (short) -1 );
			Arrays.fill( childNE , (short) -1 );
			Arrays.fill( childNW , (short) -1 );
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
			if ( size == 0 ) {
				// first element is the head: special case...
				friends[ 0 ] = friend;

				weights1[ 0 ] = firstWeight;
				weights2[ 0 ] = secondWeight;

				size++;
			}
			else if ( size != maxSize ) {
				final int parent = searchParentLeaf( 0, firstWeight, secondWeight );

				if ( size == friends.length ) expand();

				final short[] quadrant = getQuadrant( parent , firstWeight, secondWeight );
				friends[ size ] = friend;

				weights1[ size ] = firstWeight;
				weights2[ size ] = secondWeight;

				quadrant[ parent ] = size;

				size++;
			}
			else {
				// max size reached: replace value with smallest secondary weight
				// (primary weight always "make sense" in preprocessed model runner)
				// 1 - find element with smallest *secondary* weight
				final short[] smallestSecondaryIndexAndParent = searchSmallestSecondaryIndex();
				final short smallestSecondaryIndex = smallestSecondaryIndexAndParent[ 0 ];
				final short smallestSecondaryParent = smallestSecondaryIndexAndParent[ 1 ];

				// 2 - check if new element better. if not, abort.
				if ( weights2[smallestSecondaryIndex] >= secondWeight ) return;

				// 3 - remove element to replace:
				//     a - reconnect the tree
				remove( smallestSecondaryIndex , smallestSecondaryParent );
				assert size == maxSize -1;

				add( friend , firstWeight , secondWeight );
			}
		}

		private void remove(
				final short toRemoveIndex,
				final short toRemoveParent ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "remove element at "+toRemoveIndex );
			}
			// rebuild the whole subtree

			final TShortStack toReaddStack = new TShortArrayStack();

			// shift values by one
			if ( childNE[ toRemoveIndex ] != -1 ) toReaddStack.push( shift( childNE[ toRemoveIndex ] , toRemoveIndex ) );
			if ( childNW[ toRemoveIndex ] != -1 ) toReaddStack.push( shift( childNW[ toRemoveIndex ] , toRemoveIndex ) );
			if ( childSE[ toRemoveIndex ] != -1 ) toReaddStack.push( shift( childSE[ toRemoveIndex ] , toRemoveIndex ) );
			if ( childSW[toRemoveIndex] != -1 ) toReaddStack.push( shift( childSW[toRemoveIndex] , toRemoveIndex ) );

			if ( toRemoveParent != -1 ) {
				// separate subtree
				( childNW[toRemoveParent] == toRemoveIndex ? childNW :
				  childNE[toRemoveParent] == toRemoveIndex ? childNE :
				  childSE[toRemoveParent] == toRemoveIndex ? childSE :
				  childSW )[toRemoveParent] = -1;
			}

			size--;

			// fill gap
			for ( int i = toRemoveIndex; i < size; i++ ) {
				friends[i] = friends[i + 1];

				weights1[i] = weights1[i + 1];
				weights2[i] = weights2[i + 1];

				childNE[i] = childNE[i + 1];
				childNW[i] = childNW[i + 1];
				childSE[i] = childSE[i + 1];
				childSW[i] = childSW[i + 1];
			}

			// update pointers
			for ( int i = 0; i < size; i++ ) {
				if ( childNE[i] >= toRemoveIndex ) childNE[i]--;
				if ( childNW[i] >= toRemoveIndex ) childNW[i]--;
				if ( childSE[i] >= toRemoveIndex ) childSE[i]--;
				if ( childSW[i] >= toRemoveIndex ) childSW[i]--;
			}

			// "readd" to the tree.
			final int treeHead = toRemoveParent >= 0 ? toRemoveParent : 0;
			while ( toReaddStack.size() > 0 ) {
				final short toReadd = toReaddStack.pop();

				if ( childNE[toReadd] != -1 ) toReaddStack.push( childNE[toReadd] );
				if ( childNW[toReadd] != -1 ) toReaddStack.push( childNW[toReadd] );
				if ( childSE[toReadd] != -1 ) toReaddStack.push( childSE[toReadd] );
				if ( childSW[toReadd] != -1 ) toReaddStack.push( childSW[toReadd] );

				childNE[toReadd] = -1;
				childNW[toReadd] = -1;
				childSE[toReadd] = -1;
				childSW[toReadd] = -1;

				if ( toReadd != 0 ) {
					// find a new daddy
					final int parent = searchParentLeaf( treeHead, weights1[toReadd], weights2[toReadd] );
					final short[] quadrant = getQuadrant( parent, weights1[toReadd], weights2[toReadd] );
					quadrant[parent] = toReadd;
				}
			}

			if ( log.isTraceEnabled() ) {
				log.trace( "remove element at " + toRemoveIndex + " DONE" );
			}
		}

		private static short shift( final short i , final int index ) {
			return i > index ? (short) (i - 1) : i;
		}

		private short[] searchSmallestSecondaryIndex() {
			float currentMin = Float.POSITIVE_INFINITY;
			short currentMinIndex = -1;
			short currentMinParent = -1;

			final TShortStack indexStack = new TShortArrayStack();
			indexStack.push( (short) 0 );

			final TShortStack parentStack = new TShortArrayStack();
			parentStack.push( (short) -1 );

			while ( indexStack.size() > 0 ) {
				final short currentIndex = indexStack.pop();
				final short currentParent = parentStack.pop();

				if ( weights2[ currentIndex ] < currentMin ) {
					currentMin = weights2[ currentIndex ];
					currentMinIndex = currentIndex;
					currentMinParent = currentParent;
				}

				if ( childSE[ currentIndex ] != -1 ) {
					indexStack.push( childSE[ currentIndex ] );
					parentStack.push( currentIndex );
				}
				if ( childSW[ currentIndex ] != -1 ) {
					indexStack.push( childSW[ currentIndex ] );
					parentStack.push( currentIndex );
				}
			}

			return new short[]{ currentMinIndex , currentMinParent };
		}

		private synchronized void expand() {
			final int newLength = Math.min( maxSize , 2 * friends.length );
			friends = Arrays.copyOf( friends , newLength );

			weights1 = Arrays.copyOf( weights1 , newLength );
			weights2 = Arrays.copyOf( weights2 , newLength );

			Arrays.fill( weights1 , size , newLength , Float.POSITIVE_INFINITY );
			Arrays.fill( weights2 , size , newLength , Float.POSITIVE_INFINITY );

			childSE = Arrays.copyOf( childSE , newLength );
			childSW = Arrays.copyOf( childSW , newLength );
			childNE = Arrays.copyOf( childNE , newLength );
			childNW = Arrays.copyOf( childNW , newLength );

			Arrays.fill( childSE , size , newLength , (short) -1 );
			Arrays.fill( childSW , size , newLength , (short) -1 );
			Arrays.fill( childNE , size , newLength , (short) -1 );
			Arrays.fill( childNW , size , newLength , (short) -1 );
		}

		private int searchParentLeaf(
				final int head,
				final float firstWeight,
				final float secondWeight ) {
			short[] quadrant = getQuadrant( head, firstWeight, secondWeight );

			return quadrant[ head ] == -1 ? head : searchParentLeaf( quadrant[head], firstWeight, secondWeight );
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
				fillWithGreaterPoints( childSW[ head ] , alters , firstWeight , secondWeight );
			}
			if ( weights1[ head ] > firstWeight ) {
				fillWithGreaterPoints( childNW[ head ] , alters , firstWeight , secondWeight );
			}
			if ( weights2[ head ] > secondWeight ) {
				fillWithGreaterPoints( childSE[ head ] , alters , firstWeight , secondWeight );
			}
			// always look to the NW
			fillWithGreaterPoints( childNE[ head ] , alters , firstWeight , secondWeight );
		}

		public synchronized void trim() {
			final int newSize = Math.max( 1 , size );
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

