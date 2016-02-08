/* *********************************************************************** *
 * project: org.matsim.*
 * IntegerCache.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;

public class IntegerCache {
	// yyyy not sure what this class is doing.  as long as MIN_VALUE=0 (as I am finding it), it just seems to receive an
	// Integer, and return an Integer of the same value.  Maybe there was historically a situation where this did not start 
	// at 0, and then one wanted to get rid of the (un)boxing overhead? kai, jan'16
	
	private final static Logger log = Logger.getLogger(IntegerCache.class);
		
	
	private static final int MIN_VALUE = 0;
	private static final int MAX_VALUE = 8192;
	
	private static boolean initialized = false;
	private static Integer [] cache;

	public static Integer getInteger(final int i) {
		
		if (!initialized) {
			init();
		}

		if ( i < MAX_VALUE && i >= MIN_VALUE) {
			return cache[i + MIN_VALUE];
		}
		
		return Integer.valueOf(i);
	}
	
	synchronized private static void init() {
		if (initialized) {
			log.warn("IntegerCache has already been initialized.");
			return;
		}
		
		log.info("Initializing IntegerCache ...");
		cache = new Integer [MAX_VALUE - MIN_VALUE];
		for (int i = 0; i < (MAX_VALUE - MIN_VALUE); i++) {
			cache[i] = Integer.valueOf(i - MIN_VALUE);
		}
		log.info("done.");
		initialized = true;
	}
	
}
