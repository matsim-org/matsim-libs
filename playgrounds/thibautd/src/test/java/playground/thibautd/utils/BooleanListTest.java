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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * @author thibautd
 */
public class BooleanListTest {
	@Test
	public void testAddAndGetRandom() {
		final boolean[] reference = generateBooleans( 620, 1234 );

		testAddAndGet( reference );
	}

	@Test
	public void testAddAndGetTrue() {
		final boolean[] reference = new boolean[ 620 ];
		for ( int i = 0; i < reference.length; i++ ) reference[ i ] = true;
		testAddAndGet( reference );
	}

	@Test
	public void testAddAndGetFalse() {
		final boolean[] reference = new boolean[ 620 ];
		for ( int i = 0; i < reference.length; i++ ) reference[ i ] = false;
		testAddAndGet( reference );
	}

	private void testAddAndGet( boolean[] reference ) {
		final BooleanList testee = new BooleanList();
		for ( boolean b : reference ) testee.add( b );

		Assert.assertEquals(
				"unexpected size",
				reference.length,
				testee.size() );

		for ( int i = 0; i < reference.length; i++ ) {
			Assert.assertEquals(
					"unexpected value at index " + i +" after correct sequence "+
							Arrays.toString( Arrays.copyOf( reference , i ) ),
					reference[ i ],
					testee.get( i ) );
		}
	}

	@Test
	public void testArrayConstructor() {
		final boolean[] reference = generateBooleans( 623 , 1234 );

		final BooleanList testee = new BooleanList( reference );
		Assert.assertEquals(
				"unexpected storing array size",
				testee.sizeStoringArray(),
				(int) Math.ceil( reference.length / 32.0 ) );

		for ( int i = 0; i < reference.length; i++ ) {
			Assert.assertEquals(
					"unexpected value at index " + i +" after correct sequence "+
							Arrays.toString( Arrays.copyOf( reference , i ) ),
					reference[ i ],
					testee.get( i ) );
		}
	}

	private boolean[] generateBooleans( int size, int seed ) {
		final Random r = new Random( seed );

		final boolean b[] = new boolean[ size ];
		for ( int i=0; i < b.length; i++ ) {
			b[ i ] = r.nextBoolean();
		}

		return b;
	}
}
