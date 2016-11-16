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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class RandomUtils {
	private RandomUtils() {}

	public static double nextLognormal( final Random random , final double location , final double scale ) {
		return Math.exp( nextNormal( random , location , scale ) );
	}

	public static double nextNormal( final Random random, final double mean, final double sd ) {
		return mean + sd * nextStandardNormal( random );
	}

	public static double nextStandardNormal( final Random random ) {
		// Approximation based on CLT.
		// Irwin-Hall distribution. Approximation gets better with increasing N.
		// TODO Zigurat would be more efficient and exact.
		return nextIrwinHall( random , 20 ) - 10;
	}

	public static double nextIrwinHall( final Random random , final int n ) {
		double sum = 0;
		for ( int i=0; i < n; i++ ) sum += random.nextDouble();
		return sum - n;
	}

	/**
	 * Gets a random sublist of the required size. The input list is modified in place, such that the random sublist
	 * corresponds to the <tt>size</tt> first elements of the modified list.
	 * This is done for efficiency reasons.
	 */
	public static <E> List<E> sublist_withSideEffect( final Random random , final List<E> l , final int size ) {
		if ( l.size() <= size ) return new ArrayList<>( l );

		final List<E> sublist = new ArrayList<>( size );
		for ( int i=0; i < size; i++ ) {
			final int j = i + random.nextInt( size - i );
			final E elemJ = l.get( j );
			l.set( j , l.get( i ) );
			l.set( i , elemJ );
			// build the list in parallel to avoid the intermediary step of building a sublist.
			sublist.add( elemJ );
		}

		return sublist;
	}
}

