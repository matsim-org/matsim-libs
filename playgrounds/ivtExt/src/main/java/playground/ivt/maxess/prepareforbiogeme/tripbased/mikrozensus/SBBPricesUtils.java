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
package playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus;

import org.apache.log4j.Logger;
import playground.ivt.maxess.prepareforbiogeme.tripbased.RecordFillerUtils;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;

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
		return computeSBBTripPrice( klasse , halbTax , RecordFillerUtils.getDistance( trip ) );
	}

	public static double computeSBBTripPrice(
			final SBBClass klasse,
			final boolean halbTax,
			final double distance_m ) {
		if ( log.isTraceEnabled() ) log.trace( "start calculation" );
		if ( log.isTraceEnabled() ) log.trace( "distance_m="+distance_m );
		final double roundedDistance_km = roundDistance( distance_m / 1000 );

		if ( log.isTraceEnabled() ) log.trace( "roundedDistance_km="+roundedDistance_km );

		final double unroundedPrice = calcUnroundedPrice( roundedDistance_km );

		if ( log.isTraceEnabled() ) log.trace( "unroundedPrice="+unroundedPrice );

		final double secondClassPrice =
				roundPrice(
						roundedDistance_km,
						unroundedPrice );

		// This does not correspond to what is said in the norm (where the factor is on the kilometer price), but this
		// way the results fit the table: apply the factor on the rounded second class price, and round again.
		// without this, differences go up to two rounding factors.
		final double basePrice =
				SBBClass.first == klasse ?
						roundPrice(
								roundedDistance_km,
								secondClassPrice * 1.75 ) :
						secondClassPrice;

		if ( log.isTraceEnabled() ) log.trace( "basePrice="+basePrice );

		final double minimumPrice = getMinimumPrice( klasse , halbTax );

		return Math.max( halbTax ? basePrice / 2 : basePrice , minimumPrice );
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
		final int bin = (int) Math.ceil( d / binSize );
		return bin * binSize;
	}

	private static double calcUnroundedPrice( double distance ) {
		double p = 0;
		p += calcPriceInterval( 0 , 4 , distance , 0.4451 );
		p += calcPriceInterval( 4 , 14 , distance , 0.4230 );
		p += calcPriceInterval( 14 , 48 , distance , 0.3564 );
		p += calcPriceInterval( 48 , 150 , distance , 0.2581 );
		p += calcPriceInterval( 150 , 200 , distance , 0.2508 );
		p += calcPriceInterval( 200 , 250 , distance , 0.2229 );
		p += calcPriceInterval( 250 , 300 , distance , 0.2013 );
		p += calcPriceInterval( 300 , 480 , distance , 0.1960 );
		p += calcPriceInterval( 480 , 1500 , distance , 0.1936 );
		return p;
	}

	private static double calcPriceInterval( double min, double max, double distance, double rate ) {
		assert min < max;
		if ( distance <= min ) return 0;
		if ( distance > max ) return (max - min) * rate;
		return (distance - min) * rate;
	}

}

