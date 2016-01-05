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
package playground.ivt.maxess.prepareforbiogeme.tripbased;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;

/**
 * @author thibautd
 */
public class SBBPricesUtils {
	private static final Logger log = Logger.getLogger( SBBPricesUtils.class );
	public enum SBBClass {first, second;}

	/**
	 * Prices according to SBB official fare system
	 * http://voev.ch/T600_f
	 */
	public static double computeSBBTripPrice(
			final SBBClass klasse,
			final boolean halbTax,
			final Trip trip ) {
		return computeSBBTripPrice( klasse , halbTax , getDistance( trip ) );
	}

	public static double computeSBBTripPrice(
			final SBBClass klasse,
			final boolean halbTax,
			final double distance_m ) {
		if ( log.isTraceEnabled() ) log.trace( "start calculation" );
		if ( log.isTraceEnabled() ) log.trace( "distance_m="+distance_m );
		final double roundedDistance_km = roundDistance( distance_m / 1000 );

		if ( log.isTraceEnabled() ) log.trace( "roundedDistance_km="+roundedDistance_km );

		final double rate_km = getCostRate( roundedDistance_km );

		if ( log.isTraceEnabled() ) log.trace( "rate_km="+rate_km );

		if ( log.isTraceEnabled() ) log.trace( "unroundedPrice="+(roundedDistance_km * rate_km) );

		final double basePrice = roundPrice( roundedDistance_km , roundedDistance_km * rate_km );

		if ( log.isTraceEnabled() ) log.trace( "basePrice="+basePrice );

		final double multiplicator = getMultiplicator( klasse , halbTax );
		final double minimumPrice = getMinimumPrice( klasse , halbTax );

		return Math.max( multiplicator * basePrice , minimumPrice );
	}

	private static double getMultiplicator( SBBClass klasse, boolean halbTax ) {
		double m = 1;

		if ( klasse == SBBClass.first ) m *= 1.75;
		if ( halbTax ) m /= 2;

		return m;
	}

	private static double getMinimumPrice( SBBClass klasse, boolean halbTax ) {
		switch ( klasse ) {
			case first:
				return halbTax ? 2.7 : 5.40;
			case second:
				return halbTax ? 2.2 : 3;
		}
		throw new RuntimeException();
	}

	private static double roundDistance( double distance_km ) {
		if ( distance_km <= 8 ) return round( distance_km , 4 );
		if ( distance_km <= 30 ) return round( distance_km , 2 );
		if ( distance_km <= 60 ) return round( distance_km , 3 );
		if ( distance_km <= 100 ) return round( distance_km , 4 );
		if ( distance_km <= 150 ) return round( distance_km , 5 );
		if ( distance_km <= 300 ) return round( distance_km , 10 );
		return round( distance_km , 20 );
	}

	private static double roundPrice( double distance_km, double price ) {
		return round( price , distance_km < 70 ? 0.2 : 1 );
	}

	private static double round( double d , double binSize ) {
		final double bin = Math.ceil( d / binSize );
		return bin * binSize;
	}

	private static double getCostRate( double distance ) {
		//if ( true ) return 0.4342;
		if ( distance <= 4 ) return 0.4451;
		if ( distance <= 14 ) return 0.4230;
		if ( distance <= 48 ) return 0.3564;
		if ( distance <= 150 ) return 0.2581;
		if ( distance <= 200 ) return 0.2508;
		if ( distance <= 250 ) return 0.2229;
		if ( distance <= 300 ) return 0.2013;
		if ( distance <= 480 ) return 0.1960;
		return 0.1936;
	}

	public static double getDistance( Trip trip ) {
		double d = 0;

		for ( Leg l : trip.getLegsOnly() ) {
			// defined?
			d += l.getRoute().getDistance();
		}

		return d;
	}
}

