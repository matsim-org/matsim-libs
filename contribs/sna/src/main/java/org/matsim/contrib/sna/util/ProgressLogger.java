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
package org.matsim.contrib.sna.util;

import java.text.NumberFormat;


/**
 * @author jillenberger
 *
 */
public class ProgressLogger {
	
	private static final String DOT = ".";

	private static int maxVal;
	
	private static int minorTickVal;
	
	private static int majorTickVal;
	
	private static int counter;
	
	public static void init(int max, int minorTick, int majorTick) {
		maxVal = max;
		minorTickVal = (int) Math.floor(max/100.0 * minorTick);
		majorTickVal = (int) Math.floor(max/100.0 * majorTick);
		counter = 0;
		System.out.print("\tProgress: 0%");
	}
	
	public static void step() {
		counter++;
		if(counter % majorTickVal == 0) {
			System.out.print(NumberFormat.getPercentInstance().format(counter/(double)maxVal));
		} else if(counter % minorTickVal == 0) {
			System.out.print(DOT);
		}
		if(counter == maxVal) {
			System.out.println();
		}
	}
}
