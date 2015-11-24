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

import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Efficient stopwatch that is safe to use in a multithreaded environment.
 * Results make sense only if the start/end sequence is correct. No check is performed.
 * @author thibautd
 */
public class ConcurrentStopWatch<T extends Enum<T>> {
	private static final Logger log = Logger.getLogger( ConcurrentStopWatch.class );
	private final Class<T> enumType;
	private final AtomicLong[] measurements;

	public ConcurrentStopWatch( final Class<T> enumType ) {
		this.enumType = enumType;
		this.measurements = new AtomicLong[ enumType.getEnumConstants().length ];
		for ( int i=0; i < measurements.length; i++ ) measurements[ i ] = new AtomicLong( 0 );
	}

	public void startMeasurement( final T type) {
		// on the choice between currentTimeMillis and nanoTime, see
		// http://stackoverflow.com/a/1776053
		// currentTimeMillis is choosen because it does not depend on CPU
		// (basically, both might be wrong, but should give a reasonnable idea)
		measurements[ type.ordinal() ].addAndGet( -System.currentTimeMillis() );
	}

	public void endMeasurement( final T type ) {
		measurements[ type.ordinal() ].addAndGet( System.currentTimeMillis() );
	}

	public void printStats( final TimeUnit unit) {
		for ( int i=0; i < measurements.length; i++ ) {
			final T type = enumType.getEnumConstants()[ i ];
			final long measurement = measurements[ i ].get();
			log.info( "Time elapsed for "+type+" (in "+unit.name()+"): "+
							unit.convert( measurement , TimeUnit.MILLISECONDS ) );
		}
	}
}
