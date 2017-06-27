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
package playground.ivt.utils;

import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/**
 * A simple class that implements a counter that outputs the current counter-value from time to time.
 * This class is thread-safe.
 *
 * @author mrieser
 */
// Should replace matsim core's counter when language level is switched to 1.8
public class LambdaCounter {
	private final AtomicLong counter = new AtomicLong(0);
	private final AtomicLong nextCounter = new AtomicLong(1);
	private static final Logger log = Logger.getLogger(LambdaCounter.class);
	private final LongConsumer printFunction;

	/**
	 * @param prefix Some text that is output just before the counter-value.
	 */
	public LambdaCounter(final String prefix) {
		this( prefix , "" );
	}

	/**
	 * @param prefix Some text that is output just before the counter-value.
	 * @param suffix Some text that is output just after the counter-value.
	 */
	public LambdaCounter(final String prefix, final String suffix) {
		this( c -> log.info( prefix + c + suffix ) );
	}

	public LambdaCounter( final LongConsumer printFunction ) {
		this.printFunction = printFunction;
	}

	public void incCounter() {
		long i = this.counter.incrementAndGet();
		long n = this.nextCounter.get();
		if (i >= n) {
			if (this.nextCounter.compareAndSet(n, n*2)) {
				printFunction.accept( n );
			}
		}
	}

	public void printCounter() {
		printFunction.accept( counter.get() );
	}

	public long getCounter() {
		return this.counter.get();
	}

	public void reset() {
		this.counter.set(0);
		this.nextCounter.set(1);
	}
}
