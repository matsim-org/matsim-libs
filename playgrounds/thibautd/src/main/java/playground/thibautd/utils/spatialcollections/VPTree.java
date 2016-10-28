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

import org.matsim.core.utils.misc.Counter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author thibautd
 */
public class VPTree<C,T> implements SpatialTree<C, T> {
	private static final int SUBLIST_SIZE_MEDIAN = 100;
	private final SpatialCollectionUtils.Metric<C> metric;
	private final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate;

	private final Node<C,T> root = new Node<>();
	private final Random r = new Random( 123 );

	public VPTree( final SpatialCollectionUtils.Metric<C> metric,
			final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate ) {
		this.metric = metric;
		this.coordinate = coordinate;
	}


	@Override
	public int size() {
		return root.size;
	}

	/**
	 * O( log( n ) ), gets a random element from the tree, with an approximately uniform distribution if the tree is
	 * approximately balanced.
	 *
	 * @return an element
	 */
	@Override
	public T getAny() {
		if ( size() == 0 ) return null;

		Node<C,T> current = root;
		int index = r.nextInt( size() );


		while ( true ) {
			if ( index == 0 && current.value != null ) return current.value;

			assert index <= current.size : index +" > "+ current.size;
			if ( current.value != null ) index--;

			if ( current.close != null && current.close.size > index ) {
				current = current.close;
			}
			else {
				assert current.far != null : "cannot happen if sizes are right";
				index -= current.close != null ? current.close.size : 0;
				current = current.far;
			}
		}
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
		final Queue<AddFrame<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );

		// copy parameter list as it is modified in place
		stack.add( new AddFrame<>( addRoot , new ArrayList<>( points ) ) );

		final Counter counter = new Counter( "add VP Tree Node # " , " / "+points.size() );
		while ( !stack.isEmpty() ) {
			final AddFrame<C,T> currentFrame = stack.poll();
			if ( currentFrame.toAdd.isEmpty() ) continue;
			counter.incCounter();

			final T vantagePoint = removeVantagePoint( currentFrame.toAdd );

			assert vantagePoint != null;
			currentFrame.node.value = vantagePoint;
			currentFrame.node.coordinate = coordinate.getCoord( vantagePoint );

			if ( currentFrame.toAdd.isEmpty() ) {
				backtrackSize( currentFrame.node );
				continue;
			}

			final double medianDistance = medianDistance( vantagePoint , currentFrame.toAdd );

			final AddFrame<C,T> closeFrame =
					new AddFrame<>(
							new Node<>(),
							new ArrayList<>( currentFrame.toAdd.size() / 2 + 1 ) );
			final AddFrame<C,T> farFrame =
					new AddFrame<>(
							new Node<>(),
							new ArrayList<>( currentFrame.toAdd.size() / 2 + 1 ) );

			currentFrame.node.cuttoffDistance = medianDistance;

			for ( T v : currentFrame.toAdd ) {
				final double distanceToVP = metric.calcDistance( currentFrame.node.coordinate , coordinate.getCoord( v ) );

				if ( distanceToVP > medianDistance ) {
					farFrame.toAdd.add( v );
				}
				else {
					closeFrame.toAdd.add( v );
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

			// leaf: backtrack size
			if ( closeFrame.toAdd.isEmpty() && farFrame.toAdd.isEmpty() ) {
				backtrackSize( currentFrame.node );
			}
		}
		counter.printCounter();
	}

	private void backtrackSize( final Node<C, T> node ) {
		Stream.iterate( node , n -> n.parent )
				.peek( Node::recomputeSize )
				.allMatch( n -> n.parent != null );
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
		root.recomputeSize();

		add( toReadd );
	}

	@Override
	public boolean remove( final T value ) {
		Node<C,T> node = find( value );

		if ( node == null ) return false;
		node.value = null;
		backtrackSize( node );

		// remove totally invalidated branch ends, to speed up latter queries
		for( ; node.size == 0 &&
						node.parent != null;
				node = node.parent ) {
			if ( node.parent.close == node ) node.parent.close = null;
			if ( node.parent.far == node ) node.parent.far = null;
		}
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
		private int size = 0;
		private C coordinate;
		private T value = null;
		private double cuttoffDistance = Double.NaN;
		private Node<C,T> close = null;
		private Node<C,T> far = null;
		private Node<C,T> parent = null;

		public void recomputeSize() {
			size = value == null ? 0 : 1;
			size += close == null ? 0 : close.size;
			size += far == null ? 0 : far.size;
		}
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
