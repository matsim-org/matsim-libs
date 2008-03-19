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

package org.matsim.utils.misc;

import org.apache.log4j.Logger;

/**
 * A simple class that implements a counter that outputs the current counter-value from time to time.
 * The method {@link #incCounter()} is synchronized, so it can be used with Threads.
 *
 * @author mrieser
 */
public final class Counter {
	private final String prefix;
	private int counter = 0;
	private int nextCounter = 1;
	private static final Logger log = Logger.getLogger(Counter.class);

	/**
	 * @param prefix Some text that is output just before the counter-value.
	 */
	public Counter(final String prefix) {
		this.prefix = prefix;
	}

	synchronized public void incCounter() {
		this.counter++;
		if (this.counter == this.nextCounter) {
			printCounter();
			this.nextCounter *= 2;
		}
	}

	synchronized public void printCounter() {
		log.info(this.prefix + this.counter);
	}

	synchronized public int getCounter() {
		return this.counter;
	}

	synchronized public void reset() {
		this.counter = 0;
		this.nextCounter = 1;
	}
}