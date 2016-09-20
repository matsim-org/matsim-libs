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
package playground.thibautd.utils;

import eu.eunoiaproject.examples.schedulebasedteleportation.Run;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.function.ToDoubleBiFunction;

/**
 * Basic implementation of a KD-Tree, to answer spatial queries in spaces of arbitrary dimensions.
 * Balance is not enforced (yet).
 *
 * @author thibautd
 */
public class KDTree<T> {
	// used only to balance the tree
	private final Random random = MatsimRandom.getLocalInstance();
	private static final int SUBLIST_SIZE_MEDIAN = 100;

	private final int nDimensions;
	private final Node<T> root = new Node<>( 0 );

	public KDTree( final int nDimensions ) {
		this.nDimensions = nDimensions;
	}

	/**
	 * simple add: might break balance! Recommended to build the full tree at construction. No rebalancing.
	 * @param coord
	 * @param value
	 */
	public void add( final double[] coord , final T value ) {
		root.add( value , coord );
	}

	public void add( Collection<Tuple<T,double[]>> points ) {

		final Queue<AddFrame<T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );

		// copy parameter list as it is modified in place
		stack.add( new AddFrame<>( root, new ArrayList<>( points ) ) );

		while ( !stack.isEmpty() ) {
			final AddFrame<T> currentFrame = stack.poll();

			if ( currentFrame.toAdd.isEmpty() ) continue;

			final Tuple<T,double[]> median = median( currentFrame.node.dimension , currentFrame.toAdd );
			final double medianValue = median.getSecond()[ currentFrame.node.dimension ];

			final int newDimension = (currentFrame.node.dimension + 1) % nDimensions;
			final AddFrame<T> leftFrame =
					new AddFrame<>(
							new Node<T>( newDimension ),
							new ArrayList<>( currentFrame.toAdd.size() / 2 + 1 ) );
			final AddFrame<T> rightFrame =
					new AddFrame<>(
							new Node<T>( newDimension ),
							new ArrayList<>( currentFrame.toAdd.size() / 2 + 1 ) );

			currentFrame.node.left = leftFrame.node;
			currentFrame.node.right = rightFrame.node;

			for ( Tuple<T,double[]> v : currentFrame.toAdd ) {
				if ( v == median && currentFrame.node.value == null ) {
					currentFrame.node.value = v.getFirst();
					currentFrame.node.coordinate = v.getSecond();

					if ( currentFrame.node.coordinate.length != nDimensions ) throw new IllegalArgumentException( "wrong dimensionality" );
				}
				else if ( v.getSecond()[ currentFrame.node.dimension ] < medianValue ) {
					leftFrame.toAdd.add( v );
				}
				else {
					rightFrame.toAdd.add( v );
				}
			}

			stack.add( leftFrame );
			stack.add( rightFrame );
		}
	}

	private <E> List<E> sublist( final List<E> l ) {
		if ( l.size() < SUBLIST_SIZE_MEDIAN ) return l;

		final List<E> sublist = new ArrayList<>();
		final double prob = ((double) SUBLIST_SIZE_MEDIAN) / l.size();

		for ( E e : l ) {
			if ( random.nextDouble() < prob ) sublist.add( e );
		}

		return sublist;
	}

	private Tuple<T, double[]> median( final int dim , final List<Tuple<T,double[]>> l ) {
		// very simple approximation: take median of a sublist, using standard sort algorithm
		final List<Tuple<T, double[]>> sublist = sublist( l );
		Collections.sort( sublist , (t1,t2) -> Double.compare( t1.getSecond()[ dim ] , t2.getSecond()[ dim ] ) );
		return sublist.get( sublist.size() / 2 );
	}

	public Collection<T> getBox( final double[] lowers , final double[] uppers ) {
		if ( !allLower( lowers , uppers ) ) {
			throw new IllegalArgumentException( "invalid bounding box low="+Arrays.toString( lowers )+", high="+Arrays.toString( uppers ) );
		}
		final Collection<T> result = new ArrayList<>();

		final Queue<Node<T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		while ( !stack.isEmpty() ) {
			final Node<T> current = stack.poll();
			if ( current.value == null ) continue;

			if ( allLower( lowers , current.coordinate ) && allLower( current.coordinate , uppers ) ) {
				// in the box
				result.add( current.value );
				stack.add( current.left );
				stack.add( current.right );
			}
			else if ( isLeft( current.dimension , current.coordinate , lowers ) ) {
				// current point is "left" of the box: only go right
				stack.add( current.right );
			}
			else if ( isLeft( current.dimension , uppers , current.coordinate ) ) {
				// current point is "right" of the box: only go left
				stack.add( current.left );
			}
			else {
				// current point is outside the box, but inside its projection on the current axis: continue
				stack.add( current.left );
				stack.add( current.right );
			}
		}

		return result;
	}


	private boolean allLower( final double[] low, final double[] high ) {
		if ( low.length != high.length ) throw new IllegalArgumentException( "uncompatible dimensionalities" );

		for ( int i = 0 ; i < low.length; i++ ) {
			if ( low[ i ] > high[ i ] ) return false;
		}

		return true;
	}

	public T getClosestEuclidean( final double[] coord ) {
		return getClosest( coord , KDTree::euclidean );
	}

	private static double euclidean( double[] c1 , double[] c2 ) {
		double d = 0;

		for (int i=0; i < c1.length; i++ ) {
			d += Math.pow( c1[ i ] - c2[ i ] , 2 );
		}

		return d;
	}

	public T getClosest( final double[] coord , final ToDoubleBiFunction<double[],double[]> distance ) {
		T closest = root.value;
		double bestDist = distance.applyAsDouble( coord , root.coordinate );

		final Queue<Node<T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		// First find a good candidate without too much effort: search for the "insertion point", and check the distance
		// on the way (having a good start will improve pruning in the later stage)
		while( !stack.isEmpty() ) {
			final Node<T> current = stack.poll();
			if ( current.value == null ) continue;
			final double currentDist = distance.applyAsDouble( coord , current.coordinate );

			if ( currentDist < bestDist ) {
				closest = current.value;
				bestDist = currentDist;
			}

			final boolean isLeft = isLeft( current.dimension , coord , current.coordinate );

			if ( isLeft ) stack.add( current.left );
			else stack.add( current.right );
		}

		// now, go down the full tree, pruning half-spaces further away than the current best distance
		stack.add( root );
		while ( !stack.isEmpty() ) {
			final Node<T> current = stack.poll();
			if ( current.value == null ) continue;

			// if the cutting hyperplane is further away than the best distance, only explore on "our side" of it
			final double[] projected = Arrays.copyOf( coord , coord.length );
			projected[ current.dimension ] = current.coordinate[ current.dimension ];
			final double distanceToCuttingPlane = distance.applyAsDouble( projected , coord );

			if ( distanceToCuttingPlane > bestDist ) {
				final boolean isLeft = isLeft( current.dimension , current.coordinate , coord );

				// if we are to far left, only continue to the right, and vice versa
				if ( isLeft ) stack.add( current.right );
				else stack.add( current.left );
			}
			else {
				final double currentDist = distance.applyAsDouble( coord , current.coordinate );
				if ( currentDist < bestDist ) {
					closest = current.value;
					bestDist = currentDist;
				}

				// could take care of order to make pruning more probable
				stack.add( current.left );
				stack.add( current.right );
			}
		}

		return closest;
	}

	private static boolean isLeft( int dim , double[] coord , double[] of ) {
		return coord[ dim ] < of[ dim ];
	}

	private static class AddFrame<T> {
		Node<T> node;
		List<Tuple<T,double[]>> toAdd;

		public AddFrame( final Node<T> node, final List<Tuple<T, double[]>> toAdd ) {
			this.node = node;
			this.toAdd = toAdd;
		}
	}

	// TODO all of the recursive methods here could be transformed in a loop easily
	private static class Node<T> {
		private Node<T> left, right;
		private double[] coordinate;
		private T value;
		private final int dimension;

		private Node(
				final int dimension ) {
			this.dimension = dimension;
		}

		public void add( final T newValue , final double[] newCoordinate ) {
			if ( value != null && newCoordinate.length != coordinate.length ) {
				throw new IllegalArgumentException( "incompatible dimensionalities" );
			}

			if ( value == null ) {
				this.value = newValue;
				this.coordinate = newCoordinate;

				this.left = new Node<>( (dimension + 1) % coordinate.length );
				this.right = new Node<>( (dimension + 1) % coordinate.length );
				return;
			}

			final boolean isLeft = newCoordinate[ dimension ] < coordinate[ dimension ];
			if ( isLeft ) left.add( newValue , newCoordinate );
			else right.add( newValue , newCoordinate );
		}
	}
}
