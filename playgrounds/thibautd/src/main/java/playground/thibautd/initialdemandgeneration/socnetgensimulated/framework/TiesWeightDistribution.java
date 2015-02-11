/* *********************************************************************** *
 * project: org.matsim.*
 * TiesWeightDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.Arrays;

/**
 * @author thibautd
 */
public class TiesWeightDistribution {
	private final double binWidth;

	private static final int INIT_SIZE = 100;

	private int size = 0;
	private int[] binNumbers = new int[ INIT_SIZE ];
	private int[] binCount = new int[ INIT_SIZE ];
	private long overallCount = 0;

	public TiesWeightDistribution( final double binWidth ) {
		this.binWidth = binWidth;
	}

	public synchronized void addValue(final double value) {
		addValue( value , 1 );
	}

	public synchronized void addValue(final double value, final int count) {
		final int index = getIndex( value );
		binCount[index] += count;
		overallCount += count;
	}

	public void addCounts( final TiesWeightDistribution dist ) {
		if ( dist.binWidth != binWidth ) throw new IllegalArgumentException( dist.binWidth+" != "+binWidth );

		for ( int i=0; i < dist.size; i++ ) {
			// TODO: add to point in the middle of the bin?
			addValue( dist.binNumbers[ i ] * binWidth , dist.binCount[ i ] );
		}
	}

	private int getIndex( double value ) {
		final int binNumber = (int) Math.floor( value / binWidth );
		final int index = Arrays.binarySearch( binNumbers , 0 , size , binNumber );

		// bin exists: we are done
		if ( index >= 0 ) return index;

		// otherwise, create
		final int insertionIndex = - 1 - index;

		if ( binNumbers.length == size ) expand();

		for ( int i = size; i > insertionIndex; i-- ) {
			binNumbers[i] = binNumbers[i - 1];
			binCount[i] = binCount[i - 1];
		}

		binNumbers[insertionIndex] = binNumber;
		binCount[insertionIndex] = 0;
		size++;

		return insertionIndex;
	}

	private void expand() {
		final int newSize = size * 2;
		binNumbers = Arrays.copyOf( binNumbers , newSize );
		binCount = Arrays.copyOf( binCount , newSize );
	}

	// what would be a good name? has something to do with quantile/percentile,
	// but in absolute count...
	public double findLowerBound( final long countOverValue ) {
		long count = 0;
		for ( int i = size - 1; i >= 0; i++ ) {
			count += binCount[ i ];
			if ( count > countOverValue ) return binNumbers[ i ] * binWidth;
		}
		return Double.NEGATIVE_INFINITY;
	}

	public double[] getBinStarts() {
		final double[] starts = new double[ size ];

		for ( int i=0; i < size; i++ ) {
			starts[ i ] = binNumbers[ i ] * binWidth;
		}

		return starts;
	}

	public int[] getBinCounts() {
		return Arrays.copyOf( binCount , size );
	}

	public long getOverallCount() {
		return overallCount;
	}

	public double getBinWidth() {
		return binWidth;
	}
}

