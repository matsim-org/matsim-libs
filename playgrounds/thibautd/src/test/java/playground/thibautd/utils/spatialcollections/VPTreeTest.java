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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.utils.misc.Counter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * @author thibautd
 */
public class VPTreeTest {
	private static final int GRID_SIZE = 50;

	@Test
	public void testRemove() {
		final Random random = new Random( 123 );
		for ( int r = 0; r < 10; r++ ) {
			final VPTree<double[], Point> tree = createTree();

			final int initialSize = tree.size();
			tree.trueRemove(
					new Point(
							random.nextInt( GRID_SIZE ),
							random.nextInt( GRID_SIZE ),
							random.nextInt( GRID_SIZE )) );

			Assert.assertEquals(
				"unexpected claimed size",
				tree.size(),
				tree.getAll().size() );

			Assert.assertEquals( "unexpected size" , initialSize - 1 , tree.size() );

			// check tree is still valid
			for ( int i = 0; i < 500; i++ ) {
				final double[] c = new double[ 3 ];
				for ( int j = 0; j < c.length; j++ ) c[ j ] = random.nextDouble() * ( GRID_SIZE - 1 );

				final Point closest = tree.getClosest( c );

				final int[] expected = new int[ 3 ];
				for ( int j = 0; j < c.length; j++ ) expected[ j ] = (int) Math.round( c[ j ] );

				Assert.assertArrayEquals(
						"unexpected closest point " + Arrays.toString( closest.ints ) + " for " + Arrays.toString( c ),
						expected,
						closest.ints );
			}
		}
	}

	@Test
	public void testInvalidate() {
		final Random random = new Random( 123 );
		for ( int r = 0; r < 10; r++ ) {
			final VPTree<double[], Point> tree = createTree();

			final int initialSize = tree.size();
			tree.invalidate(
					new Point(
							random.nextInt( GRID_SIZE ),
							random.nextInt( GRID_SIZE ),
							random.nextInt( GRID_SIZE )) );

			Assert.assertEquals(
				"unexpected claimed size",
				tree.size(),
				tree.getAll().size() );

			Assert.assertEquals( "unexpected size" , initialSize - 1 , tree.size() );

			// check tree is still valid
			for ( int i = 0; i < 500; i++ ) {
				final double[] c = new double[ 3 ];
				for ( int j = 0; j < c.length; j++ ) c[ j ] = random.nextDouble() * ( GRID_SIZE - 1 );

				final Point closest = tree.getClosest( c );

				final int[] expected = new int[ 3 ];
				for ( int j = 0; j < c.length; j++ ) expected[ j ] = (int) Math.round( c[ j ] );

				Assert.assertArrayEquals(
						"unexpected closest point " + Arrays.toString( closest.ints ) + " for " + Arrays.toString( c ),
						expected,
						closest.ints );
			}
		}
	}

	@Test
	public void testInvalidateAndRebuild() {
		final Random random = new Random( 123 );
		for ( int r = 0; r < 10; r++ ) {
			final VPTree<double[], Point> tree = createTree();

			for ( int inv = 0; inv < tree.size() / 2; inv++ ) {
				tree.invalidate(
						new Point(
								random.nextInt( GRID_SIZE ),
								random.nextInt( GRID_SIZE ),
								random.nextInt( GRID_SIZE ) ) );
			}

			tree.rebuild();

			Assert.assertEquals(
				"unexpected claimed size",
				tree.size(),
				tree.getAll().size() );

			// check tree is still valid
			for ( int i = 0; i < 50; i++ ) {
				final double[] c = new double[ 3 ];
				for ( int j = 0; j < c.length; j++ ) c[ j ] = random.nextDouble() * ( GRID_SIZE - 1 );

				final Point closest = tree.getClosest( c );

				final int[] expected = new int[ 3 ];
				for ( int j = 0; j < c.length; j++ ) expected[ j ] = (int) Math.round( c[ j ] );

				if ( tree.contains( new Point( expected ) ) ) {
					Assert.assertArrayEquals(
							"unexpected closest point " + Arrays.toString( closest.ints ) + " for " + Arrays.toString( c ),
							expected,
							closest.ints );
				}
			}
		}
	}

	@Test
	public void testClosest() {
		final VPTree<double[],Point> tree = createTree();

		final Random random = new Random( 123 );

		final Counter counter = new Counter( "test # " );
		for ( int i=0; i < 50; i++ ) {
			counter.incCounter();
			final double[] c = new double[ 3 ];
			for ( int j=0; j < c.length; j++ ) c[ j ] = random.nextDouble() * (GRID_SIZE - 1);

			final Point closest = tree.getClosest( c );

			final int[] expected = new int[ 3 ];
			for ( int j=0; j < c.length; j++ ) expected[ j ] = (int) Math.round( c[ j ] );

			Assert.assertArrayEquals(
					"unexpected closest point "+Arrays.toString( closest.ints )+" for "+Arrays.toString( c ),
					expected,
					closest.ints );
		}
		counter.printCounter();
	}

	@Test
	public void testTreeSize() {
		final VPTree<double[],Point> tree = createTree();

		Assert.assertEquals(
				"unexpected size",
				tree.size(),
				tree.getAll().size() );
	}

	private static double[] toDouble( final int[] ints ) {
		final double[] d = new double[ ints.length ];
		for ( int i=0; i < ints.length; i++ ) d[ i ] = ints[ i ];
		return d;
	}

	private VPTree<double[],Point> createTree() {
		final VPTree<double[],Point> tree =
				new VPTree<>(
						SpatialCollectionUtils::manhattan,
						Point::getCoord );

		Collection<Point> l = new ArrayList<>();

		for ( int i=0; i < GRID_SIZE; i++ ) {
			for ( int j=0; j < GRID_SIZE; j++ ) {
				for ( int k=0; k < GRID_SIZE; k++ ) {
					final int[] v = { i , j , k };
					l.add( new Point( v ) );
				}
			}
		}

		tree.add( l );
		return tree;
	}

	private static class Point {
		private final double[] coord;
		private final int[] ints;

		private Point( final int... ints ) {
			this.ints = ints;
			this.coord = toDouble( ints );
		}

		public double[] getCoord() {
			return coord;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode( coord );
		}

		@Override
		public boolean equals( final Object other ) {
			return other instanceof Point &&
					Arrays.equals( ints, ( (Point) other ).ints );
		}
	}
}
