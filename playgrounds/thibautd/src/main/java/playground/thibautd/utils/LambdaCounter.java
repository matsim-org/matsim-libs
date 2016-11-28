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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/**
 * @author thibautd
 */
public class LambdaCounter {
	private final AtomicLong counter = new AtomicLong(0);
	private final AtomicLong nextCounter = new AtomicLong(1);

	private final LongConsumer consumer;

	public LambdaCounter( final LongConsumer consumer ) {
		this.consumer = consumer;
	}

	public void incCounter() {
		long i = this.counter.incrementAndGet();
		long n = this.nextCounter.get();
		if (i >= n) {
			if (this.nextCounter.compareAndSet(n, n*2)) {
				consumer.accept( i );
			}
		}
	}

	public void printCounter() {
		consumer.accept( counter.get() );
	}

	public long getCounter() {
		return this.counter.get();
	}

	public void reset() {
		this.counter.set(0);
		this.nextCounter.set(1);
	}
}
