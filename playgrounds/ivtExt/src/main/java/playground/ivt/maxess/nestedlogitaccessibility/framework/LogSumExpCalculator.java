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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * @author thibautd
 */
public class LogSumExpCalculator {
	private final TDoubleArrayList terms;

	double min = Double.POSITIVE_INFINITY;
	double max = Double.NEGATIVE_INFINITY;

	public LogSumExpCalculator( final int size ) {
		terms = new TDoubleArrayList( size );
	}

	public void addTerm( final double util ) {
		if ( Double.isNaN( util ) || Double.isInfinite( util ) ) {
			throw new IllegalArgumentException( "Only finite terms are allowed, got " + util );
		}
		terms.add( util );
		min = Math.min( util, min );
		max = Math.max( util, max );
	}

	public double computeLogsumExp() {
		if ( terms.isEmpty() ) throw new IllegalStateException( "nothing to sum!" );
		// under and overflow avoidance
		// see http://jblevins.org/log/log-sum-exp
		// Note that this can only avoid underflow OR overflow,
		// not both at the same time

		// correcting constant: greatest term in absolute value
		final double c = ( -min > max ) ? -min : max;

		double sum = 0;
		for ( double d : terms.toArray() ) {
			sum += Math.exp( d - c );
			// TODO check if underflow (how? compare with 0?)
			if ( Double.isInfinite( sum ) || Double.isNaN( sum ) ) {
				throw new RuntimeException( "got sum " + sum + " for exp " + d + " with correction " + c + "! (resulting in exp(" + ( d - c ) + "))" );
			}
		}

		final double logsum = Math.log( sum ) + c;

		if ( Double.isNaN( logsum ) || Double.isInfinite( logsum ) ) {
			throw new RuntimeException( "logsum is " + logsum + " for sum " + sum + " and correction term " + c );
		}

		return logsum;
	}
}
