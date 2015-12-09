/* *********************************************************************** *
 * project: org.matsim.*
 * ProgressPrinter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.util;

import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Prints a simple progress bar to stdout:
 * <br>
 * <tt>0%....50%....100%</tt>
 *
 * @author jillenberger
 *
 */
public class ProgressLogger {
	
	private static final String DOT = ".";

	private static long maxVal;
	
	private static long minorTickVal;
	
	private static long majorTickVal;
	
	private static AtomicLong counter;

	/**
	 * Initializes the progress logger.
	 * @param max the total number of steps
	 * @param minorTick the number of steps after that a dot is printed
	 * @param majorTick the number of steps after that the percentage is printed
	 */
	public static void init(long max, long minorTick, long majorTick) {
		maxVal = max;
		minorTickVal = (long) Math.ceil(max/100.0 * minorTick);
		majorTickVal = (long) Math.ceil(max/100.0 * majorTick);
		counter = new AtomicLong(0);
		System.out.print("\tProgress: 0%");
	}

	/**
	 * Moves the internal counter one step forward and prints a dot or the percentage respectively.
	 */
	public static void step() {
		counter.incrementAndGet();
		if(counter.get() == maxVal) {
			System.out.print(NumberFormat.getPercentInstance().format(counter.get()/(double)maxVal));
			System.out.print("\n");
		} else if(counter.get() % majorTickVal == 0) {
			System.out.print(NumberFormat.getPercentInstance().format(counter.get()/(double)maxVal));
		} else if(counter.get() % minorTickVal == 0) {
			System.out.print(DOT);
		}
		
	}

	/**
	 * Terminates the logger. Use this function to abort logging before the number of {@code #step()} calls has
	 * reached the total number of steps.
	 */
	public static void terminate() {
		if(counter.get() != maxVal) {
			System.out.println(NumberFormat.getPercentInstance().format(counter.get()/(double)maxVal));
		}
	}
}
