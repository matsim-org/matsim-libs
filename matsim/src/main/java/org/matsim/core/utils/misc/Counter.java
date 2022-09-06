/* *********************************************************************** *
 * project: org.matsim.*
 * Counter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.misc;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple class that implements a counter that outputs the current counter-value from time to time.
 * This class is thread-safe.
 *
 * @author mrieser
 */
public final class Counter {
	private final String prefix;
	private final String suffix;
	private final int multiplier;
	private final AtomicLong counter = new AtomicLong(0);
	private final AtomicLong nextCounter = new AtomicLong(1);
	private static final Logger log = LogManager.getLogger(Counter.class);

	/**
	 * @param prefix Some text that is output just before the counter-value.
	 */
	public Counter(final String prefix) {
		this( prefix , "" );
	}
	
	/**
	 * @param prefix Some text that is output just before the counter-value.
	 * @param suffix Some text that is output just after the counter-value.
	 */
	public Counter(final String prefix, final String suffix ) {
		this( prefix, suffix, 2 ) ;
	}
		/**
		 * @param prefix Some text that is output just before the counter-value.
		 * @param suffix Some text that is output just after the counter-value.
		 */
	public Counter(final String prefix, final String suffix, int multiplier) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.multiplier = multiplier;
	}

	public void incCounter() {
		long i = this.counter.incrementAndGet();
		long n = this.nextCounter.get();
		if ((i >= n) && (this.nextCounter.compareAndSet(n, n*multiplier))) {
			log.info(this.prefix + n + this.suffix);
		}
	}

	public void printCounter() {
		log.info(this.prefix + this.counter.get() + this.suffix);
	}

	public long getCounter() {
		return this.counter.get();
	}

	public void reset() {
		this.counter.set(0);
		this.nextCounter.set(1);
	}
}
