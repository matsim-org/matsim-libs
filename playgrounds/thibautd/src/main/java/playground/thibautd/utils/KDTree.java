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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;

import static org.osgeo.proj4j.parser.Proj4Keyword.k;
import static playground.ivt.router.TripSoftCache.LocationType.coord;

/**
 * Basic implementation of a KD-Tree, to answer spatial queries in spaces of arbitrary dimensions.
 * Balance is not enforced (yet).
 *
 * @author thibautd
 */
public class KDTree<T> {
	private final Node<T> root = new Node<>( 0 );

	public KDTree( final Map<T,double[]> points ) {
		// TODO make it smarter to get a balanced tree
		// for instance, always sample N random remaining points, and add the median of those points
		for ( Map.Entry<T,double[]> e : points.entrySet() ) {
			root.add( e.getKey() , e.getValue() );
		}
	}

	public void add( final double[] coord , final T value ) {
		root.add( value , coord );
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

		/**
		 * goes down the tree until the predicate is false. It assumes children of a "false" node are also "false"
		 *
		 * @param toFill
		 * @param predicate
		 */
		public void fillUntilFalse( final Collection<T> toFill , final Predicate<double[]> predicate ) {
			if ( value == null ) return;
			if ( !predicate.test( coordinate ) ) return;

			toFill.add( value );

			left.fillUntilFalse( toFill , predicate );
			right.fillUntilFalse( toFill , predicate );
		}
	}
}
