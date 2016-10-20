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
public class VPTree<C,T> {
	private static final int SUBLIST_SIZE_MEDIAN = 100;
	private final SpatialCollectionUtils.Metric<C> metric;
	private final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate;

	private Node<C,T> root = new Node<>();
	private int size = 0;
	private final Random r = new Random( 123 );

	public VPTree( final SpatialCollectionUtils.Metric<C> metric,
			final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate ) {
		this.metric = metric;
		this.coordinate = coordinate;
	}


	public int size() {
		return size;
	}

	public Collection<T> getAll() {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		final Collection<T> all = new ArrayList<>();
		while ( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( current.value == null ) continue;

			all.add( current.value );

			if ( current.close != null ) stack.add( current.close );
			if ( current.far != null ) stack.add( current.far );
		}

		return all;
	}

	public void add( final Collection<T> toAdd ) {
		if ( size() > 0 ) throw new IllegalStateException();
		add( root , toAdd );
	}

	private void add( final Node<C,T> addRoot , final Collection<T> points ) {
		size += points.size();

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
				stack.add( closeFrame );
			}
			if ( !farFrame.toAdd.isEmpty() ) {
				currentFrame.node.far = farFrame.node;
				stack.add( farFrame );
			}
		}
	}

	public T getClosest( final C coord ) {
		return getClosest( coord , t -> true );
	}

	public T getClosest(
			final C coord,
			final Predicate<T> predicate ) {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>() );
		stack.add( root );

		T closest = null;
		double bestDist = Double.POSITIVE_INFINITY;

		while( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();
			if ( current.value == null ) {
				assert current.close == null;
				assert current.far == null;
				continue;
			}

			final double distToVp = metric.calcDistance( coord , current.coordinate );

			// check if current VP closest than closest known
			if ( distToVp < bestDist && predicate.test( current.value ) ) {
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
