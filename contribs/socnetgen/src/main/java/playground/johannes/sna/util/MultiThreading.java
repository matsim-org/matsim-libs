/* *********************************************************************** *
 * project: org.matsim.*
 * MultiThreading.java
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
package playground.johannes.sna.util;

/**
 * @author jillenberger
 *
 */
public abstract class MultiThreading {

	private static int numAllowedThreads = Runtime.getRuntime().availableProcessors();
	
	public static void setNumAllowedThreads(int num) {
		numAllowedThreads = num;
	}
	
	public static int getNumAllowedThreads() {
		return numAllowedThreads;
	}
}
