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
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * @author thibautd
 */
public class VPTree<C,T> implements SpatialTree<C, T> {
	// do not try to much VPs, as this quickly gets expensive.
	// no real improvement noticed.
	private static final int SUBLIST_SIZE_VPS = 1;
	// not too shy: cutoff distance should be pretty accurate
	private static final int SUBLIST_SIZE_MEDIAN = 100;
	private final SpatialCollectionUtils.Metric<C> metric;
	private final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate;

	private final Node<C,T> root;
	private final Random r = new Random( 123 );

	public VPTree( final SpatialCollectionUtils.Metric<C> metric,
			final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate ) {
		this( new Node<>(), metric , coordinate );
	}

	private VPTree(
			final Node<C,T> root,
			final SpatialCollectionUtils.Metric<C> metric,
			final SpatialCollectionUtils.GenericCoordinate<C,T> coordinate ) {
		this.root = root;
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
		final Queue<AddFrame<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>( 1 + (int) Math.log( 1 + points.size() ) ) );

		// copy parameter list as it is modified in place
		stack.add( new AddFrame<>( addRoot , new ArrayList<>( points ) ) );

		final Counter counter = new Counter( "add VP Tree Node # " , " / "+points.size() );
		while ( !stack.isEmpty() ) {
			final AddFrame<C,T> currentFrame = stack.poll();
			if ( currentFrame.toAdd.isEmpty() ) continue;
			counter.incCounter();

			if ( currentFrame.toAdd.size() == 1 ) {
				currentFrame.node.value = currentFrame.toAdd.get( 0 );
				currentFrame.node.coordinate = coordinate.getCoord( currentFrame.toAdd.get( 0 ) );
				backtrackSize( currentFrame.node );
				continue;
			}

			selectAndSetVantagePoint( currentFrame );

			final T vantagePoint = currentFrame.node.value;
			final double medianDistance = currentFrame.node.cuttoffDistance;

			final AddFrame<C,T> closeFrame =
					new AddFrame<>(
							new Node<>(),
							new ArrayList<>( currentFrame.toAdd.size() / 2 + 1 ) );
			final AddFrame<C,T> farFrame =
					new AddFrame<>(
							new Node<>(),
							new ArrayList<>( currentFrame.toAdd.size() / 2 + 1 ) );

			for ( T v : currentFrame.toAdd ) {
				if ( vantagePoint == v ) continue;
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

	private void selectAndSetVantagePoint( final AddFrame<C, T> currentFrame ) {
		final List<T> vantagePoints = sublist( currentFrame.toAdd , SUBLIST_SIZE_VPS );

		// select the VP with the HIGHEST median (highest spread) is supposed to give better query times
		// the intuition is that high spread corresponds to the corners of the space, where the length of the border
		// between close and far is minimized. The length of the border is proportional to the probability that no pruning
		// occurs, which decreases performance.
		// the paper uses the second moment, that is, the sum of square difference with median.
		currentFrame.node.cuttoffDistance = Double.NEGATIVE_INFINITY;
		assert !vantagePoints.isEmpty();
		for ( T vantagePoint : vantagePoints ) {
			final double medianDistance = medianDistance( vantagePoint , currentFrame.toAdd );

			if ( medianDistance > currentFrame.node.cuttoffDistance ) {
				currentFrame.node.value = vantagePoint;
				currentFrame.node.coordinate = coordinate.getCoord( vantagePoint );
				currentFrame.node.cuttoffDistance = medianDistance;
			}
		}
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

		// copy parameter list as it is modified in place
		Node<C,T> current = root;

		final C coord = coordinate.getCoord( value );
		while ( current != null ) {
			if ( value.equals( current.value ) ) return current;

			final double distanceToVp = metric.calcDistance( current.coordinate , coord );
			if ( distanceToVp <= current.cuttoffDistance ) {
				if ( current.close != null ) current = current.close;
				else current = null;
			}
			else if ( current.far != null ) current = current.far;
			else current = null;
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
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>( 1 + (int) Math.log( 1 + size() )) );
		stack.add( root );

		T closest = null;
		double bestDist = Double.POSITIVE_INFINITY;

		// first estimate: go directly to insertion point. O( log( n ) ) for balanced tree.
		// Increases the probability of pruning in the second stage, by having a tight bound on best distance.
		while ( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			final double distanceToVp = metric.calcDistance( current.coordinate , coord );

			// check if current VP closest than closest known
			if ( current.value != null &&
					distanceToVp < bestDist &&
					predicate.test( current.value ) ) {
				closest = current.value;
				bestDist = distanceToVp;
			}

			if ( distanceToVp <= current.cuttoffDistance ) {
				if ( current.close != null ) stack.add( current.close );
			}
			else if ( current.far != null ) stack.add( current.far );
		}

		// full search
		stack.add( root );
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

	public Collection<T> getBall(
			final C coord,
			final double maxDist,
			final Predicate<T> predicate ) {
		return getBallsIntersection( Collections.singleton( coord ) , maxDist , predicate );
	}

	public Collection<T> getBallsIntersection(
			final Collection<C> coords,
			final double maxDist,
			final Predicate<T> predicate ) {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>( 1 + (int) Math.log( 1 + size() )) );
		stack.add( root );

		final Collection<T> ball = new ArrayList<>();

		while( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( current.value == null &&
					current.close == null &&
					current.far == null ) {
				continue;
			}

			final double[] distsToVp = coords.stream()
					.mapToDouble( c -> metric.calcDistance( c , current.coordinate ) )
					.toArray();

			// check if current VP in ball
			if ( current.value != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d < maxDist ) &&
					predicate.test( current.value ) ) {
				ball.add( current.value );
			}

			// test intersection of disc with the children
			if ( current.close != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d - maxDist <= current.cuttoffDistance ) ) {
				stack.add( current.close );
			}
			if ( current.far != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d + maxDist >= current.cuttoffDistance ) ) {
				stack.add( current.far );
			}
		}

		return ball;
	}

	public int getSizeOfBallsIntersection(
			final Collection<C> coords,
			final double maxDist ) {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>( 1 + (int) Math.log( 1 + size() )) );
		stack.add( root );

		int size = 0;

		while( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( current.value == null &&
					current.close == null &&
					current.far == null ) {
				continue;
			}

			final double[] distsToVp = coords.stream()
					.mapToDouble( c -> metric.calcDistance( c , current.coordinate ) )
					.toArray();

			// check if current VP in ball
			if ( current.value != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d < maxDist ) ) {
				size++;
			}

			// test intersection of disc with the children
			if ( current.close != null ) {
				if ( DoubleStream.of( distsToVp ).allMatch( d -> d + current.cuttoffDistance < maxDist ) ) {
					// early pruning if the whole close node is in the required zone
					size += current.close.size;
				}
				else if ( DoubleStream.of( distsToVp ).allMatch( d -> d - maxDist <= current.cuttoffDistance ) ) {
					stack.add( current.close );
				}
			}
			if ( current.far != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d + maxDist >= current.cuttoffDistance ) ) {
				stack.add( current.far );
			}
		}

		return size;
	}

	public VPTree<C,T> getSubtreeContainingBalls(
			final Collection<C> coords,
			final double maxDist ) {
		final Queue<Node<C,T>> stack = Collections.asLifoQueue( new ArrayDeque<>( 1 + (int) Math.log( 1 + size() )) );
		stack.add( root );

		while( !stack.isEmpty() ) {
			final Node<C,T> current = stack.poll();

			if ( current.value == null &&
					current.close == null &&
					current.far == null ) {
				continue;
			}

			final double[] distsToVp = coords.stream()
					.mapToDouble( c -> metric.calcDistance( c , current.coordinate ) )
					.toArray();

			// check if current VP in balls
			if ( current.value != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d < maxDist ) ) {
				return new VPTree<>( current, metric, coordinate );
			}

			// test intersection of disc with the children
			final boolean intersectClose = current.close != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d - maxDist <= current.cuttoffDistance );
			final boolean intersectFar = current.far != null &&
					DoubleStream.of( distsToVp ).allMatch( d -> d + maxDist >= current.cuttoffDistance );

			if ( intersectClose && intersectFar ) return new VPTree<>( current, metric, coordinate );
			if ( intersectClose ) stack.add( current.close );
			if ( intersectFar ) stack.add( current.far );
		}

		return null;
	}

	private double calcDistance( final T vantagePoint, final T v ) {
		return metric.calcDistance(
				coordinate.getCoord( v ) ,
				coordinate.getCoord( vantagePoint ) );
	}

	private double medianDistance( final T vp, final List<T> l ) {
		// very simple approximation: take median of a sublist, using standard sort algorithm
		final List<T> sublist = sublist( l , SUBLIST_SIZE_MEDIAN );
		Collections.sort(
				sublist ,
				(t1,t2) -> Double.compare(
						calcDistance( vp, t1 ),
						calcDistance( t2, vp ) ));
		return calcDistance( sublist.get( sublist.size() / 2 ), vp );
	}

	private <E> List<E> sublist( final List<E> l , final int size ) {
		if ( l.size() <= size ) return new ArrayList<>( l );

		final List<E> sublist = new ArrayList<>( size );
		for ( int i=0; i < size; i++ ) {
			final int j = i + r.nextInt( size - i );
			final E elemJ = l.get( j );
			l.set( j , l.get( i ) );
			l.set( i , elemJ );
			// build the list in parallel to avoid the intermediary step of building a sublist.
			sublist.add( elemJ );
		}

		return sublist;
	}

	private static class Node<C,T> {
		private int size = 0;
		private C coordinate = null;
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
