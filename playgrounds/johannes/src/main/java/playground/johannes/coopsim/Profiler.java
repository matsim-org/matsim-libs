/* *********************************************************************** *
 * project: org.matsim.*
 * Profiler.java
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
package playground.johannes.coopsim;

import org.apache.log4j.Logger;

import gnu.trove.TObjectLongHashMap;

/**
 * @author illenberger
 *
 */
public class Profiler {

	private static final Logger logger = Logger.getLogger(Profiler.class);
	
	private static final TObjectLongHashMap<Object> starts = new TObjectLongHashMap<Object>();
	
	private static final TObjectLongHashMap<Object> measures = new TObjectLongHashMap<Object>();
	
	public static void start(Object obj) {
		starts.put(obj, System.currentTimeMillis());
	}
	
	public static long stop(Object obj) {
		return stop(obj, false);
	}
	
	public static long stop(Object obj, boolean print) {
		long time = System.currentTimeMillis() - starts.get(obj);
		time += measures.get(obj);
		
		starts.remove(obj);
		measures.remove(obj);
		
		if(print)
			logger.info(String.format("Profiling time for \"%1$s\": %2$.3f secs.", obj.toString(), time/1000.0));
		
		return time;
	}
	
	public static void pause(Object obj) {
		long time = System.currentTimeMillis() - starts.get(obj);
		measures.adjustOrPutValue(obj, time, time);
	}
	
	public static void resume(Object obj) {
		start(obj);
	}
	
}
