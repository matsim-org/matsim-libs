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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained;

import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.log4j.Logger;

/**
 * @author thibautd
 */
class LogitProbabilityCalculator {
	private static final Logger log = Logger.getLogger( LogitProbabilityCalculator.class );
	private double numeratorUtility;
	private final TDoubleList denominatorUtilities = new TDoubleArrayList();

	private double max = Double.NEGATIVE_INFINITY;

	public void setNumeratorUtility( final double numeratorUtility ) {
		this.numeratorUtility = numeratorUtility;
		this.max = Math.max( max, numeratorUtility );
	}

	public void addDenominatorUtilities( TDoubleList us ) {
		us.forEach( u -> {
			addDenominatorUtility( u );
			return true;
		} );
	}

	public void addDenominatorUtility( final double u ) {
		this.denominatorUtilities.add( u );
		this.max = Math.max( max, u );
	}

	public double calcProbability() {
		double denominator = 0;

		for ( TDoubleIterator iterator = denominatorUtilities.iterator();
			  iterator.hasNext(); ) {
			denominator += Math.exp( iterator.next() - max );
		}

		final double numerator = Math.exp( numeratorUtility - max );

		if ( numerator > denominator ) {
			log.error( numerator+" > "+denominator );
			throw new IllegalStateException( "numerator of logit probability is greater than denominator. Please check the inputs.");
		}

		return numerator / denominator;
	}
}
