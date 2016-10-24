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
package playground.thibautd.utils.spatialcollections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.function.Predicate;

/**
 * @author thibautd
 */
public class VPTree<C,T> implements SpatialTree<C, T> {
	private static final int SUBLIST_SIZE_MEDIAN = 100;
	private final SpatialCollectionUtils.Metric<C> metric;
	private final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate;

	private int stepsToRemove = 0;

	private Node<C,T> root = new Node<>();
	private int size = 0;
	private final Random r = new Random( 123 );

	public VPTree( final SpatialCollectionUtils.Metric<C> metric,
			final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate ) {
		this.metric = metric;
		this.coordinate = coordinate;
	}


	@Override
	public int size() {
		return size;
	}

	@Override
	public T getAny() {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		while ( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( current.value != null ) return current.value;
			if ( current.close != null ) stack.add( current.close );
			if ( current.far != null ) stack.add( current.far );
		}

		return null;
	}

	@Override
	public Collection<T> getAll() {
		return getAll( root );
	}

	private Collection<T> getAll( final Node<C,T> node ) {
		if ( node == null ) return Collections.emptyList();
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( node );

		final Collection<T> all = new ArrayList<>();
		while ( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( current.value != null ) all.add( current.value );
			if ( current.close != null ) stack.add( current.close );
			if ( current.far != null ) stack.add( current.far );
		}

		return all;
	}

	@Override
	public void add( final Collection<T> toAdd ) {
		if ( size() > 0 ) throw new IllegalStateException();
		add( root , toAdd );
	}

	private void add( final Node<C,T> addRoot , final Collection<T> points ) {
		size += points.size();
		// TODO: extract in some other place, not to mix it here
		stepsToRemove += size / 2;

		final Queue<AddFrame<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );

		// copy parameter list as it is modified in place
		stack.add( new AddFrame<>( addRoot , new ArrayList<>( points ) ) );

		while ( !stack.isEmpty() ) {
			final AddFrame<C,T> currentFrame = stack.poll();
			if ( currentFrame.toAdd.isEmpty() ) continue;

			final T vantagePoint = removeVantagePoint( currentFrame.toAdd );

			assert vantagePoint != null;
			currentFrame.node.value = vantagePoint;
			currentFrame.node.coordinate = coordinate.getCoord( vantagePoint );

			if ( currentFrame.toAdd.isEmpty() ) continue;

			final double medianDistance = medianDistance( vantagePoint , currentFrame.toAdd );

			// avoid wasting memory by reusing toAdd list
			final AddFrame<C,T> closeFrame =
					new AddFrame<>(
							new Node<>(),
							currentFrame.toAdd );
			final AddFrame<C,T> farFrame =
					new AddFrame<>(
							new Node<>(),
							new ArrayList<>( currentFrame.toAdd.size() / 2 + 1 ) );

			currentFrame.node.cuttoffDistance = medianDistance;

			for ( Iterator<T> it = currentFrame.toAdd.iterator(); it.hasNext(); ) {
				final T v = it.next();
				final double distanceToVP = metric.calcDistance( currentFrame.node.coordinate , coordinate.getCoord( v ) );

				if ( distanceToVP > medianDistance ) {
					it.remove();
					farFrame.toAdd.add( v );
				}
			}

			if ( !closeFrame.toAdd.isEmpty() ) {
				currentFrame.node.close = closeFrame.node;
				closeFrame.node.parent = currentFrame.node;
				stack.add( closeFrame );
			}
			if ( !farFrame.toAdd.isEmpty() ) {
				currentFrame.node.far = farFrame.node;
				farFrame.node.parent = currentFrame.node;
				stack.add( farFrame );
			}
		}
	}

	/**
	 * Fastest version of remove. Does not remove the node, only the point.
	 * Removal itself is faster, but latter queries are slower.
	 *
	 * @param value
	 */
	public boolean invalidate( final T value ) {
		Node<C,T> node = find( value );

		if ( node == null ) return false;
		node.value = null;
		size--;

		// remove totally invalidated branch ends, to speed up latter queries
		for( ;
				node.value == null && node.far == null && node.close == null;
				node = node.parent ) {
			if ( node.parent.close == node ) node.parent.close = null;
			if ( node.parent.far == node ) node.parent.far = null;
		}
		return true;
	}

	/**
	 * rebuilds the full tree. Might help to rebalance a tree imbalanced by removals, or make a tree with lots of
	 * invalidated values smaller.
	 */
	public void rebuild() {
		final Collection<T> toReadd = getAll();

		// invalidate root
		root.cuttoffDistance = -1;
		root.far = null;
		root.close = null;
		root.coordinate = null;
		root.value = null;

		size = 0;
		add( toReadd );
	}

	/**
	 * Naively optimized remove. Invalidates values, and rebuilds after successfully invalidating 1/2 of the values.
	 * Not based on any theory.
	 *
	 * @param value
	 * @return
	 */
	@Override
	public boolean remove( final T value ) {
		boolean isRemoved = invalidate( value );

		if ( isRemoved && --stepsToRemove == 0 ) {
			// rebuild updates steps to remove
			rebuild();
		}

		return isRemoved;
	}

	public boolean trueRemove( final T value ) {
		final Node<C,T> node = find( value );

		if ( node == null ) return false;

		// get all children
		final Collection<T> toReadd = new ArrayList<>( getAll( node.close ) );
		toReadd.addAll( getAll( node.far ) );

		// invalidate node
		node.cuttoffDistance = -1;
		node.far = null;
		node.close = null;
		node.coordinate = null;
		node.value = null;

		size -= toReadd.size() + 1;
		// this part might be expensive. Way to make it faster?
		add( node , toReadd );

		return true;
	}

	@Override
	public boolean contains( final T value ) {
		return find( value ) != null;
	}

	private Node<C,T> find( final T value ) {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );

		// copy parameter list as it is modified in place
		stack.add( root );

		final C coord = coordinate.getCoord( value );
		while ( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( value.equals( current.value ) ) return current;

			final double distanceToVp = metric.calcDistance( current.coordinate , coord );
			if ( distanceToVp <= current.cuttoffDistance ) {
				if ( current.close != null ) stack.add( current.close );
			}
			else if ( current.far != null ) stack.add( current.far );
		}

		return null;
	}

	@Override
	public T getClosest( final C coord ) {
		return getClosest( coord , t -> true );
	}

	@Override
	public T getClosest(
			final C coord,
			final Predicate<T> predicate ) {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		T closest = null;
		double bestDist = Double.POSITIVE_INFINITY;

		while( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( current.value == null &&
					current.close == null &&
					current.far == null ) {
				continue;
			}

			final double distToVp = metric.calcDistance( coord , current.coordinate );

			// check if current VP closest than closest known
			if ( current.value != null &&
					distToVp < bestDist &&
					predicate.test( current.value ) ) {
				closest = current.value;
				bestDist = distToVp;
			}

			// test intersection of disc of points closest than best so far with the children
			if ( current.close != null && distToVp - bestDist <= current.cuttoffDistance ) stack.add( current.close );
			if ( current.far != null && distToVp + bestDist >= current.cuttoffDistance ) stack.add( current.far );
		}

		return closest;
	}

	private double calcDistance( final T vantagePoint, final T v ) {
		return metric.calcDistance(
				coordinate.getCoord( v ) ,
				coordinate.getCoord( vantagePoint ) );
	}

	private double medianDistance( final T vp, final List<T> l ) {
		// very simple approximation: take median of a sublist, using standard sort algorithm
		final List<T> sublist = sublist( l );
		Collections.sort(
				sublist ,
				(t1,t2) -> Double.compare(
						calcDistance( vp, t1 ),
						calcDistance( t2, vp ) ));
		return calcDistance( sublist.get( sublist.size() / 2 ), vp );
	}

	private <E> List<E> sublist( final List<E> l ) {
		if ( l.size() < SUBLIST_SIZE_MEDIAN ) return l;

		final List<E> sublist = new ArrayList<>( (int) (SUBLIST_SIZE_MEDIAN * 1.5) );
		final double prob = ((double) SUBLIST_SIZE_MEDIAN) / l.size();

		for ( E e : l ) {
			if ( r.nextDouble() < prob ) sublist.add( e );
		}

		return sublist;
	}

	private T removeVantagePoint( final List<T> toAdd ) {
		return toAdd.remove( r.nextInt( toAdd.size() ) );
	}

	private static class Node<C,T> {
		private C coordinate;
		private T value = null;
		private double cuttoffDistance = Double.NaN;
		private Node<C,T> close = null;
		private Node<C,T> far = null;
		private Node<C,T> parent = null;
	}

	private static class AddFrame<C,T> {
		Node<C,T> node;
		List<T> toAdd;

		public AddFrame( final Node<C,T> node, final List<T> toAdd ) {
			this.node = node;
			this.toAdd = toAdd;
		}
	}
}
