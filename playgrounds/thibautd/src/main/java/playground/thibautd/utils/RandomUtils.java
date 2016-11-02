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
}

