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

import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.function.Predicate;
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
	private Node<T> root = new Node<>( 0 );
	private final Coordinate<T> coordinate;

	private final boolean rebalance;
	private int stepsToRebalance = 100;
	private int size = 0;

	public interface Coordinate<T> {
		double[] getCoord( T object );
	}

	/**
	 * Creates an empty kd-tree
	 * @param nDimensions the number of dimensions of the space
	 * @param coord designed to be a lambda, with a function that calls a getter on the T object itself to get the coordinate.
	 *              The implementation should be relatively efficient. Inefficient implementations will impact the performance
	 *              of adding elements, but should have no influence on the query performance.
	 */
	public KDTree( final int nDimensions , final Coordinate<T> coord ) {
		this( false , nDimensions , coord );
	}

	public KDTree(
			final boolean rebalance,
			final int nDimensions,
			final Coordinate<T> coord ) {
		this.rebalance = rebalance;
		this.nDimensions = nDimensions;
		this.coordinate = coord;
	}

	/**
	 * simple add: might break balance! Recommended to build the full tree at construction. No rebalancing.
	 * @param value
	 */
	public void add( final T value ) {
		add( root , Collections.singleton( value ) );
		rebalanceIfNecessary();
	}

	private void rebalanceIfNecessary() {
		if ( rebalance && stepsToRebalance-- == 0 ) {
			final Collection<T> all = getAll();
			root = new Node<>( 0 );
			size = 0;
			add( all );
		}
	}

	public Collection<T> getAll() {
		final Collection<T> all = new ArrayList<>();

		final Queue<Node<T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		while ( !stack.isEmpty() ) {
			final Node<T> current = stack.poll();

			if ( current.value == null ) continue;

			all.add( current.value );

			if ( !isLeaf( current ) ) {
				stack.add( current.left );
				stack.add( current.right );
			}
		}

		return all;
	}

	/**
	 * Here mainly to be able to get a random element.
	 * @param i
	 * @return
	 */
	public T get( int i ) {
		final Queue<Node<T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		int count = 0;
		while ( !stack.isEmpty() ) {
			final Node<T> current = stack.poll();

			if ( current.value == null ) continue;
			if ( count++ == i ) return current.value;

			if ( !isLeaf( current ) ) {
				stack.add( current.left );
				stack.add( current.right );
			}
		}

		throw new IllegalArgumentException( "Index too high" );
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int size() {
		return size;
	}

	public boolean remove( final T value ) {
		size--;
		Node<T> current = find( value );

		if ( current == null ) return false;

		while ( !isLeaf( current ) ) {
			if ( current.right.value != null ) {
				final Node<T> replacement = findMin( current.right , current.dimension );
				current.value = replacement.value;
				current.coordinate = replacement.coordinate;
				current = replacement;
			}
			else {
				// there is nothing on the right: find the best value in the left to make the left a right tree,
				// and proceed as above
				final Node<T> replacement = findMin( current.left , current.dimension );
				current.value = replacement.value;
				current.coordinate = replacement.coordinate;
				current.right = current.left;
				current.left = new Node<>( (current.dimension + 1) % 2 );
				current = replacement;
			}
		}

		current.value = null;
		current.coordinate = null;
		current.left = null;
		current.right = null;

		rebalanceIfNecessary();
		return true;
	}

	private Node<T> findMin( final Node<T> start, final int dimension ) {
		final Queue<Node<T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );

		Node<T> min = start;
		stack.add( start );

		while ( !stack.isEmpty() )  {
			final Node<T> current = stack.poll();

			assert (current.coordinate == null) == (current.value == null) : current;
			if ( current.coordinate == null ) continue;

			if ( current.coordinate[ dimension ] < min.coordinate[ dimension ] ) {
				min = current;
			}

			if ( isLeaf( current ) ) continue;
			stack.add( current.left );
			if ( current.dimension != dimension ) {
				// cannot prune
				stack.add( current.right );
			}
		}

		return min;
	}


	private boolean isLeaf( final Node<T> currentRoot ) {
		return currentRoot.left.value == null && currentRoot.right.value == null;
	}

	private Node<T> find( final T value ) {
		Node<T> current = root;

		final double[] searchedCoord = coordinate.getCoord( value );
		while ( true ) {
			if ( current.value == null ) return null;

			if ( current.value.equals( value ) ) return current;

			if ( isLeft( current.dimension , searchedCoord , current.coordinate ) ) {
				current = current.left;
			}
			else {
				current = current.right;
			}
		}
	}

	/**
	 * Add a set of points to the tree. Although no rebalancing is performed, the newly created subtrees should be balanced.
	 * For optimal results, the tree should be constructed by one single call to this method.
	 *
	 * Complexity should be O( n sqrt(n) ) (it is O( n ) for each level of the tree, and there are of the order of sqrt(n)
	 * levels in the balanced case)
	 *
	 * @param points
	 */
	public void add( Collection<T> points ) {
		add( root , points );
	}

	private void add( Node<T> addRoot , Collection<T> points ) {
		size += points.size();
		// very rough heuristic. No theory behind it.
		// optimal value depends on the ratio modification/query
		// TODO: make more configurable
		stepsToRebalance = size / 3;
		final Queue<AddFrame<T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );

		// copy parameter list as it is modified in place
		stack.add( new AddFrame<>( addRoot , new ArrayList<>( points ) ) );

		while ( !stack.isEmpty() ) {
			final AddFrame<T> currentFrame = stack.poll();

			if ( currentFrame.toAdd.isEmpty() ) continue;

			final T median = median( currentFrame.node.dimension , currentFrame.toAdd );
			final double medianValue = coordinate.getCoord( median )[ currentFrame.node.dimension ];

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

			for ( T v : currentFrame.toAdd ) {
				if ( v == median && currentFrame.node.value == null ) {
					currentFrame.node.value = v;
					currentFrame.node.coordinate = coordinate.getCoord( v );

					if ( currentFrame.node.coordinate.length != nDimensions ) throw new IllegalArgumentException( "wrong dimensionality" );
				}
				else if ( coordinate.getCoord( v )[ currentFrame.node.dimension ] < medianValue ) {
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

	private T median( final int dim , final List<T> l ) {
		// very simple approximation: take median of a sublist, using standard sort algorithm
		final List<T> sublist = sublist( l );
		Collections.sort( sublist , (t1,t2) -> Double.compare( coordinate.getCoord( t1 )[ dim ] , coordinate.getCoord( t2 )[ dim ] ) );
		return sublist.get( sublist.size() / 2 );
	}

	public Collection<T> getBox(
			final double[] lowers ,
			final double[] uppers ) {
		return getBox( lowers , uppers , (e) -> true );
	}

	public Collection<T> getBox(
			final double[] lowers ,
			final double[] uppers ,
			final Predicate<T> predicate ) {
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
				if ( predicate.test( current.value ) ) {
					result.add( current.value );
				}
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

	public T getClosestEuclidean( final double[] coord , final Predicate<T> predicate ) {
		return getClosest( coord , KDTree::euclidean , predicate );
	}

	public static double euclidean( double[] c1 , double[] c2 ) {
		double d = 0;

		for (int i=0; i < c1.length; i++ ) {
			d += Math.pow( c1[ i ] - c2[ i ] , 2 );
		}

		return d;
	}

	public T getClosest( final double[] coord , final ToDoubleBiFunction<double[],double[]> distance ) {
		return getClosest( coord , distance , e -> true );
	}

	public T getClosest(
			final double[] coord ,
			final ToDoubleBiFunction<double[],double[]> distance,
			final Predicate<T> predicate ) {
		return getClosest( coord , distance , predicate , 0d );
	}

	public T getClosest(
			final double[] coord ,
			final ToDoubleBiFunction<double[],double[]> distance,
			final Predicate<T> predicate,
			final double precision) {
		return getClosest( coord , distance , predicate , precision , Integer.MAX_VALUE );
	}

	public T getClosest(
			final double[] coord ,
			final ToDoubleBiFunction<double[],double[]> distance,
			final Predicate<T> predicate,
			final double precision,
			final int maxVisitedLeaves ) {
		// use "Best Bin First": no influence if exact search, but big influence if restrict to N leaves
		final Queue<Node<T>> stack =
				new PriorityQueue<>(
						(int) Math.sqrt( size() ),
						( n1, n2 ) -> {
							final double d1 = distance.applyAsDouble( n1.coordinate, coord );
							final double d2 = distance.applyAsDouble( n2.coordinate, coord );
							// head of queue is least element: always pick the one with the smallest distance
							return Double.compare( d1, d2 );
						} );
		return getClosest( stack , coord , distance , predicate , precision , maxVisitedLeaves );
	}

	private T getClosest(
			final Queue<Node<T>> stack,
			final double[] coord ,
			final ToDoubleBiFunction<double[],double[]> distance,
			final Predicate<T> predicate,
			final double precision,
			final int maxVisitedLeaves ) {
		T closest = null;
		double bestDist = Double.POSITIVE_INFINITY;

		add( stack , root );

		// First find a good candidate without too much effort: search for the "insertion point", and check the distance
		// on the way (having a good start will improve pruning in the later stage)
		while( !stack.isEmpty() ) {
			final Node<T> current = stack.poll();
			if ( current.value == null ) continue;
			final double currentDist = distance.applyAsDouble( coord , current.coordinate );

			if ( currentDist < bestDist && predicate.test( current.value ) ) {
				closest = current.value;
				bestDist = currentDist;
			}

			final boolean isLeft = isLeft( current.dimension , coord , current.coordinate );

			if ( isLeft ) add( stack , current.left );
			else add( stack , current.right );
		}

		// now, go down the full tree, pruning half-spaces further away than the current best distance
		add( stack , root );
		int remainingLeaves = maxVisitedLeaves;
		while ( !stack.isEmpty() ) {
			final Node<T> current = stack.poll();

			// if the cutting hyperplane is further away than the best distance, only explore on "our side" of it
			final double[] projected = Arrays.copyOf( coord , coord.length );
			projected[ current.dimension ] = current.coordinate[ current.dimension ];
			final double distanceToCuttingPlane = distance.applyAsDouble( projected , coord );

			if ( distanceToCuttingPlane > bestDist ) {
				final boolean isLeft = isLeft( current.dimension , current.coordinate , coord );

				// if we are to far left, only continue to the right, and vice versa
				if ( isLeft ) add( stack , current.right );
				else add( stack , current.left );
			}
			else {
				final double currentDist = distance.applyAsDouble( coord , current.coordinate );
				if ( currentDist < bestDist && predicate.test( current.value ) ) {
					closest = current.value;
					bestDist = currentDist;
					// early abort. Default precision means abort if distance exactly 0
					if ( currentDist <= precision ) return closest;
				}

				// could take care of order to make pruning more probable
				add( stack , current.left );
				add( stack , current.right );
			}

			if ( isLeaf( current ) && remainingLeaves-- == 0 ) return closest;
		}

		return closest;
	}

	private static <T> void add( final Queue<Node<T>> stack, final Node<T> right ) {
		if ( right != null && right.value != null ) stack.add( right );
	}

	private static boolean isLeft( int dim , double[] coord , double[] of ) {
		return coord[ dim ] < of[ dim ];
	}

	private static class AddFrame<T> {
		Node<T> node;
		List<T> toAdd;

		public AddFrame( final Node<T> node, final List<T> toAdd ) {
			this.node = node;
			this.toAdd = toAdd;
		}
	}

	private static class Node<T> {
		private Node<T> left, right;
		private double[] coordinate;
		private T value;
		private final int dimension;

		private Node(
				final int dimension ) {
			this.dimension = dimension;
		}

		@Override
		public String toString() {
			return "[Node coord="+Arrays.toString( coordinate )+
					"; value="+value+
					"; dimension="+dimension+
					"; hasLeft="+(left != null)+
					"; hasRight="+(right != null)+"]";
		}
	}
}
